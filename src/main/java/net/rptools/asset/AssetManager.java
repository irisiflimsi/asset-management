package net.rptools.asset;

import java.io.IOException;
import java.util.Properties;

import net.rptools.asset.intern.Asset;

/**
 * This class exists in the client and in the server and is responsible for
 * providing opaque objects (assets) from different sources. It evaluates
 * registered supplier by priorities. See the individual suppliers for asset
 * sources. Suppliers created must be registered separately.
 * @author username
 */
public interface AssetManager {
    /**
     * Supplier register method. Idempotent.
     * @param supplier registered object. We ignore null.
     */
    public void registerAssetSupplier(AssetSupplier supplier);

    /**
     * Supplier deregister method.
     * @param supplier object to deregister. We ignore null.
     */
    public void deregisterAssetSupplier(AssetSupplier supplier);

    /**
     * Get an asset synchronously. Avoid this for anything but local and small
     * assets. The returned object is null if not found.
     * @param id identifies the asset (globally unique)
     * @param listener listener to inform for asynchronous retrieval, may be null
     * @param cache cache the asset?
     * @return the java object representing the asset.
     * @throws NullPointerException if id is null
     */
    public Asset getAsset(String id, boolean cache);

    /**
     * Get an asset asynchronously. The notification will not be on the EDT
     * (or any other prominent thread). The returned object is null if not
     * found.
     * @param id identifies the asset (globally unique)
     * @param listener the listener to inform when the java object representing
     *    the asset is available. Pass a null, if not interested in success.
     * @throws NullPointerException if id is null
     */
    public void getAssetAsync(final String id, final AssetListener listener, final boolean cache);

    /**
     * Create a new asset to be managed by the suppliers. Which handler will
     * provide the asset in the future is transparent to the user and governed
     * by priority. To determine, whether the creation was successful or not,
     * a get operation must be performed. Since the creation process may take
     * long, an immediate get is not a good idea. If there are no writable
     * suppliers, an IOException is thrown.
     * @param obj to be maintained as asset
     * @param listener listener to inform about the creation. This should be
     *   non-null, because otherwise the id created will not become known!
     * @param cache cache the asset?
     * @throws IOException in case no writable supplier was found
     */
    public void createAsset(final Asset obj, final AssetListener listener, final boolean cache) throws IOException;

    /**
     * Remove an asset from being managed by all the suppliers. False is
     * returned, if the asset could not be removed.
     * @param id identifies asset to be removed
     * @return if removal was successful
     */
    public boolean removeAsset(String id);

    /**
     * Memory problems, write access in the supplier and similar things all
     * lead to IOExceptions. The listener is informed about each asset
     * individually when completed. Partial completion is not notified. If
     * update is false and an asset already exists, a different id is
     * notified than was passed in. Equality on the asset may be checked to
     * find out the additional asset.
     * @param ids list of assets to copy
     * @param update whether to overwrite in the destination supplier.
     * @param supplier destination
     * @param listener object to inform about partial completion
     * @throws IOException in case the supplier cannot write (all) assets in the list
     */
    public void copyAssets(final String[] ids, final AssetSupplier supplier, final AssetListener listener, final boolean update) throws IOException;

    /**
     * Provides a directory which can supply assets. Read and write operations
     * possible. Faults will result in a null return value.
     * @param prefix actually an infix after <em>user.dir</em>
     * @param properties change behaviour of the supplier, may be null
     */
    public AssetSupplier createFileAssetSupplier(Properties props, String prefix);

    /**
     * Provide a URL, which can supply assets. Read operations only.
     * Faults will result in a null return value.
     * @param url url to serve as root
     * @param properties change behaviour of the supplier, may be null
     */
    public AssetSupplier createHttpAssetSupplier(Properties props, String url);

    /**
     * Provide a zip file which can supply assets. Read and write operations
     * possible. Faults will result in a null return value.
     * @param prefix actually an infix after <em>user.dir</em>
     * @param properties change behaviour of the supplier, may be null
     */
    public AssetSupplier createZipFileAssetSupplier(Properties props, String prefix);
}
