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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Properties;

import javax.imageio.ImageIO;

import net.rptools.asset.AssetListener;
import net.rptools.asset.intern.AssetImpl;

/**
 * This class provides access to HTTP URLs.
 * Network (web) access is required for this supplier to work reasonably.
 * We only provide BufferedImages currently.
 * @author username
 */
public class HttpAssetSupplier extends AbstractURIAssetSupplier {
    /** Notify partial interval */
    private long notifyInterval = 500; // millis

    /** resource root directory path */
    private String webAssetPath;

    /** Index of ids/files to locate in this supplier, allows override for testing */
    protected final Properties knownAssets = new Properties();

    /**
     * Constructor. Loads properties.
     * @param override properties to take precendence over default ones
     * @param url URL to use as asset root
     * @throws IOException can't load properties
     * @throws URISyntaxException 
     * @throws  
     * @throws NumberFormatException if certain properties aren't numbers
     */
    public HttpAssetSupplier(Properties override, String url) throws URISyntaxException, IOException {
        super(override);
        this.notifyInterval = Long.parseLong(properties.getProperty(HttpAssetSupplier.class.getSimpleName() + ".notifyInterval"));
        this.priority = Integer.parseInt(properties.getProperty(HttpAssetSupplier.class.getSimpleName() + ".priority"));
        // Load index file
        this.webAssetPath = url + (url.endsWith("/") ? "" : "/");
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
    protected String getKnownAsset(String id) {
        return webAssetPath + knownAssets.getProperty(id);
    }

    @Override
    protected AssetImpl loadImage(String id, URI uri, AssetListener listener) {
        try {
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            long assetLength = Math.max(0, connection.getContentLengthLong());
            InputStream input = new InputStreamInterceptor(id, assetLength, connection.getInputStream(), listener, notifyInterval);
            return new AssetImpl(ImageIO.read(input));
        }
        catch (MalformedURLException e) {
            return null;
        }
        catch (IOException e) {
            return new AssetImpl(null);
        }
    }
}
