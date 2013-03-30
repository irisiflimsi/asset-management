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
package net.rptools.asset.supplier;

import java.io.IOException;
import java.util.Properties;

import net.rptools.asset.AssetSupplier;

/**
 * This class provides some defaults for asset suppliers.
 * @author username
 */
public abstract class AbstractAssetSupplier implements AssetSupplier {
    /** Default priority - shall not be used */
    public static int DEFAULT_PRIORITY = 100;

    /** priority (configurable in leaf classes!) */
    protected int prio = DEFAULT_PRIORITY;

    /** Properties */
    protected Properties properties = new Properties();

    /** C'tor stub */
    protected AbstractAssetSupplier(Properties override) throws IOException {
        properties = override;
    }

    @Override
    public int getPrio() {
        return prio; // see package-info of parent package
    }

    @Override
    public boolean canCache(Class<?> clazz) {
        return false;
    }

    @Override
    public <T> void cache(String id, T obj) {
        throw new UnsupportedOperationException("AbstractAssetSupplier.cache");
    }

    @Override
    public boolean canCreate(Class<?> clazz) {
        return false;
    }

    @Override
    public <T> String create(String idHint, T obj) {
        throw new UnsupportedOperationException("AbstractAssetSupplier.create");
    }

    @Override
    public boolean canRemove(String id) {
        return false;
    }

    @Override
    public boolean remove(String id) {
        // We do not throw an exception; the result code is sufficient
        return false;
    }

    @Override
    public boolean wantOverride(String id) {
        // TODO: this means that if an entry is cached, it will be taken.
        // Maybe this should have some background task to evaluate changes.
        // Clearing the cache (and restart) will initiate a reload.
        return false;
    }
}
