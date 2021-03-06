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
package net.rptools.asset.intern.supplier;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rptools.asset.AssetListener;
import net.rptools.asset.intern.AssetImpl;

/**
 * Refactored class. Provides commonalities for UIR asset suppliers.
 * @author username
 */
public abstract class AbstractURIAssetSupplier extends AbstractAssetSupplier {
    /** Logging */
    protected final static Logger LOGGER = LoggerFactory.getLogger(AbstractURIAssetSupplier.class.getSimpleName());

    /**
     * Constructor.
     * @param override properties to take precendence over default ones
     * @throws IOException can't load properties
     */
    protected AbstractURIAssetSupplier(Properties override) throws IOException {
        super(override);
    }

    @Override
    public AssetImpl get(String id, AssetListener listener) {
        AssetImpl result = null;
        try {
            URI uri = new URI(getKnownAsset(id));
            LOGGER.info("Start loading {}", id);
            result = loadImage(id, uri, listener);
            LOGGER.info("Finished loading {}", id);
        }
        catch (URISyntaxException e) {
            LOGGER.error(id + " is not an URL", e);
            return null;
        }
        finally {
            if (listener != null)
                listener.notify(id, result);
        }
        return result;
    }

    /**
     * Read the image, informing the listener once in a while.
     * @param url url to get
     * @param listener listener to inform
     * @return prepared image
     */
    abstract protected AssetImpl loadImage(String id, URI uri, AssetListener listener);

    /**
     * Direct reference getter, to be overloaded by subclasses.
     * @param id id of the asset
     * @return asset name associate to id. An absolute URI.
     */
    abstract protected String getKnownAsset(String id);
}
