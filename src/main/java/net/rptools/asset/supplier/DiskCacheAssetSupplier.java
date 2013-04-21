package net.rptools.asset.supplier;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.net.*;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rptools.asset.Asset;
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
    public boolean canCache(Asset obj) {
        return (obj.getType().equals(BufferedImage.class));
    }

    @Override
    public synchronized void cache(String id, Asset obj) {
        File testFile = getAssetFile(id);
        if (testFile == null) {
            LOGGER.info("Cannot cache asset " + id);
            return;
        }
        // This method also serves as update, so do something even if asset
        // already exists
        try {
            ImageIO.write(RenderedImage.class.cast(obj.getMain()), obj.getFormat(), new FileOutputStream(testFile));
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
                return testFile.delete();
            }
        }
        catch (Exception e) {
            LOGGER.error("Remove failed for " + id, e);
        }
        return false;
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
    public Asset get(String id, AssetListener listener) {
        Asset result = null;
        try {
            URI uri = new URI(getKnownAsset(id));
            LOGGER.info("Start loading " + id);
            result = loadImage(id, uri, listener);
            LOGGER.info("Finished loading " + id);
        }
        catch (URISyntaxException e) {
            return null;
        }
        finally {
            if (listener != null)
                listener.notify(id, result);
        }
        return result;
    }

    /**
     * Read the image, informing the listener once in a while.
     * @param url url to get
     * @param listener listener to inform
     * @return prepared image, null, if not found or Asset(null) if wrong format.
     */
    private Asset loadImage(String id, URI uri, AssetListener listener) {
        URLConnection connection;
        try {
            connection = uri.toURL().openConnection();
            int assetLength = connection.getContentLength();
            InputStream input = new InputStreamInterceptor(id, assetLength, connection.getInputStream(), listener, notifyInterval);
            return new Asset(ImageIO.read(input));
        }
        catch (MalformedURLException e) {
            return null;
        }
        catch (IOException e) {
            return new Asset(null);
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
