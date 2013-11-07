package net.rptools.intern;

import static org.easymock.EasyMock.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import net.rptools.asset.*;
import net.rptools.asset.intern.*;
import net.rptools.asset.intern.supplier.FileAssetSupplier;
import net.rptools.asset.intern.supplier.ZipFileAssetSupplier;

import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AssetManagerExtTest extends TestConstants {
    private AssetManager testObject;

    @Before
    public void setUp() throws Exception {
        tearDown();
        testObject = AssetManagerFactory.getInstance(null);
        fileSetup(TEST_DIR2);
    }

    @After
    public void tearDown() throws Exception {
        File dir = new File(USER_DIR + TEST_DIR2);
        for (File file : dir.listFiles())
            file.delete();
    }

    @Test
    public void testCopyFile() throws Exception {
        AssetSupplier supplier2 = new FileAssetSupplier(AssetManagerImpl.getTotalProperties(null), TEST_DIR2);
        testCopy(supplier2);
        File dir = new File(USER_DIR + TEST_DIR2);
        assertThat(dir.list().length, equalTo(4)); // index + 3 assets
    }

    @Test
    public void testCopyUpdateFile() throws Exception {
        AssetSupplier supplier2 = new FileAssetSupplier(AssetManagerImpl.getTotalProperties(null), TEST_DIR2);   
        testCopyUpdate(supplier2);
        File dir = new File(USER_DIR + TEST_DIR2);
        assertThat(dir.list().length, equalTo(4)); // index + 3 assets
    }

    @Test
    public void testCopyZip() throws Exception {
        AssetSupplier supplier2 = new ZipFileAssetSupplier(AssetManagerImpl.getTotalProperties(null), TEST_ZIP);
        testCopy(supplier2);
        ZipFile zip = new ZipFile(TEST_ZIP_FULL);
        zip.entries();
    }

    @Test
    public void testCopyUpdateZip() throws Exception {
        AssetSupplier supplier2 = new ZipFileAssetSupplier(AssetManagerImpl.getTotalProperties(null), TEST_ZIP);   
        testCopyUpdate(supplier2);
    }

    private void testCopy(AssetSupplier testSupplier) throws Exception {
        // Objects and files
        BufferedImage example = fileSetup(TEST_DIR);

        // Mocks
        AssetSupplier supplier1 = new FileAssetSupplier(AssetManagerImpl.getTotalProperties(null), TEST_DIR);
        AssetListener listener = createMock("Listener", AssetListener.class);
        final List<String> ids = new ArrayList<String>();
        final CountDownLatch latch = new CountDownLatch(3);
        listener.notify(anyObject(String.class), anyObject(AssetImpl.class));
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
        listener2.notify(anyObject(String.class), anyObject(AssetImpl.class));
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
            testObject.createAsset(new AssetImpl(example), listener, false);
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
        AssetSupplier supplier1 = new FileAssetSupplier(AssetManagerImpl.getTotalProperties(null), TEST_DIR);
        AssetListener listener = createMock("Listener", AssetListener.class);
        final List<String> ids = new ArrayList<String>();
        final CountDownLatch latch = new CountDownLatch(3);
        listener.notify(anyObject(String.class), anyObject(AssetImpl.class));
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
        listener2.notify(anyObject(String.class), anyObject(AssetImpl.class));
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
            testObject.createAsset(new AssetImpl(example), listener, false);
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

    protected BufferedImage fileSetup(String testdir) throws Exception {
        BufferedImage img = ImageIO.read(ZipFileAssetSupplierTest.class.getClassLoader().getResourceAsStream(TEST_IMAGE));
        ImageIO.write(img, "png", new File(USER_DIR + TEST_DIR + TEST_IMAGE));

        File index = new File(USER_DIR + testdir + "index");
        PrintStream output = new PrintStream(new FileOutputStream(index));
        output.println("1234=" + TEST_IMAGE);
        output.close();
        return img;
    }
}
