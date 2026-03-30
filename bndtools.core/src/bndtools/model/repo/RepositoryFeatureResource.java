package bndtools.model.repo;

import java.util.Objects;

import org.bndtools.utils.resources.ResourceUtils;
import org.eclipse.core.runtime.IAdaptable;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import aQute.bnd.service.RepositoryPlugin;

/**
 * Represents an Eclipse feature resource from an R5 repository search.
 * This is a lightweight wrapper that doesn't require the full Feature object
 * to be parsed, making it suitable for displaying feature search results.
 */
public class RepositoryFeatureResource implements IAdaptable {

	private final Resource resource;
	private final String id;
	private final String version;
	private final RepositoryPlugin repoPlugin;

	public RepositoryFeatureResource(RepositoryPlugin repoPlugin, Resource resource) {
		this.repoPlugin = repoPlugin;
		this.resource = resource;
		this.id = ResourceUtils.getIdentity(resource);
		
		Version osgiVersion = ResourceUtils.getVersion(resource);
		if (osgiVersion == null)
			osgiVersion = Version.emptyVersion;
		this.version = osgiVersion.toString();
	}

	public Resource getResource() {
		return resource;
	}

	public String getId() {
		return id;
	}

	public String getVersion() {
		return version;
	}

	public RepositoryPlugin getRepo() {
		return repoPlugin;
	}

	public String getLabel() {
		// Try to get label from identity capability attributes
		for (Capability cap : resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE)) {
			Object label = cap.getAttributes().get("label");
			if (label != null) {
				return label.toString();
			}
		}
		return id;
	}

	public String getProviderName() {
		// Try to get provider-name from identity capability attributes
		for (Capability cap : resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE)) {
			Object providerName = cap.getAttributes().get("provider-name");
			if (providerName != null) {
				return providerName.toString();
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == Resource.class) {
			return (T) resource;
		}
		if (adapter == RepositoryPlugin.class) {
			return (T) repoPlugin;
		}
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(resource, repoPlugin);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RepositoryFeatureResource other = (RepositoryFeatureResource) obj;
		return Objects.equals(resource, other.resource) && Objects.equals(repoPlugin, other.repoPlugin);
	}

	@Override
	public String toString() {
		return "RepositoryFeatureResource [repo=" + repoPlugin.getName() + ", id=" + id + ", version=" + version + "]";
	}
}
