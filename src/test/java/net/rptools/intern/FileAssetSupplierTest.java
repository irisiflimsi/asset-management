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
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

import javax.imageio.ImageIO;

import net.rptools.asset.intern.Asset;
import net.rptools.asset.intern.AssetManager;
import net.rptools.asset.intern.supplier.FileAssetSupplier;

import org.junit.*;

public class FileAssetSupplierTest {

    /** local file separator constant */
    private final static String SEP = System.getProperty("file.separator");

    private FileAssetSupplier testObject;
    private final static String TEST_DIR = ".maptool" + SEP + "resources" + SEP;
    private final static String TEST_IMAGE = "Test.png";
    private File example;
    
    @Before
    public void setUp() throws Exception {
        tearDown();
        String userDir = System.getProperty("user.dir") + SEP;
        example = new File(userDir + TEST_DIR + TEST_IMAGE);
        URI uri = new URI(HttpAssetSupplierTest.TEST_IMAGE);
        InputStream in = uri.toURL().openConnection().getInputStream();
        OutputStream out = new FileOutputStream(example);
        for (int read = in.read(); read != -1; read = in.read())
            out.write(read);
        out.close();

        File index = new File(userDir + TEST_DIR + "index");
        PrintStream output = new PrintStream(new FileOutputStream(index));
        output.println("1234=" + TEST_IMAGE);
        output.close();

        testObject = new FileAssetSupplier(AssetManager.getTotalProperties(null), TEST_DIR);
    }

    @After
    public void tearDown() {
        String userDir = System.getProperty("user.dir") + SEP;
        File dir = new File(userDir + TEST_DIR);
        for (File rm : dir.listFiles())
            rm.delete();
    }

    @Test
    public void testTrivialMethods() throws URISyntaxException, IOException {
        assertThat(testObject.canCache(new Asset(new BufferedImage(1,1,1))), is(false));
        assertThat(testObject.canRemove(null), is(false));
        assertThat(testObject.remove(null), is(false));
        assertThat(testObject.canCreate(BufferedImage.class), is(true));
        assertThat(FileAssetSupplier.DEFAULT_PRIORITY, not(equalTo(testObject.getPriority())));
        assertThat(testObject.has("1235"), is(false));
        verifyIndexEmpty();
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testCacheFails() {
        testObject.cache(null, null);
    }
    
    @Test
    public void testCreateDelete() throws IOException {
        BufferedImage asset = ImageIO.read(example);
        String id = testObject.create(new Asset(asset));
        assertThat(id, not(equalTo("1234")));
        BufferedImage saved = (BufferedImage) testObject.get(id, null).getMain();
        assertThat(asset.getWidth(), is(equalTo(saved.getWidth())));
        assertThat(asset.getHeight(), is(equalTo(saved.getHeight())));
        assertThat(testObject.canRemove(id.substring(1)), is(false));
        assertThat(testObject.canRemove(id), is(true));
        assertThat(testObject.remove(id), is(true));
        assertThat(testObject.canRemove(id), is(false));
        assertThat(testObject.remove(id), is(false));

        verifyIndexEmpty();
    }

    @Test
    public void testCreateDuplicateDelete() throws IOException {
        BufferedImage asset = ImageIO.read(example);
        String id = testObject.create(new Asset(asset));
        testObject.update(id, new Asset(asset));
        String id3 = testObject.create(new Asset(asset));
        assertThat(id, is(not(equalTo(id3))));

        // "Other" asset
        BufferedImage saved = (BufferedImage) testObject.get(id3, null).getMain();
        assertThat(asset.getWidth(), is(equalTo(saved.getWidth())));
        assertThat(asset.getHeight(), is(equalTo(saved.getHeight())));

        assertThat(testObject.remove(id), is(true));
        assertThat(testObject.remove(id), is(false));
        assertThat(testObject.remove(id3), is(true));

        verifyIndexEmpty();
    }

    // Further method tests are identical to Http and handled there.

    private static void verifyIndexEmpty() {
        assertThat(new File(System.getProperty("user.dir") + SEP + TEST_DIR + "index").length(), is(lessThan(100L))); // 58 + leeway (good?)
    }
}