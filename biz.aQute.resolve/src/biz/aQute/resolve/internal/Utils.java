package biz.aQute.resolve.internal;

import java.util.List;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

public class Utils {
    static Version findIdentityVersion(Resource resource) {
        List<Capability> idCaps = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
        if (idCaps == null || idCaps.isEmpty())
            throw new IllegalArgumentException("Resource has no identity capability.");
        if (idCaps.size() > 1)
            throw new IllegalArgumentException("Resource has more than one identity capability.");

        Object versionObj = idCaps.get(0).getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
        if (versionObj == null)
            return Version.emptyVersion;

        if (versionObj instanceof Version)
            return (Version) versionObj;

        if (versionObj instanceof String)
            return Version.parseVersion((String) versionObj);

        throw new IllegalArgumentException("Unable to convert type for version attribute: " + versionObj.getClass());
    }
}
