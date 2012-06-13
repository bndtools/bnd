package org.bndtools.core.utils.resources;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;

public final class ResourceUtils {

    public static Capability getIdentityCapability(Resource resource) throws IllegalArgumentException {
        List<Capability> caps = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
        if (caps == null || caps.isEmpty())
            throw new IllegalArgumentException("Resource has no identity");
        if (caps.size() > 1)
            throw new IllegalArgumentException("Resource is schizophrenic");
        return caps.get(0);
    }

    public static String getIdentity(Capability identityCapability) throws IllegalArgumentException {
        String id = (String) identityCapability.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
        if (id == null)
            throw new IllegalArgumentException("Resource identity capability has missing identity attribute");
        return id;
    }

    public static final Version getVersion(Capability identityCapability) throws IllegalArgumentException {
        Object versionObj = identityCapability.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
        if (versionObj instanceof Version)
            return (Version) versionObj;

        if (versionObj == null || versionObj instanceof String)
            return Version.parseVersion((String) versionObj);

        throw new IllegalArgumentException("Resource identity capability has version attribute with incorrect type: " + versionObj.getClass());
    }

    public static final String getIdentity(Resource resource) throws IllegalArgumentException {
        return getIdentity(getIdentityCapability(resource));
    }

    public static final Version getVersion(Resource resource) throws IllegalArgumentException {
        return getVersion(getIdentityCapability(resource));
    }

    public static Capability getContentCapability(Resource resource) throws IllegalArgumentException {
        List<Capability> caps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        if (caps == null || caps.isEmpty())
            throw new IllegalArgumentException("Resource has no content");
        if (caps.size() > 1)
            throw new IllegalArgumentException("Resource is schizophrenic");
        return caps.get(0);
    }

    public static URI getURI(Capability contentCapability) {
        Object uriObj = contentCapability.getAttributes().get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
        if (uriObj == null)
            throw new IllegalArgumentException("Resource content capability has missing URL attribute");

        if (uriObj instanceof URI)
            return (URI) uriObj;

        try {
            if (uriObj instanceof URL)
                return ((URL) uriObj).toURI();

            if (uriObj instanceof String)
                return new URI((String) uriObj);

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Resource content capability has illegal URL attribute", e);
        }

        throw new IllegalArgumentException("Resource content capability has URL attribute with incorrect type: " + uriObj.getClass());
    }
}
