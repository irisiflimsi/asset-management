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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;

import net.rptools.asset.Asset;
import net.rptools.asset.AssetListener;

/**
 * Memory asset cache. It is discouraged to use the create method of caches.
 * @author username
 */
public class MemCacheAssetSupplier extends AbstractAssetSupplier {
    /** simple map */
    private Map<String, WeakReference<Asset>> map =
            Collections.synchronizedMap(new HashMap<String, WeakReference<Asset>>());

    /** precautionary alive set */
    private Set<Asset> alive = Collections.synchronizedSet(new HashSet<Asset>());

    /**
     * Constructor. Priorities specific to this class.
     * @param override properties to take precendence over default ones
     * @throws IOException can't load properties
     */
    public MemCacheAssetSupplier(Properties override) throws IOException {
        super(override);
        this.priority = Integer.parseInt(properties.getProperty(MemCacheAssetSupplier.class.getSimpleName() + ".priority"));
    }

    @Override
    public boolean has(String id) {
        // Note that this is volatile but we need to keep the reference from
        // being gc'ed in the next second
        final WeakReference<Asset> ref = map.get(id);
        final Asset obj = (ref != null ? ref.get() : null);

        Thread keepAlive = new Thread() {
            @Override
            public void run() {
                try {
                    alive.add(obj);
                    sleep(1000);
                }
                catch (InterruptedException e) {
                    // Don't care at this point
                }
                finally {
                    alive.remove(obj);
                }
            }
        };
        keepAlive.start();
        return (obj != null);
    }

    @Override
    public Asset get(String id, AssetListener listener) {
        WeakReference<Asset> ref = map.get(id);
        Asset obj = null;
        if (ref != null)
            obj = ref.get();
        if (listener != null)
            listener.notify(id, obj);
        return obj;
    }

    @Override
    public boolean canCreate(Class<?> clazz) {
        return true;
    }

    @Override
    public String create(Asset obj) {
        String id = UUID.randomUUID().toString();
        update(id, obj);
        return id;
    }

    @Override
    public void update(String id, Asset obj) {
        WeakReference<Asset> ref = new WeakReference<Asset>(obj);
        map.put(id, ref);
    }

    @Override
    public boolean canRemove(String id) {
        return has(id);
    }

    @Override
    public boolean remove(String id) {
        return (map.remove(id) != null);
    }
    
    /** Clear cache; valid only for this supplier */
    public void clear() {
        map.clear();
    }
}
