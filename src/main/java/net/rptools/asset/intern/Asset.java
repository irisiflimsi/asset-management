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
package net.rptools.asset.intern;

/**
 * See the exported class. Used for poor-man's component separation.
 * @author username
 */
public class Asset implements net.rptools.asset.Asset{
    /** Asset object */
    private Object main;

    /** Format to store an asset of this type. Used by writers. */
    private String format;
    
    /** Constructor taking only the main object as image(!) input */
    public Asset(Object main) {
        this.main = main;
        setFormat("png");
    }

    @Override
    public Class<?> getType() {
        return main != null ? main.getClass() : null;
    }

    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public Object getMain() {
        return main;
    }

    @Override
    public int hashCode() {
        // We are ignoring format on purpose
        final int prime = 31;
        int result = 1;
        result = prime * result + ((main == null) ? 0 : main.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        // We are ignoring format on purpose
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Asset other = (Asset) obj;
        if (main == null) {
            if (other.main != null) return false;
        }
        else if (!main.equals(other.main)) return false;
        return true;
    }
}
