package net.rptools.asset.supplier;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rptools.asset.AssetListener;

/**
 * This class provides access to the disk cache.
 * We only provide BufferedImages currently.
 * @author username
 */
public class DiskCacheAssetSupplier extends AbstractAssetSupplier {
    /** Logging */
    private final static Logger LOGGER = LoggerFactory.getLogger(DiskCacheAssetSupplier.class.getSimpleName());

    /** Cache path */
    private String fileAssetPath;

    /** Notify partial interval */
    private long notifyInterval = 500; // millis
    
    /**
     * Constructor. Loads properties.
     * @param override properties to take precendence over default ones
     * @throws IOException can't load properties
     */
    public DiskCacheAssetSupplier(Properties override) throws IOException {
        super(override);
        this.priority = Integer.parseInt(properties.getProperty(DiskCacheAssetSupplier.class.getSimpleName() + ".priority"));
        String cacheLocalPath = properties.getProperty(DiskCacheAssetSupplier.class.getSimpleName() + ".directory");
        createPath(cacheLocalPath);
    }

    @Override
    public boolean canCreate(Class<?> clazz) {
        return false;
    }

    @Override
    public <T> String create(String id, T obj, boolean update) {
        throw new UnsupportedOperationException("DiskCacheAssetSupplier.create");
    }

    @Override
    public boolean canRemove(String id) {
        return false;
    }

    @Override
    public boolean remove(String id) {
        return false;
    }

    @Override
    public boolean canCache(Class<?> clazz) {
        return clazz.equals(BufferedImage.class);
    }

    @Override
    public synchronized <T> void cache(String id, T obj) {
        File testFile = getAssetFile(id);
        if (testFile == null) {
            LOGGER.info("Cannot cache asset " + id);
            return;
        }
        // This method also serves as update, so do something even if asset
        // already exists
        try {
            ImageIO.write(RenderedImage.class.cast(obj), "png", new FileOutputStream(testFile));
        }
        catch (Exception e) {
            LOGGER.error("Cannot cache asset " + id, e);
        }
    }

    @Override
    public boolean has(String id) {
        File testFile = getAssetFile(id);
        return (testFile != null && testFile.exists());
    }

    /** Get URL from asset id */
    private String getKnownAsset(String id) {
        return "file://" + fileAssetPath + id;
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
            return new File(new URI(name));
        }
        catch (Exception e) {
            LOGGER.error("Get failed for " + id, e);
            return null;
        }
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
}
