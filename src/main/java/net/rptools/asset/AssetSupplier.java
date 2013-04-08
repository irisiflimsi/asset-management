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

/**
 * Asset supplier class. Each instance has a priority. The highest that claims
 * to provide a given asset will be picked. Any necessary thread
 * synchronization is required of implementers of this interface.
 * @author username
 */
public interface AssetSupplier {
    /** Does the supplier provide the asset with this id? */
    public boolean has(String id);

    /** Get the priority of this supplier. */
    public int getPriority();

    /** Set the priority of this supplier. */
    public void setPriority(int priority);

    /**
     * Return the asset. Assets should be preloaded, i.e. either fully
     * available or sufficiently buffered. The return value may be null.
     * @param id asset identifier
     * @param clazz class the user wants this to be cast to
     * @param listener listener to inform on (partial) success 
     * @return correct object of the desired class or null.
     * @throws ClassCastException due to technicalities to be avoided
     */
    public <T> T get(String id, Class<T> clazz, AssetListener<T> listener);

    /**
     * Can this supplier cache an asset of type clazz?
     * Note that in some special cases the input class may differ
     * from the output class used in #has.
     * @return whether this supplier caches.
     */
    public boolean canCache(Class<?> clazz);

    /**
     * Cache an asset of of type T.
     * Note that in some special cases the input class may differ
     * from the output class used in #get. This call is also used
     * for updates ("upsert").
     */
    public <T> void cache(String id, T obj);

    /**
     * Can this supplier create an asset of type clazz?
     * Note that in some special cases the input class may differ
     * from the output class used in #has.
     * @return whether this supplier can create clazz objects.
     */
    public boolean canCreate(Class<?> clazz);

    /**
     * Create an asset of of type T.
     * Note that in some special cases the input class may differ
     * from the output class used in #get. Please note the update
     * semantics of this call.
     * @param name to be resolved by the supplier, null the supplier should
     *   create one. This is a complete URI for the common handlers.
     *   If the name exists and update is true, the asset content is updated.
     *   If update is false, but the name exists, the new name is the id.
     * @param obj object to be created
     * @param update is this call an update?
     * @return new asset id or null, if creation failed.
     * @throws RuntimeException when name is null or empty
     */
    public <T> String create(String name, T obj, boolean update);

    /** Can this supplier remove the asset with given id? */
    public boolean canRemove(String id);

    /**
     * Remove the asset with the given id. This method is not called
     * asynchrously. If implementors of this interface take a long time to
     * remove, setup a different thread.
     * @return whether this the remove was successful.
     */
    public boolean remove(String id);
}
