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

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import net.rptools.asset.intern.AssetImpl;
import net.rptools.asset.intern.AssetManagerImpl;
import net.rptools.asset.intern.supplier.FileAssetSupplier;
import net.rptools.asset.intern.supplier.ZipFileAssetSupplier;

import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.Matchers.*;

/**
 * Tests assume the images are not changed. If they do change, various verification conditions
 * need to be adjusted.
 * @author username
 */
public class ZipFileAssetSupplierTest {

    /** local file separator constant */
    private final static String SEP = System.getProperty("file.separator");

    final private static String TEST_IMAGE = "Test.png";
    final private static String TEST_ZIP = ".maptool" + SEP + "resources" + SEP + "test.zip";
    final private static String TEST_ZIP_FULL = System.getProperty("user.dir") + SEP + TEST_ZIP;

    private ZipFileAssetSupplier testObject;


    @Before
    public void setUp() throws Exception {
        InputStream source = ZipFileAssetSupplierTest.class.getClassLoader().getResourceAsStream("test.zip");
        OutputStream destination = new FileOutputStream(new File(TEST_ZIP_FULL));
        for (int b = source.read(); b != -1; b = source.read()) {
            destination.write(b);
        }
        testObject = new ZipFileAssetSupplier(AssetManagerImpl.getTotalProperties(null), TEST_ZIP);
    }

    @Test
    public void testTrivialMethods() {
        assertThat(testObject.canCreate(BufferedImage.class), is(true));
        assertThat(FileAssetSupplier.DEFAULT_PRIORITY, is(not(equalTo(testObject.getPriority()))));
        assertThat(testObject.has("1234"), is(true));
        assertThat(testObject.has("1235"), is(false));
    }

    @Test
    public void testRemove() {
        assertTrue(testObject.remove("1234"));
        assertThat(testObject.has("1234"), is(false));
        File zip = new File(TEST_ZIP_FULL);
        assertTrue(zip.exists());
        Logger.getAnonymousLogger().warning(">>> Length to check: " + zip.length());
        assertTrue(zip.length() > 150 && zip.length() < 200); // 168; be flexible (good idea?)
    }

    @Test
    public void testCreateRemove() throws IOException {
        BufferedImage img = ImageIO.read(ZipFileAssetSupplierTest.class.getClassLoader().getResourceAsStream(TEST_IMAGE));
        // create
        String id = testObject.create(new AssetImpl(img));
        assertThat(testObject.has("1234"), is(true));
        assertThat(testObject.has(id), is(true));
        // file check
        File zip = new File(TEST_ZIP_FULL);
        assertTrue(zip.exists());
        Logger.getAnonymousLogger().warning(">>> Length to check: " + zip.length());
        assertTrue(zip.length() > 4600 && zip.length() < 4800); // 4654; be flexible (good idea?)
        // remove
        assertTrue(testObject.remove(id));
        assertThat(testObject.has(id), is(false));
        zip = new File(TEST_ZIP_FULL);
        assertTrue(zip.exists());
        Logger.getAnonymousLogger().warning(">>> Length to check: " + zip.length());
        assertTrue(zip.length() > 2500 && zip.length() < 2600); // 2578; be flexible (good idea?)
    }

    @Test
    public void testUpdate() throws IOException {
        BufferedImage img = ImageIO.read(ZipFileAssetSupplierTest.class.getClassLoader().getResourceAsStream(TEST_IMAGE));
        // create
        String id = "1234";
        testObject.update(id, new AssetImpl(img));
        assertThat(testObject.has("1234"), is(true));
        // file check
        File zip = new File(TEST_ZIP_FULL);
        assertTrue(zip.exists());
        Logger.getAnonymousLogger().warning(">>> Length to check: " + zip.length());
        assertTrue(zip.length() > 2100 && zip.length() < 2300); // 2200; be flexible (good idea?)
    }
}
