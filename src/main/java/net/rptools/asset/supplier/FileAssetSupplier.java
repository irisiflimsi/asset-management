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
package net.rptools.asset.supplier;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

import javax.imageio.ImageIO;

import net.rptools.asset.AssetListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides access to File URLs.
 * We only provide BufferedImages currently.
 * @author username
 */
public class FileAssetSupplier extends  AbstractAssetSupplier {
    /** Logging */
    private final static Logger LOGGER = LoggerFactory.getLogger(DiskCacheAssetSupplier.class.getSimpleName());

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
     * @param prefix actually an infix after <em>user.dir</em>
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

    /**
     * Constructor for derived classes. Loads properties.
     * @param override properties to take precendence over default ones
     * @throws IOException can't load properties
     */
    protected FileAssetSupplier(Properties override) throws IOException {
        super(override);
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
    public synchronized <T> String create(String name, T obj, boolean update) {
        try {
            BufferedImage img = BufferedImage.class.cast(obj);
            String id = UUID.randomUUID().toString();
            // Set up name, if nothing useful is passed
            if (name == null || name.length() == 0)
                name = "file://" + fileAssetPath + id;

            if (knownAssets.containsValue(name)) {
                if (update) {
                    id = reverseLookup(name); // Update
                }
                else {
                    name = "file://" + fileAssetPath + id; // Leave old asset be
                }
            }
            File f = setAssetFile(id, name);
            ImageIO.write(img, "png", f);
            return id;
        }
        catch (Exception e) {
            LOGGER.warn("Create failed for " + name, e);
            return null;
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
            String name = getKnownAsset(id);
            LOGGER.info("reading " + id + " as " + name);
            if (name == null)
                return null;
            return new File(new URI(name));
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
     * @param file URI name referenced
     * @throws IOException if close failed
     */
    private synchronized File setAssetFile(String id, String name) throws IOException {
        OutputStream stream = null;
        try {
            setKnownAsset(id, name);
            LOGGER.info("writing " + id + " as " + name);
            stream = new FileOutputStream(fileAssetPath + "index");
            knownAssets.store(stream, "Encoded as java properties");
            if (name == null)
                return null;
            return new File(new URI(name));
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
     * @param name asset to associate with the id
     */
    private void setKnownAsset(String id, String name) {
        if (name == null)
            knownAssets.remove(id);
        else
            knownAssets.setProperty(id, name);
    }

    /**
     * Direct reference getter, to be overloaded by subclasses
     * @param id id of the asset
     * @return asset name associate to id
     */
    private String getKnownAsset(String id) {
        return knownAssets.getProperty(id);
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
     * @param cachePath path to find and potentially create
     * @return directory
     * @throws SecurityException if the path cannot be created
     */
    private void createPath(String cacheLocalPath) throws IOException {
        final String SEP = System.getProperty("file.separator");
        String home = System.getProperty("user.dir");
        fileAssetPath = home + SEP + cacheLocalPath.replaceAll("/", SEP);
        if (!fileAssetPath.endsWith(SEP))
            fileAssetPath += SEP;

        File completedPath = new File(fileAssetPath);
        if (!completedPath.exists()) {
            if (!completedPath.mkdirs())
                throw new IOException("Cannot create " + completedPath.getAbsolutePath());
        }
    }

    /**
     * Lookup up the id for a file name.
     * @param name file to look up
     * @return id of the file name
     */
    private String reverseLookup(String name) {
        for (Entry<Object, Object> entry : knownAssets.entrySet()) {
            if (entry.getValue().equals(name))
                return (String) entry.getKey();
        }
        return null;
    }

    @Override
    public <T> T get(String id, Class<T> clazz, AssetListener<T> listener) {
        BufferedImage result = null;
        try {
            URI uri = new URI(getKnownAsset(id));
            // Only support buffered images
            if (BufferedImage.class.equals(clazz)) {
                LOGGER.info("Start loading " + id);
                result = loadImage(id, uri, (AssetListener<?>) listener);
                LOGGER.info("Finished loading " + id);
            }
        }
        catch (URISyntaxException e) {
            LOGGER.error(id + " is not an URL", e);
        }
        catch (IOException e) {
            LOGGER.error(id + " cannot be correctly read", e);
        }

        if (listener != null) {
            listener.notify(id, clazz.cast(result));
        }
        return clazz.cast(result);
    }

    /**
     * Read the image, informing the listener once in a while.
     * @param url url to get
     * @param listener listener to inform
     * @return prepared image
     * @throws IOException in case any any problems occur
     */
    private BufferedImage loadImage(String id, URI uri, AssetListener<?> listener) throws IOException {
        URLConnection connection = uri.toURL().openConnection();
        int assetLength = connection.getContentLength();
        InputStream input = new Interceptor(id, assetLength, connection.getInputStream(), listener);
        return ImageIO.read(input);
    }

    /**
     * Interceptor to inform users of progress.
     * @author username
     */
    private class Interceptor extends InputStream {
        private InputStream inputStream;
        private long remainder;
        private volatile boolean done;
        private Interceptor(final String id, final long assetLength, InputStream inputStream, final AssetListener<?> listener) {
            this.remainder = assetLength;
            this.inputStream = inputStream;
            this.done = false;

            Thread supervisor = new Thread() {
                @Override
                public void run() {
                    while (!done) {
                        try {
                            LOGGER.info("notifyInterval: " + notifyInterval);
                            sleep(notifyInterval);
                            LOGGER.info("Notifying " + remainder + "/" + assetLength);
                            double ratio = 0;
                            if (assetLength >= remainder) {
                                ratio = Math.max(assetLength - remainder, 0)/(double)assetLength;
                            }
                            else {
                                ratio = (assetLength - remainder)/(double)(assetLength - remainder - 1); // white (?) lie
                            }
                            listener.notifyPartial(id, ratio);
                        }
                        catch (Exception e) {
                            // Any exception stops this
                            LOGGER.error("Abort loading asset " + id, e);
                            done = true;
                        }
                    }
                }
            };
            if (listener != null)
                supervisor.start();
        }

        @Override
        public int read() throws IOException {
            // Only extremely fast operations allowed here
            remainder--;
            int result = inputStream.read();
            // stop threads;
            if (result == -1) done = true;
            if (done) return -1;
            return result;
        }

        @Override
        public void close() throws IOException {
            if (inputStream != null)
                inputStream.close();
        }
    }
}
