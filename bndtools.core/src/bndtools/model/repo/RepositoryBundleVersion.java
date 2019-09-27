package bndtools.model.repo;

import java.util.Map;

import aQute.bnd.service.Actionable;
import aQute.bnd.service.Strategy;
import aQute.bnd.version.Version;

public class RepositoryBundleVersion extends RepositoryEntry implements Actionable {

	private final Version			version;
	private final RepositoryBundle	bundle;

	public RepositoryBundleVersion(RepositoryBundle bundle, final Version version) {
		super(bundle.getRepo(), bundle.getBsn(), new VersionFinder(version.toString(), Strategy.EXACT) {
			@Override
			Version findVersion() throws Exception {
				return version;
			}
		});
		this.bundle = bundle;
		this.version = version;
	}

	public Version getVersion() {
		return version;
	}

	public RepositoryBundle getParentBundle() {
		return bundle;
	}

	@Override
	public String toString() {
		return "RepositoryBundleVersion [version=" + version + ", bundle=" + bundle + "]";
	}

	@Override
	public String title(Object... target) throws Exception {
		try {
			if (bundle.getRepo() instanceof Actionable) {
				String s = ((Actionable) bundle.getRepo()).title(bundle.getBsn(), version);
				if (s != null)
					return s;
			}
		} catch (Exception e) {
			e.printStackTrace();
			// just default
		}
		return getVersion().toString();
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		try {
			if (bundle.getRepo() instanceof Actionable) {
				String s = ((Actionable) bundle.getRepo()).tooltip(bundle.getBsn(), version);
				if (s != null)
					return s;
			}
		} catch (Exception e) {
			// just default
		}
		return null;
	}

	@Override
	public Map<String, Runnable> actions(Object... target) throws Exception {
		Map<String, Runnable> map = null;
		try {
			if (bundle.getRepo() instanceof Actionable) {
				map = ((Actionable) bundle.getRepo()).actions(bundle.getBsn(), version);
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
			return version.toString();
		}
	}

}
