package name.neilbartlett.eclipse.bndtools.classpath;

import org.eclipse.core.runtime.IPath;

import aQute.libg.version.Version;

public class ExportedBundle {
	
	private final IPath path;
	private final String symbolicName;
	private final Version version;
	private final IPath sourceBndFilePath;

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
		return "ExportedBundle [symbolicName=" + symbolicName + ", version="
				+ version + ", path=" + path + "]";
	}
}
