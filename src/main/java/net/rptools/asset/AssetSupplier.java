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

import net.rptools.asset.intern.AssetImpl;

/**
 * Asset supplier class. Each instance has a priority to aid strategies
 * when selecting where to read from or write to. Any necessary thread
 * synchronization is required of implementers of this interface.
 * @author username
 */
public interface AssetSupplier {
    /** Does the supplier provide the asset with this id? */
    public boolean has(String id);

    /**
     * Get the priority of this supplier. Suppliers with higher numerical
     * priorities are chosen in preference of those with lower numerical
     * values.
     * @return priority
     */
    public int getPriority();

    /**
     * Set the priority of this supplier. Suppliers with higher numerical
     * priorities are chosen in preference of those with lower numerical
     * values.
     * @param priority (new) priority
     */
    public void setPriority(int priority);

    /**
     * Return the asset. Assets should be preloaded, i.e. either fully
     * available or sufficiently buffered. The return value may be null.
     * @param id asset identifier
     * @param listener listener to inform on (partial) success 
     * @return correct object of the desired class or null.
     */
    public AssetImpl get(String id, AssetListener listener);

    /**
     * Can this supplier create an asset of type clazz?
     * @return whether this supplier can create clazz objects.
     */
    public boolean canCreate(Class<?> clazz);

    /**
     * Create an asset.
     * @param obj object to be created
     * @return new asset id or null, if creation failed.
     */
    public String create(AssetImpl obj);

    /**
     * Update an asset. Note that this should be implemented as upsert.
     * Callers should check with canCreate first.
     * @param id to be resolved by the supplier.
     * @param obj object used for update.
     */
    public void update(String id, AssetImpl obj);

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
