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
package net.rptools.asset;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.rptools.asset.supplier.DiskCacheAssetSupplier;
import net.rptools.asset.supplier.MemCacheAssetSupplier;

/**
 * This class exists in the client and in the server and is responsible for
 * providing opaque objects (assets) from different sources. It evaluates
 * registered supplier by priorities. See the individual suppliers for asset
 * sources.
 * @author username
 */
public final class AssetManager {
    /** Singleton */
    private static AssetManager assetManager;

    /** Our thread pool */
    private ExecutorService executors = Executors.newCachedThreadPool();

    /** All suppliers */
    private SortedSet<AssetSupplier> assetSuppliers;

    /**
     * Make constructor invisible. Provide a comparator instead of decorating
     * all AssetSuppliers
     * @param properties properties to initialize suppliers with
     * @throws Exception instantiation or property retrieval problems
     */
    private AssetManager(Properties properties) throws Exception {
        SortedSet<AssetSupplier> set = new TreeSet<AssetSupplier>(new Comparator<AssetSupplier>() {
            @Override
            public int compare(AssetSupplier high, AssetSupplier low) {
                return low.getPriority() - high.getPriority();
            }
        });
        assetSuppliers = Collections.synchronizedSortedSet(set);
        fillSuppliers(properties);
    };

    /**
     * Use singleton pattern. The asset manager class will operate multi-threaded.
     * @param properties properties to initialize suppliers with
     * @throws Exception instantiation or property retrieval problems
     */
    public static synchronized AssetManager getInstance(Properties properties) throws Exception {
        if (assetManager == null)
            assetManager = new AssetManager(properties);
        return assetManager;
    }

    /**
     * Supplier register method. Idempotent.
     * @param supplier registered object. We ignore null.
     */
    public void registerAssetSupplier(AssetSupplier supplier) {
        if (supplier == null) return;
        // Safety
        for (AssetSupplier iSupplier : assetSuppliers) {
            if (supplier != iSupplier && supplier.getPriority() == iSupplier.getPriority())
                throw new RuntimeException("Two asset suppliers with the same priority!");
        }
        assetSuppliers.add(supplier);
    }

    /**
     * Supplier deregister method.
     * @param supplier object to deregister. We ignore null.
     */
    public void deregisterAssetSupplier(AssetSupplier supplier) {
        if (supplier == null) return;
        assetSuppliers.remove(supplier);
    }

    /** @see #getAsset(id, listener, cache) */
    public Asset getAsset(String id, boolean cache) {
        return getAsset(id, null, cache);
    }

    /**
     * Get an asset asynchronously. The notification will not be on the EDT
     * (or any other prominent thread). The returned object is null if not
     * found.
     * @param id identifies the asset (globally unique)
     * @param listener the listener to inform when the java object representing
     *    the asset is available. Pass a null, if not interested in success.
     * @throws NullPointerException if id is null
     */
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

    /**
     * Create a new asset to be managed by the suppliers. Which handler will
     * provide the asset in the future is transparent to the user and governed
     * by priority. In special cases the type of an asset to be created may
     * differ from the type of an object to be retrieved. To determine, whether
     * the creation was successful or not, a get operation must be performed.
     * Since the creation process may take long, an immediate get is not a good
     * idea. If there are no writable suppliers, an IOException is thrown.
     * @param obj to be maintained as asset
     * @param name to be resolved by the supplier, null the supplier should
     *   create one. This is a complete URI for the common handlers.
     *   If the name exists and update is true, the asset content is updated.
     *   If update is false, but the name exists, the new name is the id.
     * @param listener listener to inform about the creation. This should be
     *   non-null, because otherwise the id created will not become known!
     * @param cache cache the asset?
     * @param update in case the asset already exists, update or create a new
     * @throws IOException in case no writable supplier was found
     */
    public void createAsset(final String name, final Asset obj, final AssetListener listener, final boolean update, final boolean cache) throws IOException {
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
                String id = prioSupplier.create(name, obj, update);
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

    /**
     * Remove an asset from being managed by all the suppliers. False is
     * returned, if the asset could not be removed.
     * @param id identifies asset to be removed
     * @return if removal was successful
     */
    public boolean removeAsset(String id) {
        boolean success = true;
        for (AssetSupplier supplier : assetSuppliers)
            if (supplier.canRemove(id))
                success &= supplier.remove(id);
        return success;
    }

    /**
     * Get an asset synchronously. Avoid this for anything but local and small
     * assets. The returned object is null if not found.
     * @param id identifies the asset (globally unique)
     * @param listener listener to inform for asynchronous retrieval, may be null
     * @param cache cache the asset?
     * @return the java object representing the asset.
     * @throws NullPointerException if id is null
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
     * Memory problems, write access in the supplier and similar things all
     * lead to IOExceptions. The listener is informed about each asset
     * individually when completed. Partial completion is not notified.
     * @param ids list of assets to copy
     * @param update whether to overwrite in the destination supplier.
     * @param supplier destination
     * @param listener object to inform about partial completion
     * @throws IOException in case the supplier cannot write (all) assets in the list
     */
    public void copyAssets(final String[] ids, final AssetSupplier supplier, final AssetListener listener, final boolean update) throws IOException {
        executors.execute(new Runnable() {
            @Override
            public void run() {
                // We are copying "through memory", because it is comprehensible solution, although it
                // would (probably) be more efficient treating each type separately through NIO.
                for (String id : ids) {
                    Asset obj = getAsset(id, false);
                    if (obj != null)
                        supplier.create(null, obj, update);
                    // We notify none-the-less
                    if (listener != null)
                        listener.notify(id, obj);
                }
            }
        });
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

