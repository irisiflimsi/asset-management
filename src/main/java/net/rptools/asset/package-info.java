/**
<p>
This package (and its sub-package) provides access to assets. Assets are opaque objects that are
referenced from "primary" objects such as campaigns, maps, and tokens. RP tools are structured such
that primaries contain the assets they need, allowing for self-contained primaries. Asset retrieval
thus has several aspects: (*) efficient retrieval, because primaries may only become available over
a slow connection. The contained assets might be retrieved faster from another source. (*) identity
management. Since an asset may be available from several sources, an "equivalence relation" must be
established among them. (*) Updating assets. The original source of an asset included in a primary
may change and the dependent should reflect that.
</p>
<p>
The first point is solved by providing two caches, one in memory and one in disk. Some assets may
be volatile or retrieved fast enough without caches, so methods are provided that by-pass caches.
Additionally, multiple suppliers may be attached to make use of priority handling.
</p>
<p>
The second is handled by each individual supplier to identify assets it provides. The equivalence
across suppliers is a consequence of this. Handling for non-cache suppliers is suggested as a index
set mapping specific ids to a (generalized) URIs.
</p>
<p>
The third point is realized through API calls. While it is possible to have a timestamp or similar
construction in the index files to facilitate an update procedure, this is currently not
implemented. An update must be triggered by the user.
</p>
<p>
There are asset handlers (see below) that can write, read, and cache assets. Requests for assets
may be synchrounous or asynchronous. Long running requests, such as network requests should be
handled asynchronously. In this case a listener is registered that is informed about progress of
asset retrieval. Since requestors of assets are often oblivious as to the source that is used,
generally asynchronous retrieval is suggested.
</p>
<p>
<ul>
<li>Memory cache: java heap; subject to memory management of java. Prio 100.</li>
<li>Disk cache: asset cache in a dedicated place on a local disk. Prio 50.</li>
<li>Files: local file access, but may be mounted via network. Prio to be supplied.</li>
<li>Zip files: extension of the above. Prio to be supplied.</li>
<li>HTTP: Web based storages. Prio to be supplied./li>
<li>Zip HTTP: archive extension of the above. Prio to be supplied. (TODO: Not implemented yet.)</li>
<li>Server: a server process will provide assets. Prio to be supplied. (TODO: Not implemented yet.)</li>
</ul>
</p>
*/
// TODO:
// * Refactor for Interface and redundant implementation.
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