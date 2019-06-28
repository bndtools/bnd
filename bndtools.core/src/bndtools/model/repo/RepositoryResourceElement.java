package bndtools.model.repo;

import org.bndtools.utils.resources.ResourceUtils;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;

import aQute.bnd.service.RepositoryPlugin;

public class RepositoryResourceElement {

	private final Resource					resource;
	private final String					name;
	private final RepositoryBundleVersion	repositoryBundleVersion;

	RepositoryResourceElement(RepositoryPlugin repoPlugin, Resource resource) {
		this.resource = resource;
		this.name = ResourceUtils.getIdentity(resource);
		this.repositoryBundleVersion = new RepositoryBundleVersion(new RepositoryBundle(repoPlugin, name),
			aQute.bnd.version.Version.parseVersion(getVersionString()));
	}

	public RepositoryBundleVersion getRepositoryBundleVersion() {
		return repositoryBundleVersion;
	}

	String getIdentity() {
		return name;
	}

	String getVersionString() {
		Version version = ResourceUtils.getVersion(resource);
		if (version == null)
			version = Version.emptyVersion;
		return version.toString();
	}

	public Resource getResource() {
		return resource;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((resource == null) ? 0 : resource.hashCode());
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
		RepositoryResourceElement other = (RepositoryResourceElement) obj;
		if (resource == null) {
			if (other.resource != null)
				return false;
		} else if (!resource.equals(other.resource))
			return false;
		return true;
	}

}
