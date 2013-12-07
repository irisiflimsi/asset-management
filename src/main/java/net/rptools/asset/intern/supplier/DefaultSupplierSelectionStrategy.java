package net.rptools.asset.intern.supplier;

import java.util.SortedSet;

import net.rptools.asset.Asset;
import net.rptools.asset.AssetListener;
import net.rptools.asset.AssetSupplier;

/**
 * Default supplier selection strategy. When reading we choose the suppliers
 * MemCache, DiskCache, File, ZipFile, Http in that order and among one type
 * according to priority. When writing, the first priority (writable) File is
 * chosen. All DiskCaches and MemCaches are updated when reading and
 * writing (and the cache parameter is true).
 * @author username
 */
public class DefaultSupplierSelectionStrategy {
    // Sorted class names
    private static Class<?>[] order = {
        MemCacheAssetSupplier.class, DiskCacheAssetSupplier.class, FileAssetSupplier.class, ZipFileAssetSupplier.class,
        HttpAssetSupplier.class
    };

    /**
     * Get an asset according to this strategy.
     * @param assetSuppliers list of suppliers to choose from
     * @param id id to look for
     * @param listener listener to inform about progress
     * @param cache whether to cache the obtained asset
     * @return the asset sought for
     */
    public static Asset getAssetByStrategy(SortedSet<AssetSupplier> assetSuppliers, String id, AssetListener listener, boolean cache) {
        for (Class<?> clazz : order) {
            for (AssetSupplier supplier : assetSuppliers) {
                if (clazz.isInstance(supplier)) {
                    if (supplier.has(id)) {
                        Asset obj = supplier.get(id, listener);
                        if (cache)
                            updateCaches(id, assetSuppliers, obj);
                        return obj;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Create an asset according to this strategy. TODO: signal failure.
     * @param assetSuppliers list of suppliers to choose from
     * @param listener listener to inform about progress
     * @param cache whether to cache the obtained asset
     * @param obj asset to store/create
     */
    public static void createAsset(SortedSet<AssetSupplier> assetSuppliers, final Asset obj, final AssetListener listener, final boolean cache) {
        for (AssetSupplier supplier : assetSuppliers) {
            if (supplier instanceof FileAssetSupplier && supplier.canCreate(obj.getType())) {
                String id = supplier.create(obj);
                if (listener != null)
                    listener.notify(id, obj);
                if (cache)
                    updateCaches(id, assetSuppliers, obj);
                return;
            }
        }
    }

    /**
     * Update caches with a new object.
     * @param id asset id to update
     * @param updateSet caches to update
     * @param obj new object for the given id
     */
    private static void updateCaches(String id, SortedSet<AssetSupplier> updateSet, Asset obj) {
        // Now update
        for (AssetSupplier supplier : updateSet) {
            if (supplier instanceof DiskCacheAssetSupplier || supplier instanceof MemCacheAssetSupplier)
                supplier.update(id, obj);
        }
    }
}
