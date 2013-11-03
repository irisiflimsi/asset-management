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
import java.awt.image.RenderedImage;
import java.io.*;
import java.net.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rptools.asset.AssetListener;
import net.rptools.asset.intern.AssetImpl;

/**
 * This class provides access to the disk cache.
 * We only provide BufferedImages currently.
 * @author username
 */
public class DiskCacheAssetSupplier extends AbstractURIAssetSupplier {
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
    public boolean canCache(AssetImpl obj) {
        return (obj.getType().equals(BufferedImage.class));
    }

    @Override
    public synchronized void cache(String id, AssetImpl obj) {
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
                return testFile.delete();
            }
        }
        catch (Exception e) {
            LOGGER.error("Remove failed for " + id, e);
        }
        return false;
    }

    @Override
    protected String getKnownAsset(String id) {
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
     * This method will reduce the cache until it is at most "toSize". The
     * order in which files are removed is the last-accessed order. That is,
     * the oldest not seen goes first. 
     * @param toSize size to prune to
     */
    public void prune(long toSize) {
        try {
            long totalSize = 0;
            SortedMap<FileTime, File> map = new TreeMap<FileTime, File>();
            File dir = new File(fileAssetPath);
            File[] list = dir.listFiles();
            // Check on all files
            for (File elem : list) {
                Path file = FileSystems.getDefault().getPath(elem.getAbsolutePath());
                BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                FileTime time = attrs.lastAccessTime();
                map.put(time, elem);
                totalSize += elem.length();
            }
            // Prune according to sorting order
            for (File elem : map.values()) {
                if (totalSize <= toSize)
                    break;
                totalSize -= elem.length();
                LOGGER.info("Removing from cache " + elem.getName());
                if (!elem.delete())
                    LOGGER.error("Cannot delete: " + elem.getAbsolutePath());
            }
        }
        catch (IOException e) {
            LOGGER.error("Can't prune the disc cache correctly", e);
        }
    }
}
