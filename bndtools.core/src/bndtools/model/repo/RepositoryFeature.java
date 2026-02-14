package bndtools.model.repo;

import java.util.Map;

import aQute.bnd.service.Actionable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.Strategy;
import aQute.bnd.version.Version;
import aQute.p2.provider.Feature;

/**
 * Represents an Eclipse P2 Feature in the repository view. This is a synthetic
 * entry that wraps a {@link Feature} and provides hierarchical display with
 * included features, required features, and included bundles as children.
 */
public class RepositoryFeature extends RepositoryEntry implements Actionable {

	private final Feature feature;

	public RepositoryFeature(final RepositoryPlugin repo, final Feature feature) {
		super(repo, feature.getId(), new VersionFinder(feature.getVersion(), Strategy.EXACT) {
			@Override
			Version findVersion() throws Exception {
				if (feature.getVersion() != null) {
					try {
						return Version.parseVersion(feature.getVersion());
					} catch (IllegalArgumentException e) {
						return null;
					}
				}
				return null;
			}
		});
		this.feature = feature;
	}

	public Feature getFeature() {
		return feature;
	}

	@Override
	public String toString() {
		return "RepositoryFeature [repo=" + getRepo() + ", id=" + feature.getId() + ", version="
			+ feature.getVersion() + "]";
	}

	@Override
	public String title(Object... target) throws Exception {
		try {
			if (getRepo() instanceof Actionable) {
				String s = ((Actionable) getRepo()).title(feature.getId());
				if (s != null)
					return s;
			}
		} catch (Exception e) {
			// just default
		}
		return feature.getId();
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		if (getRepo() instanceof Actionable) {
			try {
				String s = ((Actionable) getRepo()).tooltip(feature.getId());
				if (s != null)
					return s;
			} catch (Exception e) {
				// fall through to default
			}
		}
		// Build default tooltip from feature metadata
		StringBuilder sb = new StringBuilder();
		sb.append(feature.getId());
		if (feature.getVersion() != null) {
			sb.append(" ")
				.append(feature.getVersion());
		}
		if (feature.getProviderName() != null) {
			sb.append("\nProvider: ")
				.append(feature.getProviderName());
		}
		return sb.toString();
	}

	@Override
	public Map<String, Runnable> actions(Object... target) throws Exception {
		Map<String, Runnable> map = null;
		try {
			if (getRepo() instanceof Actionable) {
				map = ((Actionable) getRepo()).actions(feature.getId());
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
			return feature.getId();
		}
	}
}
