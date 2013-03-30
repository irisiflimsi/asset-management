/**
<p>
This package (and its sub-package) provides access to assets for rptools. The assets are considered
opaque items, such as images or sounds usually handled by the OS at a specific time and place.
Certain asstes (such as vision-blocking images) may be processed internally. Asset objects inside
this process are considered immutable.
</p>
<p>
The problem to be solved is efficient retrieval of these assets. Network connections may be slow or
instable. Assets may change or be updated. Effectively, assets must have an "equivalence relation"
across location and time. This is achieved by identifying each asset with an id. The cuurent
suppliers of assets use ids that are URLs they can handle. Using URLs has the benefit of making
access simple, making ids externally useful, and no references that map ids to locations need to be
kept.
</p>
<p>
There are various asset handlers (see below) that can write, read and cache assets. The manager
implements a simple algorithm to shift often used assets into caches. Requests for assets may be
synchrounous or asynchronous. Long running requests, such as network requests should be handled
asynchronously. In this case a listener is registered that is informed about progress of asset
retrieval.
</p>
<ul>
<li>Memory cache: java heap; subject to memory management of java. Prio 10.</li>
<li>Disk cache: asset cache in a dedicated place on a local disk. Prio 20.</li>
<li>Files: local file access, but may be mounted via network. Prio 30.</li>
<li>Zip files: archive extension of the above. Prio 40.</li>
<li>HTTP: Web based storages. Prio 50./li>
<li>Zip HTTP: archive extension of the above. Prio 60.</li>
<li>Server: a server process will provide assets. Prio 70.</li>
</ul>
<p>
The algorithm is simple to allow efficient caching and yet be customizable. Each supplier provides
a configurable priority. The highest priority that overrides (usually only if an update occurred
at the orginal location) will provide the asset. All suppliers that have cache capabilities with
lower priority will be asked to cache the asset. If at a later time this algorithm is deemed
inadequate it can be changed. Hopefully, the interface is sufficient for this. (E.g. consider not
caching files in the DiskCacheAssetSuplier.)
</p>
<p>
If an asset supplier should not cache its entries, give it a lower priority than the cache
handlers. Any handler can be removed by not setting its priority.
</p>
*/
// TODO:
// * A nice "convenience" method that would take a WriteableAssetSupplier and a collection of asset id and go off in
//   its own thread running through that list of assets fetching them and writing them to the WriteableAssetSupplier,
//   this way the AssetManager can be used to easily create transportable asset "packs" that can be put on webserves,
//   drop box, google drive, sugar sync, usb stick, what ever else. The progress call back should be used to update
//   percetange of files done or whatever.
// * At the moment the AssetManager fetches the asset converts it to an image and returns null if it cant do this.
//   Really there are two separate conditions, the asset can't be found and the asset can't be loaded. These can be
//   represented differently on the map so that users know which problem they are looking for.
// * I have been toying around with the idea of having a separate metadata file be carried around with the asset (just
//   a small file with credits, license type (not the whole license just the name and url) and web page of creator. So
//   when these things are loaded it can return an Asset<T> instead of the object itself which has this metadata, or
//   even just a getAssetMetadata(...) method in AssetManager. Basically when the asset is fetched from the rptools
//   website or some other third party repository someone has set up it grabs the asset file and if it exists the
//   metadata file and caches them (if required), that way its possible for people to look up the credits and licenses
//   for any images that are fetched this way. I know many of these file formats contain places to put metadata but I
//   don't think its going to be a non messy solution to put all of this information in the file. This is certainly not
//   a must have, but I would list it as a very nice to have.
package net.rptools.asset;