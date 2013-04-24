package net.rptools.intern;

import static org.easymock.EasyMock.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import javax.imageio.ImageIO;

import net.rptools.asset.AssetListener;
import net.rptools.asset.AssetManagerFactory;
import net.rptools.asset.AssetSupplier;
import net.rptools.asset.intern.*;
import net.rptools.asset.intern.supplier.FileAssetSupplier;
import net.rptools.asset.intern.supplier.ZipFileAssetSupplier;

import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AssetManagerExtTest {
    final private static String SEP = System.getProperty("file.separator");
    final private static String USER_DIR = System.getProperty("user.dir") + SEP;
    final private static String TEST_DIR = ".maptool" + SEP + "resources" + SEP;
    final private static String TEST_DIR2 = ".maptool" + SEP + "resources2" + SEP;
    final private static String TEST_ZIP = ".maptool" + SEP + "resources" + SEP + "test.zip";
    final private static String TEST_ZIP_FULL = System.getProperty("user.dir") + SEP + TEST_ZIP;
    private AssetManager testObject;

    @Before
    public void setUp() throws Exception {
        tearDown();
        testObject = AssetManagerFactory.getInstance(null);
        fileSetup(TEST_DIR2);
    }

    @After
    public void tearDown() {
        File dir = new File(USER_DIR + TEST_DIR2);
        for (File file : dir.listFiles())
            file.delete();
    }

    @Test
    public void testCopyFile() throws Exception {
        AssetSupplier supplier2 = new FileAssetSupplier(AssetManager.getTotalProperties(null), TEST_DIR2);
        testCopy(supplier2);
        File dir = new File(USER_DIR + TEST_DIR2);
        assertThat(dir.list().length, equalTo(5)); // index + Test.png + 3 new assets
    }

    @Test
    public void testCopyUpdateFile() throws Exception {
        AssetSupplier supplier2 = new FileAssetSupplier(AssetManager.getTotalProperties(null), TEST_DIR2);   
        testCopyUpdate(supplier2);
        File dir = new File(USER_DIR + TEST_DIR2);
        assertThat(dir.list().length, equalTo(5)); // index + Test.png + 3 new assets
    }

    @Test
    public void testCopyZip() throws Exception {
        AssetSupplier supplier2 = new ZipFileAssetSupplier(AssetManager.getTotalProperties(null), TEST_ZIP);
        testCopy(supplier2);
        ZipFile zip = new ZipFile(TEST_ZIP_FULL);
        zip.entries();
    }

    @Test
    public void testCopyUpdateZip() throws Exception {
        AssetSupplier supplier2 = new ZipFileAssetSupplier(AssetManager.getTotalProperties(null), TEST_ZIP);   
        testCopyUpdate(supplier2);
    }

    private void testCopy(AssetSupplier testSupplier) throws Exception {
        // Objects and files
        BufferedImage example = fileSetup(TEST_DIR);

        // Mocks
        AssetSupplier supplier1 = new FileAssetSupplier(AssetManager.getTotalProperties(null), TEST_DIR);
        AssetListener listener = createMock("Listener", AssetListener.class);
        final List<String> ids = new ArrayList<String>();
        final CountDownLatch latch = new CountDownLatch(3);
        listener.notify(anyObject(String.class), anyObject(Asset.class));
        expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() {
                String id = (String) getCurrentArguments()[0];
                ids.add(id);
                latch.countDown();
                Logger.getAnonymousLogger().info("Created id=" + id);
                return null;
            }
        }).anyTimes();
        AssetListener listener2 = createMock("Listener2", AssetListener.class);
        final CountDownLatch latch2 = new CountDownLatch(3);
        listener2.notify(anyObject(String.class), anyObject(Asset.class));
        expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() {
                String id = (String) getCurrentArguments()[0];
                latch2.countDown();
                Logger.getAnonymousLogger().info("Copied id=" + id);
                return null;
            }
        }).anyTimes();

        replay(listener, listener2);

        // Prepare
        testObject.registerAssetSupplier(supplier1);
        for (int i = 0; i < 3; i++) {
            testObject.createAsset(new Asset(example), listener, false);
        }
        assertThat(latch.await(5000, TimeUnit.MILLISECONDS), is(true));

        // Test
        testObject.copyAssets(ids.toArray(new String[0]), testSupplier, listener2, false);
        assertThat(latch2.await(5000, TimeUnit.MILLISECONDS), is(true));

        for (int i = 0; i < 3; i++) {
            assertThat(testSupplier.has(ids.get(i)), is(true));
        }

        testObject.deregisterAssetSupplier(supplier1);
        verify(listener, listener2);
    }

    private void testCopyUpdate(AssetSupplier testSupplier) throws Exception {
        // Objects and files
        BufferedImage example = fileSetup(TEST_DIR);

        // Mocks
        AssetSupplier supplier1 = new FileAssetSupplier(AssetManager.getTotalProperties(null), TEST_DIR);
        AssetListener listener = createMock("Listener", AssetListener.class);
        final List<String> ids = new ArrayList<String>();
        final CountDownLatch latch = new CountDownLatch(3);
        listener.notify(anyObject(String.class), anyObject(Asset.class));
        expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() {
                String id = (String) getCurrentArguments()[0];
                ids.add(id);
                latch.countDown();
                Logger.getAnonymousLogger().info("Created id=" + id);
                return null;
            }
        }).anyTimes();
        AssetListener listener2 = createMock("Listener2", AssetListener.class);
        final CountDownLatch latch2 = new CountDownLatch(6);
        listener2.notify(anyObject(String.class), anyObject(Asset.class));
        expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() {
                String id = (String) getCurrentArguments()[0];
                latch2.countDown();
                Logger.getAnonymousLogger().info("Copied id=" + id);
                return null;
            }
        }).anyTimes();

        replay(listener, listener2);

        // Prepare
        testObject.registerAssetSupplier(supplier1);
        for (int i = 0; i < 3; i++) {
            testObject.createAsset(new Asset(example), listener, false);
        }
        assertThat(latch.await(5000, TimeUnit.MILLISECONDS), is(true));

        // Test (put assets in twice)
        testObject.copyAssets(ids.toArray(new String[0]), testSupplier, listener2, true);
        testObject.copyAssets(ids.toArray(new String[0]), testSupplier, listener2, true);
        assertThat(latch2.await(5000, TimeUnit.MILLISECONDS), is(true));

        for (int i = 0; i < 3; i++) {
            assertThat(testSupplier.has(ids.get(i)), is(true));
        }

        testObject.deregisterAssetSupplier(supplier1);
        verify(listener, listener2);
    }

    private BufferedImage fileSetup(String testdir) throws Exception {
        String TEST_IMAGE = "Test.png";
        String userDir = System.getProperty("user.dir") + SEP;
        File example = new File(userDir + testdir + TEST_IMAGE);
        URI uri = new URI(HttpAssetSupplierTest.TEST_IMAGE);
        InputStream in = uri.toURL().openConnection().getInputStream();
        OutputStream out = new FileOutputStream(example);
        for (int read = in.read(); read != -1; read = in.read())
            out.write(read);
        out.close();

        File index = new File(userDir + testdir + "index");
        PrintStream output = new PrintStream(new FileOutputStream(index));
        output.println("1234=" + TEST_IMAGE);
        output.close();
        return ImageIO.read(example);
    }
}
