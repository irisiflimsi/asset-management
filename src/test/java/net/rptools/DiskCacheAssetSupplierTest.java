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
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import net.rptools.asset.AssetListener;
import net.rptools.asset.AssetManager;
import net.rptools.asset.supplier.DiskCacheAssetSupplier;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.Matchers.*;

public class DiskCacheAssetSupplierTest {

    private DiskCacheAssetSupplier testObject;

    @Before
    public void setUp() throws Exception {
        testObject = new DiskCacheAssetSupplier(AssetManager.getTotalProperties(null));
    }

    @AfterClass
    public static void tearDown() throws URISyntaxException {
        File dir = new File(new URI("file://" + System.getProperty("user.dir") + "/.maptool/assetcache"));
        for (File rm : dir.listFiles())
            rm.delete();
    }

    @Test
    public void testTrivialMethods() {
        assertThat(testObject.canCache(Double.class), is(false));
        assertThat(testObject.canCache(BufferedImage.class), is(true));
        assertThat(testObject.canCreate(BufferedImage.class), is(false));
        assertThat(testObject.canRemove(null), is(false));
        assertThat(testObject.remove(null), is(false));
        assertThat(DiskCacheAssetSupplier.DEFAULT_PRIORITY, is(not(equalTo(testObject.getPriority()))));
    }
    
    @Test
    public void testCache() {
        BufferedImage inAsset = new BufferedImage(1, 2, BufferedImage.TYPE_BYTE_GRAY);
        String TESTID = "test-asset";
        // create
        testObject.cache(TESTID, inAsset);
        assertThat(testObject.has(TESTID), is(true));
        BufferedImage outAsset = testObject.get(TESTID, BufferedImage.class, null);
        // verify
        assertThat(outAsset.getWidth(), is(equalTo(inAsset.getWidth())));
        assertThat(outAsset.getHeight(), is(equalTo(inAsset.getHeight())));
    }

    @Test
    public void testCreateGetAsyncDelete() {
        BufferedImage inAsset = new BufferedImage(3, 4, BufferedImage.TYPE_BYTE_GRAY);
        String TESTID = "test-asset";
        // create
        testObject.cache(TESTID, inAsset);
        @SuppressWarnings("unchecked")
        AssetListener<BufferedImage> listener = createMock("Listener", AssetListener.class);
        listener.notify(eq(TESTID), eq(inAsset));
        replay(listener);

        BufferedImage outAsset = testObject.get(TESTID, BufferedImage.class, null);
        // verify
        assertThat(outAsset.getWidth(), is(equalTo(inAsset.getWidth())));
        assertThat(outAsset.getHeight(), is(equalTo(inAsset.getHeight())));
    }
}
