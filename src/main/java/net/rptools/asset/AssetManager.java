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
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class exists in the client and in the server and is responsible for
 * providing opaque objects (assets) from different sources. Seethe individual
 * suppliers for asset sources.
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
     * @throws Exception instantiation or property retrieval problems
     */
    private AssetManager(Properties properties) throws Exception {
        SortedSet<AssetSupplier> set = new TreeSet<AssetSupplier>(new Comparator<AssetSupplier>() {
            @Override
            public int compare(AssetSupplier high, AssetSupplier low) {
                return high.getPrio() - low.getPrio();
            }
        });
        assetSuppliers = Collections.synchronizedSortedSet(set);
        fillSuppliers(properties);
    };

    /**
     * Use singleton pattern. The asset manager class will operate multi-threaded.
     * @throws Exception instantiation or property retrieval problems
     */
    public static AssetManager getInstance(Properties properties) throws Exception {
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
            if (supplier != iSupplier && supplier.getPrio() == iSupplier.getPrio())
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

    /**
     * Get an asset synchronously. Avoid this for anything but few and small
     * assets. The returned object is null if not found.
     * @param id identifies the asset (globally unique)
     * @param clazz type of object that the asset must be represented as. 
     * @return the java object representing the asset.
     * @throws NullPointerException if id or clazz are null
     */
    public <T> T getAsset(String id, Class<T> clazz) {
        return getAsset(id, clazz, null);
    }

    /** @see #getAsset(UUID id, Class clazz) */
    private <T> T getAsset(String id, Class<T> clazz, AssetListener<T> listener) {
        if (id == null || clazz == null)
            throw new NullPointerException("getAsset: id or clazz are null");

        Set<AssetSupplier> updateSet = new HashSet<AssetSupplier>();

        AssetSupplier usedSupplier = null;
        for (AssetSupplier supplier : assetSuppliers) {
            // Take the first one
            if (usedSupplier == null && supplier.has(id)) {
                updateSet.add(supplier);
                usedSupplier = supplier;
            }
            // Check override
            else if (usedSupplier != null && supplier.has(id) && supplier.wantOverride(id)) {
                updateSet.add(supplier);
                usedSupplier = supplier;
            }
        }
        T obj = usedSupplier.get(id, clazz, listener);
        // Now update
        for (AssetSupplier supplier : updateSet) {
            if (supplier != usedSupplier && supplier.canCache(clazz))
                supplier.cache(id, obj);
        }
        return obj;
    }

    /**
     * Get an asset asynchronously. The notification will not be on the EDT
     * (or any other prominent thread). The returned object is null if not
     * found.
     * @param id identifies the asset (globally unique)
     * @param clazz type of object that the asset must be represented as. 
     * @param listener the listener to inform when the java object representing
     *    the asset is available. Pass a null, if not interested in success.
     * @throws NullPointerException if id or clazz are null
     */
    public <T> void getAssetAsync(final String id, final Class<T> clazz, final AssetListener<T> listener) {
        if (id == null || clazz == null)
            throw new NullPointerException("getAssetAsync: id or clazz are null");
        executors.execute(new Runnable() {
            @Override
            public void run() {
                getAsset(id, clazz, listener);
            }
        });
    }

    /**
     * Create a new asset to be managed by the suppliers. Which handler will
     * provide the asset in the future is transparent to the user. In special
     * cases the type of an asset to be created may differ from the type of an
     * object to be retrieved. To determine whether the creation was successful
     * or not, a get operation must be performed. (If a null is returned, the
     * create has also failed.
     * @param obj to be maintained as asset
     * @return new id
     */
    public <T> String createAsset(final T obj) {
        AssetSupplier tmpPrioSupplier = null;
        // Create this in the highest repo only
        for (AssetSupplier supplier : assetSuppliers) {
            if (supplier.canCache(obj.getClass()))
                tmpPrioSupplier = supplier;
        }
        final AssetSupplier prioSupplier = tmpPrioSupplier;
        if (prioSupplier == null) return null;
        final UUID uuid = UUID.randomUUID();

        // User already has the object. Let's not let him wait.
        executors.execute(new Runnable() {
            @Override
            public void run() {
                prioSupplier.cache(uuid.toString(), obj);
            }
        });
        return uuid.toString();
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
     * Factory-like method to provide all available suppliers through
     * properties.
     * @throws Exception instantiation exceptions or properties cannot be
     *     loaded
     */
    private void fillSuppliers(Properties override) throws IOException, Exception {
        Properties properties = getTotalProperties(override);
        for (Object property : properties.keySet()) {
            if (property.toString().endsWith(".priority")) {
                String className = "net.rptools.asset.supplier." + property.toString().replaceFirst(".priority", "");
                Constructor<?> ctor = Class.forName(className).getConstructors()[0];
                AssetSupplier supplier =
                        AssetSupplier.class.cast(ctor.newInstance(properties));
                assetSuppliers.add(supplier);
            }
        }
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

