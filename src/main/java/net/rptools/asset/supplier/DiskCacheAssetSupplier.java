package net.rptools.asset.supplier;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    protected static Logger LOGGER = LoggerFactory.getLogger(DiskCacheAssetSupplier.class.getSimpleName());

    /** assect cache absolute path */
    protected String cacheDir;

    /** Cached file name prefix */
    private final static String CACHED = "Cached-";

    /** local file separator constant */
    private final static String SEP = System.getProperty("file.separator");

    /**
     * C'tor. Loads properties.
     * @param override properties to take precendence over default ones
     * @throws IOException can't load properties
     */
    public DiskCacheAssetSupplier(Properties override) throws IOException {
        super(override);
        this.prio = Integer.parseInt(properties.getProperty(DiskCacheAssetSupplier.class.getSimpleName() + ".priority"));
        String home = System.getProperty("user.dir");
        String cachePath = properties.getProperty(DiskCacheAssetSupplier.class.getSimpleName() + ".directory");
        this.cacheDir = createPath(home, cachePath);
    }

    @Override
    public boolean has(String id) {
        File testFile = getAssetFile(id);
        return (testFile != null && testFile.exists());
    }

    @Override
    public <T> T get(String id, Class<T> clazz, AssetListener<T> listener) {
        File testFile = getAssetFile(id);
        BufferedImage result = null;
        try {
            if (testFile != null)
                result = ImageIO.read(testFile);
            if (listener != null)
                listener.notify(id, clazz.cast(result));
            return clazz.cast(result);
        }
        catch (Exception e) {
            LOGGER.error("Can't get Asset", e);
        }
        return null;
    }

    @Override
    public boolean canCache(Class<?> clazz) {
        if (clazz.equals(BufferedImage.class))
            return true;
        return false;
    }

    @Override
    public <T> void cache(String id, T obj) {
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
    public boolean canRemove(String id) {
        File testFile = getAssetFile(id);
        return (testFile != null && testFile.exists());
    }

    @Override
    public boolean remove(String id) {
        File testFile = getAssetFile(id);
        try {
            if (testFile != null && testFile.exists()) {
                testFile.delete();
                return true;
            }
        }
        catch (Exception e) {
            LOGGER.error("Cannot delete in asset cache", e);
        }
        return false;
    }

    @Override
    public boolean wantOverride(String id) {
        // Caches never override
        return false;
    }

    /**
     * Digest helper. May be overloaded to provide better hashes. Must return
     * valid filenames.
     * @param id id to hash
     * @return hashed id that can serve as a file name
     * @throws NoSuchAlgorithmException system exception, really
     * @throws UnsupportedEncodingException system exception, really
     */
    protected String hash(String id) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(id.getBytes("UTF-8"));
        return new String(digest); // MD5 is always ASCII, encoding implicit
    }

    /**
     * Helper to get the asset file. This is a two step approach. We check,
     * whether the id already is a cache-generated id. If not, the id is hashed
     * to produce a unique filename.
     * @param id id to look for
     * @return file (may not exist)
     */
    private File getAssetFile(String id) {
        try {
            String hash = hash(id);
            return new File(cacheDir + SEP + CACHED + hash);
        }
        catch (Exception e) {
            LOGGER.error("Cache conversion problem", e);
            return null;
        }
    }

    /**
     * Helper to set up cache directory path. Does not include the identifying prefix.
     * @param home prefix that is ensured to exist.
     * @param cachePath path to find and potentially create
     * @return directory
     * @throws SecurityException if the path cannot be created
     */
    private String createPath(String home, String cachePath) {
        cachePath = cachePath.replaceAll("/", SEP);
        File completedPath = new File(home + SEP + cachePath);
        if (!completedPath.exists()) {
            if (!completedPath.mkdirs())
                throw new RuntimeException("Cannot create " + completedPath.getAbsolutePath());
        }
        return completedPath.getAbsolutePath();
    }
}
