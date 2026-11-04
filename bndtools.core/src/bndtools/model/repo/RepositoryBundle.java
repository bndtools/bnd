package bndtools.model.repo;

import java.util.Map;
import java.util.SortedSet;

import org.osgi.framework.namespace.IdentityNamespace;

import aQute.bnd.service.Actionable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.Strategy;
import aQute.bnd.version.Version;

/**
 * Abstracts the Bundle in repository views, it wraps the underlying Repository
 * Plugin with the bsn of the bundle. It supports {@code Actionable} by
 * implementing its methods but forwarding them to the Repository Plugin.
 */
public class RepositoryBundle extends RepositoryEntry implements Actionable {

	public RepositoryBundle(final RepositoryPlugin repo, final String bsn) {
		super(repo, bsn, new VersionFinder("latest", Strategy.HIGHEST) {
			@Override
			Version findVersion() throws Exception {
				SortedSet<Version> vs = repo.versions(bsn);
				if (vs == null || vs.isEmpty())
					return null;
				return vs.last();
			}
		});
	}

	@Override
	public String toString() {
		return "RepositoryBundle [repo=" + getRepo() + ", bsn=" + getBsn() + "]";
	}

	@Override
	public String title(Object... target) throws Exception {
		try {
			if (getRepo() instanceof Actionable) {
				String s = ((Actionable) getRepo()).title(getBsn());
				if (s != null)
					return s;
			}
		} catch (Exception e) {
			// just default
		}
		return getBsn();
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		if (getRepo() instanceof Actionable) {
			String s = ((Actionable) getRepo()).tooltip(getBsn());
			if (s != null)
				return s;
		}
		return null;
	}

	@Override
	public Map<String, Runnable> actions(Object... target) throws Exception {
		Map<String, Runnable> map = null;
		try {
			if (getRepo() instanceof Actionable) {
				map = ((Actionable) getRepo()).actions(getBsn());
			}
		} catch (Exception e) {
			// just default
		}
		return map;
	}

	public String getText() {
		try {
			return title();
		} catch (Exception e) {
			return getBsn();
		}
	}

	@Override
	protected Map<String, String> getProperties() {
		return Map.of(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_BUNDLE);
	}

	/**
	 * Override to filter out features when looking up bundles. When a feature
	 * and bundle share the same ID, we want the bundle, not the feature.
	 */
	@Override
	protected boolean acceptResourceType(org.osgi.resource.Resource resource) {
		// Reject resources that are features
		return !isFeatureResource(resource);
	}

	/**
	 * Check if a resource represents an Eclipse feature rather than a bundle.
	 */
	private boolean isFeatureResource(org.osgi.resource.Resource resource) {
		return resource.getCapabilities(org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE)
			.stream()
			.anyMatch(cap -> "org.eclipse.update.feature"
				.equals(cap.getAttributes()
					.get(org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE)));
	}
}
