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

import net.rptools.asset.AssetListener;
import net.rptools.asset.intern.Asset;
import net.rptools.asset.intern.AssetManager;
import net.rptools.asset.intern.supplier.MemCacheAssetSupplier;

import org.junit.Before;
import org.junit.Test;

public class MemCacheAssetSupplierTest {

    private MemCacheAssetSupplier testObject;

    @Before
    public void setUp() throws Exception {
        testObject = new MemCacheAssetSupplier(AssetManager.getTotalProperties(null));
    }

    @Test
    public void testTrivialMethods() {
        assertThat(testObject.canCache(new Asset(Math.PI)), is(true));
        assertThat(testObject.canCache(new Asset(new BufferedImage(1,1,1))), is(true));
        assertThat(testObject.canCreate(BufferedImage.class), is(false));
        assertThat(MemCacheAssetSupplier.DEFAULT_PRIORITY, is(not(equalTo(testObject.getPriority()))));
    }
    
    @Test(expected=UnsupportedOperationException.class)
    public void testCreateFails() {
        testObject.create(null);
    }
    
    @Test
    public void testCacheRemove() {
        BufferedImage inAsset = new BufferedImage(1, 2, BufferedImage.TYPE_BYTE_GRAY);
        String TESTID = "test asset";
        // create
        testObject.cache(TESTID, new Asset(inAsset));
        assertThat(testObject.has(TESTID), is(true));
        BufferedImage outAsset = (BufferedImage) testObject.get(TESTID, null).getMain();
        // verify
        assertThat(outAsset.getWidth(), equalTo(inAsset.getWidth()));
        assertThat(outAsset.getHeight(), equalTo(inAsset.getHeight()));
        // remove
        assertThat(testObject.canRemove(TESTID), is(true));
        assertThat(testObject.remove(TESTID), is(true));
        // verify some more
        assertThat(testObject.get(TESTID, null), is(nullValue()));
        assertThat(testObject.remove(TESTID), is(false));
    }

    @Test
    public void testCreateGetAsyncRemove() {
        BufferedImage inAsset = new BufferedImage(3, 4, BufferedImage.TYPE_BYTE_GRAY);
        String TESTID = "test asset";
        // create
        testObject.cache(TESTID, new Asset(inAsset));
        AssetListener listener = createMock("Listener", AssetListener.class);
        listener.notify(eq(TESTID), eq(new Asset(inAsset)));
        replay(listener);

        BufferedImage outAsset = (BufferedImage) testObject.get(TESTID, null).getMain();
        // verify
        assertThat(outAsset.getWidth(), equalTo(inAsset.getWidth()));
        assertThat(outAsset.getHeight(), equalTo(inAsset.getHeight()));
        // delete
        assertThat(testObject.canRemove(TESTID), is(true));
        assertThat(testObject.remove(TESTID), is(true));
        assertThat(testObject.get(TESTID, null), is(nullValue()));
    }
}
