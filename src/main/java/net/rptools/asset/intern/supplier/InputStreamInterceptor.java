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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rptools.asset.AssetListener;

/**
 * Interceptor for input stream to inform users of progress/partial completion for the download.
 * @author username
 */
public class InputStreamInterceptor extends InputStream {
    /** Logging */
    private final static Logger LOGGER = LoggerFactory.getLogger(InputStreamInterceptor.class.getSimpleName());

    /** Intercepted input stream */
    private InputStream inputStream;

    /** Count down for stream content */
    private long remainder;

    /** Stream to close */
    private volatile boolean done;

    /** Notify partial interval */
    private long notifyInterval; // millis

    /**
     * Constructor.
     * @param id used to notify listener
     * @param assetLength length of asset
     * @param inputStream stream to intercept
     * @param listener this object is informed of partial completion
     */
    public InputStreamInterceptor(final String id, final long assetLength, InputStream inputStream, final AssetListener listener, long interval) {
        this.remainder = assetLength;
        this.inputStream = inputStream;
        this.done = false;
        this.notifyInterval = interval;

        Thread notifier = new PartialNotifierThread(id, assetLength, listener);
        if (listener != null)
            notifier.start();
    }

    @Override
    public int read() throws IOException {
        // Only extremely fast operations allowed here
        remainder--;
        int result = inputStream.read();
        // stop threads;
        if (result == -1) done = true;
        if (done) return -1;
        return result;
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null)
            inputStream.close();
    }
    
    /**
     * Notifier thread.
     * @author username
     */
    private final class PartialNotifierThread extends Thread {
        /** Asset id */
        private final String id;
        /** Asset length */
        private final long assetLength;
        /** listener to inform */
        private final AssetListener listener;
        /** Standard constructor */
        private PartialNotifierThread(String id, long assetLength, AssetListener listener) {
            this.id = id;
            this.assetLength = assetLength;
            this.listener = listener;
        }

        @Override
        public void run() {
            while (!done) {
                try {
                    LOGGER.info("notifyInterval: {}", notifyInterval);
                    sleep(notifyInterval);
                    double ratio = 0;
                    // remainder counts down from assetLength, which might be 0.
                    // In some valid cases, remainder may get negative
                    if (remainder >= 0) {
                        ratio = (assetLength + 1 - remainder)/(double)(assetLength + 1);
                    }
                    else {
                        ratio = remainder/(double)(remainder - 1); // white (?) lie
                    }
                    LOGGER.info("Notifying {} & {}; ratio={}", remainder, assetLength, ratio);
                    listener.notifyPartial(id, ratio);
                }
                catch (Exception e) {
                    // Any exception stops this
                    LOGGER.error("Abort loading asset " + id, e);
                    done = true;
                }
            }
        }
    }
}
