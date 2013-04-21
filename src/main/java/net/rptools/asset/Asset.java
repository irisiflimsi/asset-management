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
public class Asset {
    /** Asset object */
    private Object main;

    /** Format to store an asset of this type. Used by writers. */
    private String format;
    
    /** Constructor taking only the main object as image(!) input */
    public Asset(Object main) {
        this.main = main;
        setFormat("png");
    }

    /** Getter. Currently this is identical to getClass of the main object */
    public Class<?> getType() {
        return main != null ? main.getClass() : null;
    }

    /** Getter */
    public String getFormat() {
        return format;
    }

    /** Setter */
    public void setFormat(String format) {
        this.format = format;
    }

    /** Getter */
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
