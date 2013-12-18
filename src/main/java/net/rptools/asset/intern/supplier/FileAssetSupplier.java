/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package net.rptools.asset.intern.supplier;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.UUID;

import javax.imageio.ImageIO;

import net.rptools.asset.Asset;
import net.rptools.asset.AssetListener;
import net.rptools.asset.intern.AssetImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides access to File URLs. The index file holds local file names.
 * We only provide BufferedImages currently.
 * @author username
 */
public class FileAssetSupplier extends AbstractURIAssetSupplier {
    /** Logging */
    private final static Logger LOGGER = LoggerFactory.getLogger(FileAssetSupplier.class.getSimpleName());

    /** Notify partial interval */
    private long notifyInterval = 500; // millis

    /** Index of ids/files to locate in this supplier */
    private final Properties knownAssets = new Properties();

    /** resource root directory path */
    private String fileAssetPath;

    /**
     * Constructor. Loads properties and sets up the index file at the
     * <em>prefix</em> location.
     * @param override properties to take precendence over default ones
     * @param prefix absolute path to the directory holding the assets
     * @throws IOException can't load properties
     * @throws NumberFormatException if certain properties aren't numbers
     */
    public FileAssetSupplier(Properties override, String prefix) throws IOException {
        super(override);
        this.notifyInterval = Long.parseLong(properties.getProperty(FileAssetSupplier.class.getSimpleName() + ".notifyInterval"));
        this.priority = Integer.parseInt(properties.getProperty(FileAssetSupplier.class.getSimpleName() + ".priority"));
        createPath(prefix);
        loadIndex(prefix);
    }

    @Override
    public boolean has(String id) {
        if (id == null) return false;
        return (getAssetFile(id) != null);
    }

    @Override
    public boolean canCreate(Class<?> clazz) {
        return BufferedImage.class.equals(clazz);
    }

    @Override
    public synchronized String create(Asset obj) {
        try {
            BufferedImage img = BufferedImage.class.cast(obj.getMain());
            String id = UUID.randomUUID().toString();
            // Set up name, if nothing useful is passed
            String localName = id;
            File f = setAssetFile(id, localName);
            ImageIO.write(img, obj.getFormat(), f);
            return id;
        }
        catch (Exception e) {
            LOGGER.warn("Create failed for " + obj, e);
            return null;
        }
    }

    @Override
    public synchronized void update(String id, Asset obj) {
        try {
            BufferedImage img = BufferedImage.class.cast(obj.getMain());
            String absName = getKnownAsset(id); // Returns absolute URI
            String localName = null;
            if (absName == null)
                localName = id;
            else
                localName = absName.substring(("file://" + fileAssetPath).length());
            File f = setAssetFile(id, localName);
            ImageIO.write(img, obj.getFormat(), f);
        }
        catch (Exception e) {
            LOGGER.warn("Create failed for " + id, e);
        }
    }

    @Override
    public boolean canRemove(String id) {
        if (id == null) return false;
        return (getAssetFile(id) != null);
    }

    @Override
    public boolean remove(String id) {
        if (id == null) return false;
        File testFile = getAssetFile(id);
        try {
            if (testFile != null && testFile.exists()) {
                setAssetFile(id, null);
                return testFile.delete();
            }
        }
        catch (Exception e) {
            LOGGER.error("Remove failed for " + id, e);
        }
        return false;
    }

    /**
     * Resolve reference for reading.
     * @param id asset to resolve
     * @return resolved asset
     */
    private File getAssetFile(String id) {
        try {
            String absName = getKnownAsset(id);
            if (absName == null)
                return null;
            return new File(new URI(absName));
        }
        catch (Exception e) {
            LOGGER.error("Get failed for " + id, e);
            return null;
        }
    }

    /**
     * Resolve reference for writing. If the second parameter is null, this is
     * a delete call.
     * @param id asset to resolve
     * @param localName local part of URI referenced
     * @throws IOException if close failed
     */
    private synchronized File setAssetFile(String id, String localName) throws IOException {
        OutputStream stream = null;
        try {
            setKnownAsset(id, localName);
            LOGGER.info("writing {} as {}", id, localName);
            stream = new FileOutputStream(fileAssetPath + "index");
            knownAssets.store(stream, "Encoded as java properties");
            if (localName == null)
                return null;
            return new File(new URI("file://" + fileAssetPath + localName));
        }
        catch (Exception e) {
            LOGGER.error("Store failed for " + id, e);
            return null;
        }
        finally {
            if (stream != null) stream.close();
        }
    }

    /**
     * Direct reference setter, to be overloaded by subclasses. If name == null
     * the property is unset.
     * @param id id of the asset
     * @param localName asset to associate with the id
     */
    private void setKnownAsset(String id, String localName) {
        if (localName == null)
            knownAssets.remove(id);
        else
            knownAssets.setProperty(id, localName);
    }

    @Override
    protected String getKnownAsset(String id) {
        String localName = knownAssets.getProperty(id);
        if (localName == null) return null;
        return "file://" + fileAssetPath + localName;
    }

    /**
     * Load index in memory. To be overloaded.
     * @param prefix URI prefix
     * @throws IOException can't load index (file)
     */
    private void loadIndex(String prefix) throws IOException {
        InputStream stream = null;
        try {
            stream = new FileInputStream(prefix + SEP + "index");
            knownAssets.load(stream);
        }
        finally {
            if (stream != null)
                stream.close();
        }
    }

    /**
     * Helper to set up cache directory path. Does not include the identifying prefix.
     * @param home prefix that is ensured to exist.
     * @param absPath (absolute) path to find and potentially create
     * @return directory
     * @throws SecurityException if the path cannot be created
     */
    private void createPath(String absPath) throws IOException {
        final String SEP = System.getProperty("file.separator");
        fileAssetPath = absPath.replaceAll("/", SEP);
        if (!fileAssetPath.endsWith(SEP))
            fileAssetPath += SEP;

        File completedPath = new File(fileAssetPath);
        if (!completedPath.exists()) {
            if (!completedPath.mkdirs())
                throw new IOException("Cannot create " + completedPath.getAbsolutePath());
        }
    }

    @Override
    protected AssetImpl loadImage(String id, URI uri, AssetListener listener) {
        try {
            URLConnection connection = uri.toURL().openConnection();
            int assetLength = Math.max(0, connection.getContentLength());
            InputStream input = new InputStreamInterceptor(id, assetLength, connection.getInputStream(), listener, notifyInterval);
            return new AssetImpl(ImageIO.read(input));
        }
        catch (MalformedURLException e) {
            return null;
        }
        catch (IOException e) {
            return new AssetImpl(null);
        }
    }
}
