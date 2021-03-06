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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.Properties;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rptools.asset.Asset;
import net.rptools.asset.AssetListener;
import net.rptools.asset.intern.AssetImpl;

/**
 * Using NIO to get assets from ZIP files.
 * @author username
 */
public class ZipFileAssetSupplier extends AbstractURIAssetSupplier {
    /** Logging */
    private final static Logger LOGGER = LoggerFactory.getLogger(ZipFileAssetSupplier.class.getSimpleName());

    /** Zipfile used */
    private FileSystem zipFile;

    /** Zipfile path */
    private String zipFilePath;

    /** Index of ids/files to locate in this supplier */
    private final Properties knownAssets = new Properties();

    /** Notify partial interval */
    private long notifyInterval = 500; // millis

    /**
     * Constructor. Loads properties and sets up the index (file) at the
     * <em>prefix</em> location.
     * @param override properties to take precendence over default ones
     * @param prefix actually an infix after <em>user.dir</em>
     * @throws IOException can't load properties
     * @throws NumberFormatException if certain properties aren't numbers
     */
    public ZipFileAssetSupplier(Properties override, String prefix) throws IOException {
        super(override);
        this.notifyInterval = Long.parseLong(properties.getProperty(ZipFileAssetSupplier.class.getSimpleName() + ".notifyInterval"));
        this.priority = Integer.parseInt(properties.getProperty(ZipFileAssetSupplier.class.getSimpleName() + ".priority"));
        zipFilePath = System.getProperty("user.dir") + ("/" + prefix).replaceAll("/", SEP);
        reloadZipFile();
        loadIndex();
    }

    @Override
    public boolean has(String id) {
        if (id == null) return false;
        return (knownAssets.getProperty(id) != null);
    }

    /**
     * Read the image, informing the listener once in a while.
     * @param url url to get
     * @param listener listener to inform
     * @return prepared image
     * @throws IOException in case any any problems occur
     */
    protected AssetImpl loadImage(String id, URI uri, AssetListener listener) {
        InputStream input = null;
        try {
            input = getStreamFromZipEntry(id, uri, listener);
        }
        catch (IOException e) {
            // input == null
            return null;
        }
        try {
            return new AssetImpl(ImageIO.read(input));
        }
        catch (IOException e) {
            return new AssetImpl(null);
        }
        finally {
            try {
                input.close(); // closes stream as well
            }
            catch (Exception e) { // includes NPE
            }
        }
    }

    /**
     * This method synchronizes access to the ziip file, consistent with other accesses.
     * @param id asset-id being looked for
     * @param uri element in zip file to seek
     * @param listener listsner to be notified as load progresses.
     * @return stream containing the asset
     */
    private synchronized InputStream getStreamFromZipEntry(String id, URI uri, AssetListener listener) throws IOException {
        InputStream input;
        // This is a zip-local-URI;
        Path entry = zipFile.getPath(uri.getPath());
        InputStream stream = Files.newInputStream(entry);
        long assetLength = Files.size(entry);
        input = new InputStreamInterceptor(id, assetLength, stream, listener, notifyInterval);
        return input;
    }

    /**
     * Helper to set up cache directory path. Does not include the identifying prefix.
     * @param home prefix that is ensured to exist.
     * @param cachePath path to find and potentially create
     * @return directory
     * @throws SecurityException if the path cannot be created
     */
    private void reloadZipFile() throws IOException {
        if (zipFile != null) zipFile.close();
        Path path = Paths.get(zipFilePath);
        zipFile = FileSystems.newFileSystem(path, null);
    }

    /**
     * Load index in memory. To be overloaded.
     * @throws IOException can't load index (file)
     */
    private synchronized void loadIndex() throws IOException {
        InputStream stream = null;
        try {
            Path index = zipFile.getPath("index");
            stream = Files.newInputStream(index);
            knownAssets.load(stream);
        }
        finally {
            if (stream != null)
                stream.close();
        }
    }

    @Override
    public synchronized String create(Asset obj) {
        OutputStream stream = null;
        try {
            BufferedImage img = BufferedImage.class.cast(obj.getMain());
            String id = UUID.randomUUID().toString();
            // Set up name
            String name = id;
            setAssetFile(id, name);
            Path entry = zipFile.getPath(name);
            stream = Files.newOutputStream(entry);
            ImageIO.write(img, obj.getFormat(), stream);
            return id;
        }
        catch (Exception e) {
            LOGGER.warn("Create failed", e);
            return null;
        }
        finally {
            try {
                if (stream != null)
                    stream.close();
                reloadZipFile();
            }
            catch (IOException e) {
                LOGGER.error("Closing stream failed", e);
            }
        }
    }

    @Override
    public synchronized void update(String id, Asset obj) {
        OutputStream stream = null;
        try {
            BufferedImage img = BufferedImage.class.cast(obj.getMain());
            String name = getKnownAsset(id);
            if (name != null) {
                Files.delete(zipFile.getPath(name)); // prepare for update
            }
            else {
                name = id;
            }
            setAssetFile(id, name);
            Path entry = zipFile.getPath(name);
            stream = Files.newOutputStream(entry);
            ImageIO.write(img, obj.getFormat(), stream);
        }
        catch (Exception e) {
            LOGGER.warn("Create failed", e);
        }
        finally {
            try {
                if (stream != null)
                    stream.close();
                reloadZipFile();
            }
            catch (IOException e) {
                LOGGER.error("Closing stream failed", e);
            }
        }
    }

    @Override
    public synchronized boolean remove(String id) {
        if (id == null) return false;
        try {
            String name = getKnownAsset(id);
            if (name == null)
                return false;
            setAssetFile(id, null);
            Path entry = zipFile.getPath(name);
            Files.delete(entry);
            reloadZipFile();
            return true;
        }
        catch (Exception e) {
            LOGGER.error("Cannot remove " + id, e);
        }
        return false;
    }

    @Override
    public boolean canCreate(Class<?> clazz) {
        return BufferedImage.class.equals(clazz);
    }

    /**
     * Direct reference getter, to be overloaded by subclasses
     * @param id id of the asset
     * @return asset name associate to id
     */
    protected String getKnownAsset(String id) {
        return knownAssets.getProperty(id);
    }

    /**
     * Resolve reference for writing. If the second parameter is null, this is
     * a delete call.
     * @param id asset to resolve
     * @param file URI name referenced
     * @throws IOException if stream can't be closed
     */
    private void setAssetFile(String id, String name) throws IOException {
        OutputStream stream = null;
        try {
            setKnownAsset(id, name);
            LOGGER.info("writing {} as {}", id, name);
            Path entry = zipFile.getPath("index");
            Files.delete(entry);
            stream = Files.newOutputStream(entry);
            knownAssets.store(stream, "Encoded as java properties");
        }
        catch (Exception e) {
            LOGGER.error("Store failed for " + id, e);
        }
        finally {
            if (stream != null) stream.close();
        }
    }

    /**
     * Direct reference setter, to be overloaded by subclasses. If name == null
     * the property is unset.
     * @param id id of the asset
     * @param name asset to associate with the id
     */
    private void setKnownAsset(String id, String name) {
        if (name == null)
            knownAssets.remove(id);
        else
            knownAssets.setProperty(id, name);
    }
}
