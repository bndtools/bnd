package org.bndtools.builder;

import org.eclipse.core.runtime.IPath;

import aQute.bnd.version.Version;

/**
 * <p>
 * A bundle exported by a project.
 * </p>
 * <p>
 * This class implements the {@link Comparable} interface. Instances are
 * compared first on the lexical ordering of their symbolic names, and if these
 * are equal then on their version.
 * </p>
 *
 * @author Neil Bartlett
 */
class ExportedBundle implements Comparable<ExportedBundle> {

	private final IPath		path;
	private final String	symbolicName;
	private final Version	version;
	private final IPath		sourceBndFilePath;

	public ExportedBundle(IPath bundlePath, IPath sourceBndFilePath, String symbolicName, Version version) {
		this.path = bundlePath;
		this.sourceBndFilePath = sourceBndFilePath;
		this.symbolicName = symbolicName;
		this.version = version;
	}

	public IPath getPath() {
		return path;
	}

	public IPath getSourceBndFilePath() {
		return sourceBndFilePath;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public Version getVersion() {
		return version;
	}

	@Override
	public String toString() {
		return "ExportedBundle [symbolicName=" + symbolicName + ", version=" + version + ", path=" + path + "]";
	}

	@Override
	public int compareTo(ExportedBundle other) {
		int diff = this.getSymbolicName()
			.compareTo(other.getSymbolicName());
		if (diff == 0) {
			Version version1 = this.getVersion();
			if (version1 == null)
				version1 = new Version(0);
			Version version2 = other.getVersion();
			if (version2 == null)
				version2 = new Version(0);

			diff = version1.compareTo(version2);
		}
		return diff;
	}
}
