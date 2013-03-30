package net.rptools;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import net.rptools.asset.AssetListener;
import net.rptools.asset.AssetManager;
import net.rptools.asset.supplier.HttpAssetSupplier;

import org.junit.Before;
import org.junit.Test;

public class HttpAssetSupplierTest {

    private HttpAssetSupplier testObject;
    final public static String MY_TEST_IMAGE = "http://www.w3.org/Graphics/PNG/nurbcup2si.png";
    
    @Before
    public void setUp() throws Exception {
        testObject = new HttpAssetSupplier(AssetManager.getTotalProperties(null));
    }

    @Test
    public void testTrivialMethods() {
        assertFalse(testObject.canCache(BufferedImage.class));
        assertFalse(testObject.canRemove(null));
        assertFalse(testObject.remove(null));
        assertFalse(testObject.canCache(BufferedImage.class));
        assertFalse(testObject.canCreate(BufferedImage.class));
        assertNotSame(HttpAssetSupplier.DEFAULT_PRIORITY, testObject.getPrio());
        assertTrue(testObject.has("http://foo.bar"));
        assertFalse(testObject.has("ftp://foo.bar"));
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testCreateFails() {
        testObject.cache(null, null);
    }
    
    @Test
    // Requires "internet" access
    public void testGet() {
        BufferedImage png = testObject.get(MY_TEST_IMAGE, BufferedImage.class, null);
        assertNotNull(png);
        assertTrue(png.getHeight() * png.getWidth() > 1);
    }

    @Test
    // Requires "internet" access
    public void testGetAsync() throws TimeoutException {
        @SuppressWarnings("unchecked")
        AssetListener<BufferedImage> listener = createMock("Listener", AssetListener.class);
        listener.notifyPartial(eq(MY_TEST_IMAGE), anyDouble());
        expectLastCall().anyTimes();
        listener.notify(eq(MY_TEST_IMAGE), anyObject(BufferedImage.class));
        replay(listener);

        BufferedImage png = testObject.get(MY_TEST_IMAGE, BufferedImage.class, listener);

        assertNotNull(png);
        assertTrue(png.getHeight() * png.getWidth() > 1);
        verify(listener);
    }

    @Test
    // Requires "internet" access. This test may fail, if the the retrieval of the image
    // is particularly fast.
    public void testGetAsyncAbort() throws TimeoutException {
        @SuppressWarnings("unchecked")
        AssetListener<BufferedImage> listener = createMock("Listener", AssetListener.class);
        listener.notifyPartial(eq(MY_TEST_IMAGE), anyDouble());
        expectLastCall().andThrow(new TimeoutException());
        listener.notify(eq(MY_TEST_IMAGE), anyObject(BufferedImage.class));
        replay(listener);
        Logger.getAnonymousLogger().warning("The following exceptions and their stack are part of the test!");
        BufferedImage png = testObject.get(MY_TEST_IMAGE, BufferedImage.class, listener);

        assertNull(png);
        verify(listener);
        Logger.getAnonymousLogger().warning("Exception test ended");
    }
}
