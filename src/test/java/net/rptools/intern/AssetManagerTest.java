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

import java.util.UUID;

import net.rptools.asset.AssetListener;
import net.rptools.asset.AssetManagerFactory;
import net.rptools.asset.AssetSupplier;
import net.rptools.asset.intern.*;

import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AssetManagerTest {

    private AssetManager testObject;
    private AssetSupplier mock1;
    private AssetSupplier mock2;

    @Before
    public void setUp() throws Exception {
        testObject = AssetManagerFactory.getInstance(null);
        mock1 = createMock("Mock1", AssetSupplier.class);
        mock2 = createMock("Mock2", AssetSupplier.class);
        expect(mock1.getPriority()).andReturn(1).anyTimes();
        expect(mock2.getPriority()).andReturn(2).anyTimes();
    }
    
    @After
    public void tearDown() {
        testObject.deregisterAssetSupplier(mock1);
        testObject.deregisterAssetSupplier(mock2);
    }

    @Test
    public void testRegisterDeregister() {
        assertThat(testObject, is(not(nullValue())));
        replay(mock1, mock2);
        
        testObject.registerAssetSupplier(null); //
        testObject.registerAssetSupplier(mock1); // mock1
        testObject.registerAssetSupplier(mock2); // mock1, mock2
        testObject.registerAssetSupplier(mock1); // mock1, mock2
        testObject.registerAssetSupplier(null); // mock1, mock2
        testObject.deregisterAssetSupplier(null); // mock1, mock2
        testObject.deregisterAssetSupplier(mock1); // mock2
        testObject.deregisterAssetSupplier(mock2); //
        testObject.registerAssetSupplier(null); //
        testObject.deregisterAssetSupplier(null); //

        verify(mock1, mock2);
    }

    @Test
    public void testRegisterTwice() {
        assertThat(testObject, is(not(nullValue())));
        replay(mock1, mock2);

        testObject.registerAssetSupplier(mock1); // mock1

        AssetSupplier mock1a = createMock("Mock1a", AssetSupplier.class);
        expect(mock1a.getPriority()).andReturn(1).anyTimes();
        replay(mock1a);

        try {
            testObject.registerAssetSupplier(mock1a); // Error
        }
        catch (RuntimeException e) {
            assertThat(e.getMessage().toString().length(), is(greaterThan(0)));
        }

        verify(mock1, mock1a, mock2);
    }
    
    @Test
    public void testGetAsset() {
        assertThat(testObject, is(not(nullValue())));

        String id = UUID.randomUUID().toString();
        expect(mock1.has(id)).andReturn(true).anyTimes();
        expect(mock1.get(id, null)).andReturn(new Asset(Math.PI)).anyTimes();
        expect(mock2.has(id)).andReturn(false).anyTimes();
        expect(mock2.canCache(anyObject(Asset.class))).andReturn(false).anyTimes();
        replay(mock1, mock2);

        testObject.registerAssetSupplier(mock1);
        testObject.registerAssetSupplier(mock2);

        assertThat((Double)testObject.getAsset(id, true).getMain(), is((Double) Math.PI));
        verify(mock1, mock2);
    }

    @Test
    public void testGetAssetTwoHaveIt() {
        assertThat(testObject, is(not(nullValue())));

        String id = UUID.randomUUID().toString();
        expect(mock2.get(id, null)).andReturn(new Asset(Math.PI));
        expect(mock2.has(id)).andReturn(true);
        replay(mock1, mock2);

        testObject.registerAssetSupplier(mock1);
        testObject.registerAssetSupplier(mock2);

        assertThat((Double)testObject.getAsset(id, false).getMain(), is((Double) Math.PI));
        verify(mock1, mock2);
    }

    @Test
    public void testGetAssetAsync() throws InterruptedException {
        assertThat(testObject, is(not(nullValue())));

        final String id = UUID.randomUUID().toString();
        final AssetListener mockListener = createMock("Listener", AssetListener.class);
        expect(mock1.has(id)).andReturn(true);
        IAnswer<Asset> answer = new IAnswer<Asset>() {
            @Override
            public Asset answer() throws Throwable {
                mockListener.notify(id, new Asset(Math.PI));
                return new Asset(Math.PI);
            }
        };
        expect(mock1.get(id, mockListener)).andAnswer(answer);
        mockListener.notify(eq(id), eq(new Asset(Math.PI)));
        replay(mock1, mock2, mockListener);

        testObject.registerAssetSupplier(mock1); // mock1

        testObject.getAssetAsync(id, mockListener, true);
        Thread.sleep(200); // Fake
        verify(mock1, mock2, mockListener);
    }
}
