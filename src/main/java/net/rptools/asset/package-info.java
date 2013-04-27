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
<li>HTTP: Web based storages. Prio to be supplied. (This should work for HTTP/SSL, but isn't tested.)</li>
<li>Zip HTTP: archive extension of the above. Prio to be supplied. (TODO: Not implemented yet.)</li>
<li>Server: a server process will provide assets. Prio to be supplied. (TODO: Not implemented yet.)</li>
</ul>
</p>
*/
package net.rptools.asset;