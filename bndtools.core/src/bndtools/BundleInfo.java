package bndtools;

import java.io.File;
import java.util.List;
import java.util.Map;

import aQute.bnd.osgi.Clazz;

class BundleInfo {
	final String					bsn;
	final String					version;
	final File						file;
	final Map<String, List<Clazz>>	clazzes;

	public BundleInfo(String bsn, String version, File file, Map<String, List<Clazz>> clazzes) {
		this.bsn = bsn;
		this.version = version;
		this.file = file;
		this.clazzes = clazzes;
	}

	public String getBsn() {
		return bsn;
	}

	public String getVersion() {
		return version;
	}

	public File getFile() {
		return file;
	}
}
