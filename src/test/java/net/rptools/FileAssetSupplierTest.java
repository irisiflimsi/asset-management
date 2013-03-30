package net.rptools;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import net.rptools.asset.AssetManager;
import net.rptools.asset.supplier.FileAssetSupplier;

import org.junit.Before;
import org.junit.Test;

public class FileAssetSupplierTest {

    /** local file separator constant */
    private final static String SEP = System.getProperty("file.separator");

    private FileAssetSupplier testObject;
    final public static String MY_TEST_IMAGE = ".maptool/assetcache/nurbcup2si.png";
    private File example;
    
    @Before
    public void setUp() throws Exception {
        testObject = new FileAssetSupplier(AssetManager.getTotalProperties(null));
        example = new File(System.getProperty("user.dir") + SEP + MY_TEST_IMAGE);
        if (!example.exists()) {
            URI uri = new URI(HttpAssetSupplierTest.MY_TEST_IMAGE);
            InputStream in = uri.toURL().openConnection().getInputStream();
            OutputStream out = new FileOutputStream(example);
            for (int read = in.read(); read != -1; read = in.read())
                out.write(read);
            out.close();
        }
    }

    @Test
    public void testTrivialMethods() {
        assertFalse(testObject.canCache(BufferedImage.class));
        assertFalse(testObject.canRemove(null));
        assertFalse(testObject.remove(null));
        assertFalse(testObject.canCache(BufferedImage.class));
        assertTrue(testObject.canCreate(BufferedImage.class));
        assertNotSame(FileAssetSupplier.DEFAULT_PRIORITY, testObject.getPrio());
        assertTrue(testObject.has("file://foo.bar"));
        assertFalse(testObject.has("ftp://foo.bar"));
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testCacheFails() {
        testObject.cache(null, null);
    }
    
    @Test
    public void testCreateDelete() throws IOException {
        BufferedImage asset = ImageIO.read(example);
        String id = testObject.create(null, asset);
        Logger.getAnonymousLogger().info("Added " + id);
        assertNotNull(id);
        BufferedImage saved = testObject.get(id, BufferedImage.class, null);
        assertEquals(asset.getWidth(), saved.getWidth());
        assertEquals(asset.getHeight(), saved.getHeight());
        assertFalse(testObject.canRemove(id.substring(1)));
        assertTrue(testObject.canRemove(id));
        assertTrue(testObject.remove(id));
        assertFalse(testObject.canRemove(id));
        assertFalse(testObject.remove(id));
    }

    // Further method tests are identical to Http and handled there.
}
