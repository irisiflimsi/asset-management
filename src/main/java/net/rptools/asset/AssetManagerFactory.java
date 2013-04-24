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

import java.util.Properties;

import net.rptools.asset.intern.AssetManager;

/**
 * This class exists in the client and in the server and is responsible for
 * providing opaque objects (assets) from different sources. It evaluates
 * registered supplier by priorities. See the individual suppliers for asset
 * sources.
 * @author username
 */
public class AssetManagerFactory {
    /** Singleton */
    private static AssetManager assetManager;

    /**
     * Use singleton pattern. The asset manager class will operate multi-threaded.
     * @param properties properties to initialize suppliers with
     * @throws Exception instantiation or property retrieval problems
     */
    public static synchronized AssetManager getInstance(Properties properties) throws Exception {
        if (assetManager == null)
            assetManager = new AssetManager(properties);
        return assetManager;
    }
}

