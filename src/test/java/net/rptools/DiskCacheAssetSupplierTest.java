package net.rptools;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.awt.image.BufferedImage;

import net.rptools.asset.AssetListener;
import net.rptools.asset.AssetManager;
import net.rptools.asset.supplier.DiskCacheAssetSupplier;

import org.junit.Before;
import org.junit.Test;

public class DiskCacheAssetSupplierTest {

    private DiskCacheAssetSupplier testObject;

    @Before
    public void setUp() throws Exception {
        testObject = new DiskCacheAssetSupplier(AssetManager.getTotalProperties(null));
    }

    @Test
    public void testTrivialMethods() {
        assertFalse(testObject.canCache(Double.class));
        assertTrue(testObject.canCache(BufferedImage.class));
        assertFalse(testObject.canCreate(BufferedImage.class));
        assertNotSame(DiskCacheAssetSupplier.DEFAULT_PRIORITY, testObject.getPrio());
    }
    
    @Test
    public void testCreateDelete() {
        BufferedImage inAsset = new BufferedImage(1, 2, BufferedImage.TYPE_BYTE_GRAY);
        // create
        testObject.cache("test asset", inAsset);
        BufferedImage outAsset = testObject.get("test asset", BufferedImage.class, null);
        // verify
        assertEquals(outAsset.getWidth(), inAsset.getWidth());
        assertEquals(outAsset.getHeight(), inAsset.getHeight());
        // delete
        assertTrue(testObject.remove("test asset"));
        // verify some more
        assertFalse(testObject.has("test asset"));
        assertFalse(testObject.remove("test asset"));
    }

    @Test
    public void testCreateGetAsyncDelete() {
        BufferedImage inAsset = new BufferedImage(3, 4, BufferedImage.TYPE_BYTE_GRAY);
        // create
        testObject.cache("test asset", inAsset);
        @SuppressWarnings("unchecked")
        AssetListener<BufferedImage> listener = createMock("Listener", AssetListener.class);
        listener.notify(eq("test asset"), eq(inAsset));
        replay(listener);

        BufferedImage outAsset = testObject.get("test asset", BufferedImage.class, null);
        // verify
        assertEquals(outAsset.getWidth(), inAsset.getWidth());
        assertEquals(outAsset.getHeight(), inAsset.getHeight());
        // delete
        assertTrue(testObject.remove("test asset"));
        assertFalse(testObject.has("test asset"));
    }
}
