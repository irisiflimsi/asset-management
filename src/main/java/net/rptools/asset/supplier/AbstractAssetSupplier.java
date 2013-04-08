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
 * This class provides defaults for asset suppliers.
 * @author username
 */
public abstract class AbstractAssetSupplier implements AssetSupplier {
    /** Default priority - shall not be used */
    public static final int DEFAULT_PRIORITY = 0;

    /** priority (configurable in leaf classes!) */
    protected int priority = DEFAULT_PRIORITY;

    /** local file separator constant */
    protected final static String SEP = System.getProperty("file.separator");

    /** Properties */
    protected Properties properties = new Properties();

    /** Constructtor stub */
    protected AbstractAssetSupplier(Properties override) throws IOException {
        properties = override;
    }

    @Override
    public int getPriority() {
        return priority; // see package-info of parent package
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
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
    public <T> String create(String id, T obj, boolean update) {
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
}
