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
package net.rptools.asset.intern;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rptools.asset.AssetListener;
import net.rptools.asset.AssetSupplier;
import net.rptools.asset.intern.supplier.*;

/**
 * See the exported class. Used for poor-man's component separation.
 * @author username
 */
public class AssetManager implements net.rptools.asset.AssetManager {
    /** Logging */
    private final static Logger LOGGER = LoggerFactory.getLogger(DiskCacheAssetSupplier.class.getSimpleName());

    /** Our thread pool */
    private ExecutorService executors = Executors.newCachedThreadPool();

    /** All suppliers */
    private SortedSet<AssetSupplier> assetSuppliers;

    /**
     * Should make constructor invisible. Provide a comparator instead of decorating
     * all AssetSuppliers
     * @param properties properties to initialize suppliers with
     * @throws Exception instantiation or property retrieval problems
     */
    public AssetManager(Properties properties) throws Exception {
        SortedSet<AssetSupplier> set = new TreeSet<AssetSupplier>(new Comparator<AssetSupplier>() {
            @Override
            public int compare(AssetSupplier high, AssetSupplier low) {
                return low.getPriority() - high.getPriority();
            }
        });
        assetSuppliers = Collections.synchronizedSortedSet(set);
        fillSuppliers(properties);
    };

    @Override
    public void registerAssetSupplier(AssetSupplier supplier) {
        if (supplier == null) return;
        // Safety
        for (AssetSupplier iSupplier : assetSuppliers) {
            if (supplier != iSupplier && supplier.getPriority() == iSupplier.getPriority())
                throw new RuntimeException("Two asset suppliers with the same priority!");
        }
        assetSuppliers.add(supplier);
    }

    @Override
    public void deregisterAssetSupplier(AssetSupplier supplier) {
        if (supplier == null) return;
        assetSuppliers.remove(supplier);
    }

    @Override
    public Asset getAsset(String id, boolean cache) {
        return getAsset(id, null, cache);
    }

    @Override
    public void getAssetAsync(final String id, final AssetListener listener, final boolean cache) {
        if (id == null)
            throw new NullPointerException("getAssetAsync: id is null");
        executors.execute(new Runnable() {
            @Override
            public void run() {
                getAsset(id, listener, cache);
            }
        });
    }

    @Override
    public void createAsset(final Asset obj, final AssetListener listener, final boolean cache) throws IOException {
        AssetSupplier tmpPrioSupplier = null;
        // Create this in the highest repo only
        for (AssetSupplier supplier : assetSuppliers) {
            if (supplier.canCreate(obj.getType())) {
                tmpPrioSupplier = supplier;
                break;
            }
        }
        final AssetSupplier prioSupplier = tmpPrioSupplier;
        if (prioSupplier == null) throw new IOException("No creating AssetSupplier found");

        // User already has the object. Let's not let him wait.
        executors.execute(new Runnable() {
            @Override
            public void run() {
                String id = prioSupplier.create(obj);
                if (listener != null)
                    listener.notify(id, obj);
                
                Set<AssetSupplier> updateSet = new HashSet<AssetSupplier>();
                for (AssetSupplier supplier : assetSuppliers) {
                    if (cache && supplier.canCache(obj)) {
                        updateSet.add(supplier);
                    }
                }
                updateCaches(id, updateSet, obj);
            }
        });
    }

    @Override
    public boolean removeAsset(String id) {
        boolean success = true;
        for (AssetSupplier supplier : assetSuppliers)
            if (supplier.canRemove(id))
                success = success && supplier.remove(id);
        return success;
    }

    @Override
    public void copyAssets(final String[] ids, final AssetSupplier supplier, final AssetListener listener, final boolean update) throws IOException {
        executors.execute(new Runnable() {
            @Override
            public void run() {
                // We are copying "through memory", because it is comprehensible solution, although it
                // would (probably) be more efficient treating each type separately through NIO.
                for (String id : ids) {
                    Asset obj = getAsset(id, false);
                    if (obj != null) {
                        if (supplier.has(id) && !update) {
                            id = supplier.create(obj);
                        }
                        else {
                            supplier.update(id, obj);
                        }
                    }
                    // We notify none-the-less
                    if (listener != null)
                        listener.notify(id, obj);
                }
            }
        });
    }

    @Override
    public AssetSupplier createFileAssetSupplier(Properties props, String prefix) {
        try {
            return new FileAssetSupplier(props, prefix);
        }
        catch (Exception e) {
            LOGGER.error("Can't get file asset supplier", e);
            return null;
        }
    }

    @Override
    public AssetSupplier createHttpAssetSupplier(Properties props, String url) {
        try {
            return new HttpAssetSupplier(props, url);
        }
        catch (Exception e) {
            LOGGER.error("Can't get HTTP asset supplier", e);
            return null;
        }
    }

    @Override
    public AssetSupplier createZipFileAssetSupplier(Properties props, String prefix) {
        try {
            return new ZipFileAssetSupplier(props, prefix);
        }
        catch (Exception e) {
            LOGGER.error("Can't get zip file asset supplier", e);
            return null;
        }
    }
    /**
     * Main method for both getAsset and getAssetAsync.
     */
    private Asset getAsset(String id, AssetListener listener, boolean cache) {
        if (id == null)
            throw new NullPointerException("getAsset: id is null");

        Set<AssetSupplier> updateSet = new HashSet<AssetSupplier>();

        AssetSupplier usedSupplier = null;
        for (AssetSupplier supplier : assetSuppliers) {
            // Take the first one
            if (usedSupplier == null && supplier.has(id)) {
                usedSupplier = supplier;
            }
            // Check caching
            if (cache && usedSupplier != supplier && !supplier.has(id)) {
                updateSet.add(supplier);
            }
        }
        Asset obj = usedSupplier.get(id, listener);
        updateCaches(id, updateSet, obj);
        return obj;
    }

    /**
     * Update caches with a new object.
     * @param id asset id to update
     * @param updateSet caches to update
     * @param obj new object for the given id
     */
    private void updateCaches(String id, Set<AssetSupplier> updateSet, Asset obj) {
        // Now update
        for (AssetSupplier supplier : updateSet) {
            if (supplier.canCache(obj))
                supplier.cache(id, obj);
        }
    }

    /**
     * Factory-like method to provide all cache suppliers through
     * properties.
     * @throws IOException if something fails during registering
     */
    private void fillSuppliers(Properties override) throws IOException {
        Properties properties = getTotalProperties(override);
        registerAssetSupplier(new MemCacheAssetSupplier(properties));
        registerAssetSupplier(new DiskCacheAssetSupplier(properties));
    }

    /**
     * Helper method to combine properties. Make public for testing.
     * @param override more properties
     * @return merged properties
     * @throws IOException can't find properties
     */
    public static Properties getTotalProperties(Properties override) throws IOException {
        Properties properties = new Properties();
        InputStream defaultsStream = AssetManager.class.getClassLoader().getResourceAsStream("asset-management.properties");
        properties.load(defaultsStream);
        defaultsStream.close();
        if (override != null) {
            properties.putAll(override);
        }
        return properties;
    }
}

