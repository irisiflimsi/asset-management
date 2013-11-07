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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import net.rptools.asset.intern.AssetImpl;
import net.rptools.asset.intern.AssetManagerImpl;
import net.rptools.asset.intern.supplier.DiskCacheAssetSupplier;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class DiskCacheAssetSupplierTest {

    private DiskCacheAssetSupplier testObject;

    @Before
    public void setUp() throws Exception {
        testObject = new DiskCacheAssetSupplier(AssetManagerImpl.getTotalProperties(null));
    }

    @AfterClass
    public static void tearDown() throws URISyntaxException {
        File dir = new File(new URI("file://" + System.getProperty("user.dir") + "/.maptool/assetcache"));
        for (File rm : dir.listFiles())
            rm.delete();
    }

    @Test
    public void testTrivialMethods() {
        assertThat(testObject.canCreate(BufferedImage.class), is(true));
        assertThat(testObject.canRemove(null), is(false));
        assertThat(testObject.remove(null), is(false));
        assertThat(DiskCacheAssetSupplier.DEFAULT_PRIORITY, is(not(equalTo(testObject.getPriority()))));
    }
    
    @Test
    public void testCache() {
        BufferedImage inAsset = new BufferedImage(1, 2, BufferedImage.TYPE_BYTE_GRAY);
        String TESTID = "test-asset";
        // create
        testObject.update(TESTID, new AssetImpl(inAsset));
        assertThat(testObject.has(TESTID), is(true));
        BufferedImage outAsset = (BufferedImage) testObject.get(TESTID, null).getMain();
        // verify
        assertThat(outAsset.getWidth(), is(equalTo(inAsset.getWidth())));
        assertThat(outAsset.getHeight(), is(equalTo(inAsset.getHeight())));
    }

    @Test
    public void testCacheDelete() {
        BufferedImage inAsset = new BufferedImage(3, 4, BufferedImage.TYPE_BYTE_GRAY);
        String TESTID = "test-asset";
        // create
        testObject.update(TESTID, new AssetImpl(inAsset));
        // verify delete
        assertThat(testObject.canRemove(TESTID), is(true));
        assertThat(testObject.remove(TESTID), is(true));
        // verify some more
        assertThat(testObject.remove(TESTID), is(false));
    }
    
    @Test
    public void testPrune() throws InterruptedException {
        BufferedImage inAsset = new BufferedImage(3, 4, BufferedImage.TYPE_BYTE_GRAY);
        String TESTID = "test-asset-";
        // create
        testObject.update(TESTID + 1, new AssetImpl(inAsset));
        Thread.sleep(1100); // We need to get a difference in the last access time
        testObject.update(TESTID + 2, new AssetImpl(inAsset));
        Thread.sleep(1100); // We need to get a difference in the last access time
        testObject.update(TESTID + 3, new AssetImpl(inAsset));
        testObject.prune(70);
        assertThat(testObject.has(TESTID + 1), is(false));
        assertThat(testObject.has(TESTID + 2), is(false));
        assertThat(testObject.has(TESTID + 3), is(true));
    }
}
