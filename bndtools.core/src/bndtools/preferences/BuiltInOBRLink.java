package bndtools.preferences;

import java.net.URL;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.viewers.StyledString;
import org.osgi.framework.Bundle;

import bndtools.shared.OBRLink;
import bndtools.utils.BundleUtils;

public class BuiltInOBRLink implements OBRLink {

    private final String bundleId;
    private final String label;
    private final URL url;

    public BuiltInOBRLink(IConfigurationElement element) {
        bundleId = element.getContributor().getName();
        label = element.getAttribute("label");
        url = getURL(element);
    }

    public StyledString getLabel() {
        StyledString label = new StyledString(this.label);
        label.append(" (" + bundleId + ")", StyledString.QUALIFIER_STYLER);

        return label;
    }

    private static URL getURL(IConfigurationElement element) {
        URL result = null;

        String bundleId = element.getContributor().getName();
        Bundle bundle = BundleUtils.findBundle(bundleId, null);

        if (bundle != null) {
            result = bundle.getEntry(element.getAttribute("resource"));
        }

        return result;
    }

    public String getLink() {
        return url == null ? null : url.toExternalForm();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bundleId == null) ? 0 : bundleId.hashCode());
        result = prime * result + ((label == null) ? 0 : label.hashCode());
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
        BuiltInOBRLink other = (BuiltInOBRLink) obj;
        if (bundleId == null) {
            if (other.bundleId != null)
                return false;
        } else if (!bundleId.equals(other.bundleId))
            return false;
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        return true;
    }



}
