package net.rptools;

import java.util.UUID;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.easymock.IAnswer;
import org.junit.*;

import net.rptools.asset.AssetListener;
import net.rptools.asset.AssetManager;
import net.rptools.asset.AssetSupplier;

public class AssetManagerTest {

    private AssetManager testObject;
    private AssetSupplier mock1;
    private AssetSupplier mock2;

    @Before
    public void setUp() throws Exception {
        testObject = AssetManager.getInstance(null);
        mock1 = createMock("Mock1", AssetSupplier.class);
        mock2 = createMock("Mock2", AssetSupplier.class);
        expect(mock1.getPrio()).andReturn(1).anyTimes();
        expect(mock2.getPrio()).andReturn(2).anyTimes();
    }
    
    @After
    public void tearDown() {
        testObject.deregisterAssetSupplier(mock1);
        testObject.deregisterAssetSupplier(mock2);
    }

    @Test
    public void testRegisterDeregister() {
        assertNotNull(testObject);
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
        assertNotNull(testObject);
        replay(mock1, mock2);

        testObject.registerAssetSupplier(mock1); // mock1

        AssetSupplier mock1a = createMock("Mock1a", AssetSupplier.class);
        expect(mock1a.getPrio()).andReturn(1).anyTimes();
        replay(mock1a);

        try {
            testObject.registerAssetSupplier(mock1a); // Error
        }
        catch (RuntimeException e) {
            assertTrue(e.getMessage().toString().length() > 0);
        }

        verify(mock1, mock1a, mock2);
    }
    
    @Test
    public void testGetAsset() {
        assertNotNull(testObject);

        String id = UUID.randomUUID().toString();
        expect(mock1.has(id)).andReturn(true).anyTimes();
        expect(mock1.get(id, Double.class, null)).andReturn(Math.PI).anyTimes();
        expect(mock2.has(id)).andReturn(false).anyTimes();
        replay(mock1, mock2);

        testObject.registerAssetSupplier(mock1); // mock1
        testObject.registerAssetSupplier(mock2); // mock2

        assertEquals(testObject.getAsset(id, Double.class), (Double) Math.PI);
        verify(mock1, mock2);
    }

    @Test
    public void testGetAssetTwoHaveIt() {
        assertNotNull(testObject);

        String id = UUID.randomUUID().toString();
        expect(mock1.has(id)).andReturn(true);
        expect(mock1.get(id, Double.class, null)).andReturn(Math.PI);
        expect(mock2.has(id)).andReturn(true);
        expect(mock2.wantOverride(id)).andReturn(false);
        replay(mock1, mock2);

        testObject.registerAssetSupplier(mock1); // mock1
        testObject.registerAssetSupplier(mock2); // mock2

        assertEquals(testObject.getAsset(id, Double.class), (Double) Math.PI);
        verify(mock1, mock2);
    }

    @Test
    public void testGetAssetTwoHaveItOverride() {
        assertNotNull(testObject);

        String id = UUID.randomUUID().toString();
        expect(mock1.has(id)).andReturn(true);
        expect(mock1.get(id, Double.class, null)).andReturn(-Math.PI);
        expect(mock1.canCache(Double.class)).andReturn(false);
        expect(mock2.has(id)).andReturn(true);
        expect(mock2.wantOverride(id)).andReturn(true);
        expect(mock2.get(id, Double.class, null)).andReturn(Math.PI);
        replay(mock1, mock2);

        testObject.registerAssetSupplier(mock1); // mock1
        testObject.registerAssetSupplier(mock2); // mock2
        assertEquals(testObject.getAsset(id, Double.class), (Double) Math.PI);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetAssetAsync() {
        assertNotNull(testObject);

        final String id = UUID.randomUUID().toString();
        final AssetListener<Double> mockListener = createMock("Listener", AssetListener.class);
        expect(mock1.has(id)).andReturn(true);
        IAnswer<Double> answer = new IAnswer<Double>() {
            @Override
            public Double answer() throws Throwable {
                mockListener.notify(id, Math.PI);
                return Math.PI;
            }
        };
        expect(mock1.get(id, Double.class, mockListener)).andAnswer(answer);
        mockListener.notify(id, Math.PI);
        replay(mock1, mock2, mockListener);

        testObject.registerAssetSupplier(mock1); // mock1

        testObject.getAssetAsync(id, Double.class, mockListener);
        try { Thread.sleep(200); } catch (InterruptedException e) { } // Fake
        verify(mock1, mock2, mockListener);
    }
}
