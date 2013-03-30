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
 * Asset supplier class. Each instance provides a priority. The highest will
 * be picked. Any necessary synchronization is required of implementers of
 * this interface.
 * @author username
 */
public interface AssetSupplier {
    /** Does the supplier provide the asset with this id? */
    public boolean has(String id);

    /** provide the priority of this supplier. */
    public int getPrio();

    /**
     * Return the asset. Assets should be preloaded, i.e. either fully
     * available or sufficiently buffered. The return value may be null.
     * <tt>
     * obj = ...
     * if (obj == null || !clazz.isInstance(obj))
     *      return null;
     * return clazz.cast(o);
     * </tt>
     * @param id asset identifier
     * @param clazz class the user wants this to be cast to.
     * @return correct object of the desired class or null.
     * @throws ClassCastException due to technicalities to be avoided
     */
    public <T> T get(String id, Class<T> clazz, AssetListener<T> listener);

    /**
     * Can this supplier cache an asset of type clazz?
     * Note that in some special cases the input class may differ
     * from the output class used in #has.
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
     */
    public boolean canCreate(Class<?> clazz);

    /**
     * Create an asset of of type T.
     * Note that in some special cases the input class may differ
     * from the output class used in #get.
     * @param idHint hint for the id, to be used as (postfox) of the id
     * @param obj object to be created
     * @return new id
     */
    public <T> String create(String idHint, T obj);

    /** Can this supplier remove the asset with given id? */
    public boolean canRemove(String id);

    /**
     * Remove the asset with the given id. This method is not called
     * asynchrously. If implementors of this interface take a long time to
     * remove, setup a different thread.
     */
    public boolean remove(String id);

    /**
     * A lower priority asset supplier has found the id as well. Do we want
     * an override, e. g. a cache override?
     * @param id asset id looked for
     * @return override or not
     */
    public boolean wantOverride(String id);
}
