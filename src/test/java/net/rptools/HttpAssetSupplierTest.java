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
package net.rptools;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.*;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import net.rptools.asset.Asset;
import net.rptools.asset.AssetListener;
import net.rptools.asset.AssetManager;
import net.rptools.asset.supplier.HttpAssetSupplier;

import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.Matchers.*;

public class HttpAssetSupplierTest {

    public class TestHttpAssetSupplier extends HttpAssetSupplier {
        public TestHttpAssetSupplier(Properties override, String prefix) throws URISyntaxException, IOException {
            super(override, prefix);
        }
        @Override
        protected void loadIndexProperties() {
            knownAssets.setProperty(MY_ID, "nurbcup2si.png");
        }
    }

    private HttpAssetSupplier testObject;
    final public static String TEST_IMAGE = "http://www.w3.org/Graphics/PNG/nurbcup2si.png";
    final private static String MY_ID = "1234";
    
    @Before
    public void setUp() throws Exception {
        testObject = new TestHttpAssetSupplier(AssetManager.getTotalProperties(null), "http://www.w3.org/Graphics/PNG/");
    }

    @Test
    public void testTrivialMethods() {
        assertThat(testObject.canCache(new Asset(new BufferedImage(1,1,1))), is(false));
        assertThat(testObject.canRemove(null), is(false));
        assertThat(testObject.remove(null), is(false));
        assertThat(testObject.canCreate(BufferedImage.class), is(false));
        assertThat(testObject.has("ftp://foo.bar"), is(false));
        assertThat(HttpAssetSupplier.DEFAULT_PRIORITY, is(not(equalTo(testObject.getPriority()))));
        assertThat(testObject.has(MY_ID), is(true));
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testCreateFails() {
        testObject.cache(null, null);
    }
    
    @Test
    // Requires "internet" access
    public void testGet() {
        BufferedImage png = (BufferedImage) testObject.get(MY_ID, null).getMain();
        assertThat(png, is(notNullValue()));
        assertThat(png.getHeight() * png.getWidth(), is(greaterThan(4))); // not likely to become that small
    }

    @Test
    // Requires "internet" access
    public void testGetAsync() throws TimeoutException {
        AssetListener listener = createMock("Listener", AssetListener.class);
        listener.notifyPartial(eq(MY_ID), anyDouble());
        expectLastCall().anyTimes();
        listener.notify(eq(MY_ID), anyObject(Asset.class));
        replay(listener);

        BufferedImage png = (BufferedImage) testObject.get(MY_ID, listener).getMain();

        assertThat(png, is(notNullValue()));
        assertThat(png.getHeight() * png.getWidth(), is(greaterThan(4))); // not likely to become that small
        verify(listener);
    }

    @Test
    // Requires "internet" access. This test may fail, if the the retrieval of the image
    // is particularly fast.
    public void testGetAsyncAbort() throws TimeoutException {
        AssetListener listener = createMock("Listener", AssetListener.class);
        listener.notifyPartial(eq(MY_ID), anyDouble());
        expectLastCall().andThrow(new TimeoutException());
        listener.notify(eq(MY_ID), anyObject(Asset.class));
        replay(listener);
        Logger.getAnonymousLogger().warning("Exception test started");

        Asset png = testObject.get(MY_ID, listener);
        assertThat(png == null || png.getMain() == null, is(true));
        verify(listener);

        Logger.getAnonymousLogger().warning("Exception test ended");
    }
}
