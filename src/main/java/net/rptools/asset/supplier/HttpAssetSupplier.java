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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Properties;

import javax.imageio.ImageIO;

import net.rptools.asset.Asset;
import net.rptools.asset.AssetListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides access to HTTP URLs.
 * Network (web) access is required for this supplier to work reasonably.
 * We only provide BufferedImages currently.
 * @author username
 */
public class HttpAssetSupplier extends  AbstractAssetSupplier {
    /** Logging */
    private final static Logger LOGGER = LoggerFactory.getLogger(DiskCacheAssetSupplier.class.getSimpleName());

    /** Notify partial interval */
    private long notifyInterval = 500; // millis

    /** resource root directory path */
    private String webAssetPath;

    /** Index of ids/files to locate in this supplier, allows override for testing */
    protected final Properties knownAssets = new Properties();

    /**
     * Constructor. Loads properties.
     * @param override properties to take precendence over default ones
     * @throws IOException can't load properties
     * @throws URISyntaxException 
     * @throws  
     * @throws NumberFormatException if certain properties aren't numbers
     */
    public HttpAssetSupplier(Properties override, String prefix) throws URISyntaxException, IOException {
        super(override);
        this.notifyInterval = Long.parseLong(properties.getProperty(HttpAssetSupplier.class.getSimpleName() + ".notifyInterval"));
        this.priority = Integer.parseInt(properties.getProperty(HttpAssetSupplier.class.getSimpleName() + ".priority"));
        // Load index file
        this.webAssetPath = prefix + (prefix.endsWith("/") ? "" : "/");
        loadIndexProperties();
    }

    /**
     * Allows override for testing.
     * @param prefix
     * @throws URISyntaxException 
     * @throws IOException 
     */
    protected void loadIndexProperties() throws IOException, URISyntaxException {
        InputStreamReader reader = null;
        try {
            URLConnection connection = new URI(webAssetPath + "index").toURL().openConnection();
            String encoding = connection.getContentEncoding();
            encoding = encoding == null ? "UTF-8" : encoding;
            reader = new InputStreamReader(connection.getInputStream(), encoding);
            knownAssets.load(reader);
        }
        finally {
            if (reader != null)
                reader.close();
        }
    }

    @Override
    public boolean has(String id) {
        return (knownAssets.getProperty(id) != null);
    }

    @Override
    public Asset get(String id, AssetListener listener) {
        Asset result = null;
        try {
            URI uri = new URI(webAssetPath + getKnownAsset(id));
            LOGGER.info("Start loading " + id);
            result = loadImage(id, uri, listener);
            LOGGER.info("Finished loading " + id);
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
     * Direct reference getter, to be overloaded by subclasses
     * @param id id of the asset
     * @return asset name associate to id
     */
    private String getKnownAsset(String id) {
        return knownAssets.getProperty(id);
    }

    /**
     * Read the image, informing the listener once in a while.
     * @param url url to get
     * @param listener listener to inform
     * @return prepared image
     * @throws IOException in case any any problems occur
     */
    private Asset loadImage(String id, URI uri, AssetListener listener) {
        URLConnection connection;
        try {
            connection = uri.toURL().openConnection();
            int assetLength = connection.getContentLength();
            InputStream input = new InputStreamInterceptor(id, assetLength, connection.getInputStream(), listener, notifyInterval);
            return new Asset(ImageIO.read(input));
        }
        catch (MalformedURLException e) {
            return null;
        }
        catch (IOException e) {
            return new Asset(null);
        }
    }
}
