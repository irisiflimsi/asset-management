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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Properties;

import javax.imageio.ImageIO;

import net.rptools.asset.AssetListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides some defaults for URLS asset suppliers.
 * @author username
 */
public abstract class AbstractURLAssetSupplier extends AbstractAssetSupplier {
    /** Logging */
    protected static Logger LOGGER = LoggerFactory.getLogger(AbstractURLAssetSupplier.class.getName());

    /** Notify partial interval */
    protected long notifyInterval = 500; // millis
    
    /** C'tor stub */
    protected AbstractURLAssetSupplier(Properties override) throws IOException {
        super(override);
    }

    @Override
    public <T> T get(String id, Class<T> clazz, AssetListener<T> listener) {
        BufferedImage result = null;
        try {
            URI uri = new URI(id);
            // Only support buffered images
            if (BufferedImage.class.equals(clazz)) {
                LOGGER.info("Start loading " + id);
                result = loadImage(id, uri, (AssetListener<?>) listener);
                LOGGER.info("Finished loading " + id);
            }
        }
        catch (URISyntaxException e) {
            LOGGER.error(id + " is not an URL", e);
        }
        catch (IOException e) {
            LOGGER.error(id + " cannot be correctly read", e);
        }

        if (listener != null) {
            listener.notify(id, clazz.cast(result));
        }
        return clazz.cast(result);
    }

    /**
     * Read the image, informing the listener once in a while.
     * @param url url to get
     * @param listener listener to inform
     * @return prepared image
     * @throws IOException in case any any problems occur
     */
    private BufferedImage loadImage(String id, URI uri, AssetListener<?> listener) throws IOException {
        URLConnection connection = uri.toURL().openConnection();
        int assetLength = connection.getContentLength();
        InputStream input = new HttpInterceptor(id, assetLength, connection.getInputStream(), listener);
        return ImageIO.read(input);
    }

    /**
     * Interceptor to inform users of progress.
     * @author username
     */
    public class HttpInterceptor extends InputStream {
        private InputStream inputStream;
        private int remainder;
        private volatile boolean done;
        private HttpInterceptor(final String id, final int length, InputStream inputStream, final AssetListener<?> listener) {
            this.remainder = length;
            this.inputStream = inputStream;
            this.done = false;

            Thread supervisor = new Thread() {
                @Override
                public void run() {
                    while (!done) {
                        try {
                            LOGGER.info("notifyInterval: " + notifyInterval);
                            sleep(notifyInterval);
                            LOGGER.info("Notifying " + remainder + "/" + length);
                            if (length > 0) {
                                listener.notifyPartial(id, (length - remainder)/(double)length);
                            }
                            if (length < 0) {
                                listener.notifyPartial(id, remainder/(double)(remainder-1)); // white (?) lie
                            }
                        }
                        catch (Exception e) {
                            // Any exception stops this
                            LOGGER.error("Abort loading asset " + id, e);
                            done = true;
                        }
                    }
                }
            };
            if (listener != null)
                supervisor.start();
        }

        @Override
        public int read() throws IOException {
            // Only extremely fast operations allowed here
            if (remainder > 0) remainder--;
            int result = inputStream.read();
            // stop threads;
            if (result == -1) done = true;
            if (done) return -1;
            return result;
        }
    }
}
