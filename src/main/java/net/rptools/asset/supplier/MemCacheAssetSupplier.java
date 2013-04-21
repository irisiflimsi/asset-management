package net.rptools.asset.supplier;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;

import net.rptools.asset.Asset;
import net.rptools.asset.AssetListener;

/**
 * Memory asset cache.
 * @author miju
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
    public boolean canCache(Asset obj) {
        return true;
    }

    @Override
    public void cache(String id, Asset obj) {
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
}
