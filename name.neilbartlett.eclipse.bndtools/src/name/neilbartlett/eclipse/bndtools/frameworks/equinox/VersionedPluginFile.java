package name.neilbartlett.eclipse.bndtools.frameworks.equinox;

import java.io.File;

import org.osgi.framework.Version;

class VersionedPluginFile {
	private static final String SUFFIX_JAR = ".jar"; //$NON-NLS-1$
	private final File file;
	private final Version version;
	
	/**
	 * @param pluginId The expected ID of the plugin.
	 * @param pluginJar The plugin JAR file.
	 * @return A new versioned file, or {@code null} if the file does not exist, is not a plain file, or if the file name does not start with the specified plugin ID.
	 * @throws IllegalArgumentException If the version string is not parseable.
	 */
	static VersionedPluginFile fromPluginJar(String pluginId, File pluginJar) {
		if(!pluginJar.isFile())
			return null;
		
		String fileName = pluginJar.getName();
		String prefix = pluginId + "_";
		if(!fileName.startsWith(prefix))
			return null;
		
		String versionStr = fileName.substring(prefix.length(), fileName.length() - SUFFIX_JAR.length());
		return new VersionedPluginFile(pluginJar, new Version(versionStr));
	}
	
	private VersionedPluginFile(File file, Version version) {
		this.file = file;
		this.version = version;
	}

	public File getFile() {
		return file;
	}

	public Version getVersion() {
		return version;
	}
}
