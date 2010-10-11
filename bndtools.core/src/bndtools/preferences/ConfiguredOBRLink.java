package bndtools.preferences;

import org.eclipse.jface.viewers.StyledString;

import bndtools.shared.OBRLink;

public class ConfiguredOBRLink implements OBRLink {

    private final String url;

    public ConfiguredOBRLink(String url) {
        this.url = url;
    }

    public StyledString getLabel() {
        return new StyledString(url);
    }

    public String getLink() {
        return url;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConfiguredOBRLink other = (ConfiguredOBRLink) obj;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        return true;
    }

}
