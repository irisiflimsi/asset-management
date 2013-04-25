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
package net.rptools.asset;


/**
 * <p>
 * Wrapper for assets. Assets have their main object of interest, but also carry type information
 * that is used internally and storage format information. For instance, maps may be better stored
 * as JPGs, while markers are better stored as PNGs, because the former uses less memory but the
 * former provides transparency. Note that the equality definition(s) do not consider the format
 * field.
 * </p>
 * <p>
 * While returning assets not found will usually return a null, an asset that was found but could
 * not be converted into the desired format, will return a non-null object but its fields will be
 * null. So such objects are not considered corrupt
 * </p>
 * <p>
 * TODO: add license field that can be populated, when an appropriate license file is found
 * in the repository. Note that when assets are copied around, licenses shall NOT be copied.
 * Compliance to licenses is something the user has to take care for. Potentially copying into
 * another repository may violate licenses and the developer(s) of this package do not take any
 * responsibility for such license violation. If a license does not allow copying the user should
 * refrain from doing so. It is suggested to implement licenses only as references, not as full
 * text.
 * </p>
 * @author username
 */
public interface Asset {
    /** Getter. Currently this is identical to getClass of the main object */
    public Class<?> getType();

    /** Getter */
    public String getFormat();

    /** Setter */
    public void setFormat(String format);

    /** Getter */
    public Object getMain();
}
