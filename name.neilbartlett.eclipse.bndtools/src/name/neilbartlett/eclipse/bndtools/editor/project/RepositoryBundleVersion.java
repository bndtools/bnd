package name.neilbartlett.eclipse.bndtools.editor.project;

import aQute.libg.version.Version;

class RepositoryBundleVersion {
	
	private final Version version;
	private final RepositoryBundle bundle;

	public RepositoryBundleVersion(RepositoryBundle bundle, Version version) {
		this.bundle = bundle;
		this.version = version;
	}
	public Version getVersion() {
		return version;
	}
	public RepositoryBundle getBundle() {
		return bundle;
	}
	@Override
	public String toString() {
		return "RepositoryBundleVersion [version=" + version + ", bundle="
				+ bundle + "]";
	}
}
