package bndtools.model.repo;

import java.util.Objects;

import org.bndtools.utils.resources.ResourceUtils;
import org.eclipse.core.runtime.IAdaptable;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;

import aQute.bnd.service.RepositoryPlugin;

public class RepositoryResourceElement implements ResourceProvider, IAdaptable {

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

	@Override
	public Resource getResource() {
		return resource;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {

		// this is triggered e.g. when you doubleclick on an
		// Advanved-Search result entry in the RepositoryBrowser
		// see
		// bndtools.views.repository.RepositoriesView.createPartControl(Composite)
		// and scroll down to "viewer.addDoubleClickListener(..."
		// but since repositoryBundleVersion already implements IAdaptable
		// we just forward to it
		return repositoryBundleVersion.getAdapter(adapter);
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
		return Objects.equals(resource, other.resource);
	}

}
