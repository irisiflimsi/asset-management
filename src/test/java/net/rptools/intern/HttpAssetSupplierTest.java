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
package net.rptools.intern;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import net.rptools.asset.AssetListener;
import net.rptools.asset.intern.Asset;
import net.rptools.asset.intern.AssetManager;
import net.rptools.asset.intern.supplier.HttpAssetSupplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HttpAssetSupplierTest extends TestConstants {
    private HttpAssetSupplier testObject;
    
    @Before
    public void setUp() throws Exception {
        BufferedImage img = ImageIO.read(ZipFileAssetSupplierTest.class.getClassLoader().getResourceAsStream(TEST_IMAGE));
        ImageIO.write(img, "png", new File(USER_DIR + TEST_DIR + TEST_IMAGE));
        HttpTestServer.start();
        testObject = new HttpAssetSupplier(AssetManager.getTotalProperties(null), "http://localhost:8080");
    }

    @After
    public void teardown() throws Exception {
        File dir = new File(USER_DIR + TEST_DIR);
        for (File rm : dir.listFiles())
            rm.delete();
        HttpTestServer.stop();
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
    public void testGet() {
        BufferedImage png = (BufferedImage) testObject.get(MY_ID, null).getMain();
        assertThat(png, is(notNullValue()));
        assertThat(png.getHeight() * png.getWidth(), is(greaterThan(4))); // not likely to become that small
    }

    @Test
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
    public void testGetAsyncAbort() throws TimeoutException, IOException {
        AssetListener listener = createMock("Listener", AssetListener.class);
        listener.notifyPartial(eq(MY_ID), anyDouble());
        expectLastCall().andThrow(new TimeoutException());
        listener.notify(eq(MY_ID), anyObject(Asset.class));
        replay(listener);
        Logger.getAnonymousLogger().warning("Exception test started");
        testObject.get(MY_ID, listener);
        BufferedImage img = ImageIO.read(ZipFileAssetSupplierTest.class.getClassLoader().getResourceAsStream(TEST_IMAGE));
        ImageIO.write(img, "png", new File(USER_DIR + TEST_DIR + TEST_IMAGE));
        verify(listener);

        Logger.getAnonymousLogger().warning("Exception test ended");
    }
}
