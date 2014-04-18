package aQute.lib.deployer;

import java.io.*;

import aQute.bnd.version.*;

public class VersionFilePair {
	private Version	version;
	private File	file;

	VersionFilePair(Version version, File file) {
		super();
		this.version = version;
		this.file = file;
	}

	public Version getVersion() {
		return version;
	}

	public void setVersion(Version version) {
		this.version = version;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("VersionFilePair [version=");
		builder.append(version);
		builder.append(", file=");
		builder.append(file);
		builder.append("]");
		return builder.toString();
	}
}
