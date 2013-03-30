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

/**
 * This class provides access to HTTP URLs.
 * Network (web) access is required for this supplier to work reasonably.
 * We only provide BufferedImages currently.
 * @author username
 */
public class HttpAssetSupplier extends  AbstractURLAssetSupplier {
    /**
     * C'tor. Loads properties.
     * @param override properties to take precendence over default ones
     * @throws IOException can't load properties
     * @throws NumberFormatException if certain properties aren't numbers
     */
    public HttpAssetSupplier(Properties override) throws IOException {
        super(override);
        this.notifyInterval = Long.parseLong(properties.getProperty(HttpAssetSupplier.class.getSimpleName() + ".notifyInterval"));
        this.prio = Integer.parseInt(properties.getProperty(HttpAssetSupplier.class.getSimpleName() + ".priority"));
    }

    @Override
    public boolean has(String id) {
        return id.startsWith("http://");
    }
}
