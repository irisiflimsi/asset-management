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
package net.rptools.asset.supplier;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.UUID;

import javax.imageio.ImageIO;

/**
 * This class provides access to File URLs.
 * We only provide BufferedImages currently.
 * @author username
 */
public class FileAssetSupplier extends  AbstractURLAssetSupplier {
    /** Directory we can work in */
    private String baseDir;

    /** local file separator constant */
    private final static String SEP = System.getProperty("file.separator");

    /**
     * C'tor. Loads properties.
     * @param override properties to take precendence over default ones
     * @throws IOException can't load properties
     * @throws NumberFormatException if certain properties aren't numbers
     */
    public FileAssetSupplier(Properties override) throws IOException {
        super(override);
        this.notifyInterval = Long.parseLong(properties.getProperty(FileAssetSupplier.class.getSimpleName() + ".notifyInterval"));
        this.prio = Integer.parseInt(properties.getProperty(FileAssetSupplier.class.getSimpleName() + ".priority"));
        this.baseDir = System.getProperty("user.dir") + SEP + properties.getProperty(FileAssetSupplier.class.getSimpleName() + ".directory");
    }

    @Override
    public boolean has(String id) {
        return id.startsWith("file://");
    }

    @Override
    public boolean canCreate(Class<?> clazz) {
        return BufferedImage.class.equals(clazz);
    }

    @Override
    public <T> String create(String idHint, T obj) {
        // TODO don't ignore hint
        try {
            UUID id = UUID.randomUUID();
            BufferedImage img = BufferedImage.class.cast(obj);
            File f = new File(baseDir + SEP + id.toString());
            ImageIO.write(img, "png", f);
            return f.toURI().toString().replace("file:", "file://"); // java ommits the URI-authority
        }
        catch (IOException e) {
            LOGGER.warn("Remove failed", e);
            return null;
        }
    }

    @Override
    public boolean canRemove(String id) {
        try {
            return (id != null) && new File(new URI(id)).exists();
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean remove(String id) {
        if (id == null)
            return false;
        try {
            File f = new File(new URI(id));
            return f.delete();
        }
        catch (Exception e) {
            LOGGER.warn("Remove failed", e);
        }
        return false;
    }

}
