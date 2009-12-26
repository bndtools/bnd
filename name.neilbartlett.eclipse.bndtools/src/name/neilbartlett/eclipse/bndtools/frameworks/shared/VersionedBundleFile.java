package name.neilbartlett.eclipse.bndtools.frameworks.shared;

import java.io.File;

import org.osgi.framework.Version;

public class VersionedBundleFile {
	private static final String SUFFIX_JAR = ".jar"; //$NON-NLS-1$
	private final File file;
	private final Version version;
	
	/**
	 * @param id The expected ID of the bundle.
	 * @param jarFile The bundle JAR file.
	 * @param separator The separator between the id and the version; usually either hyphen or underscore.
	 * @return A new versioned file, or {@code null} if the file does not exist, is not a plain file, or if the file name does not start with the specified ID.
	 * @throws IllegalArgumentException If the version string is not parseable.
	 */
	public static VersionedBundleFile fromBundleJar(String id, File jarFile, char separator) {
		if(!jarFile.isFile())
			return null;
		
		String fileName = jarFile.getName();
		String prefix = id + separator;
		if(!fileName.startsWith(prefix) || !fileName.endsWith(SUFFIX_JAR))
			return null;
		
		String versionStr = fileName.substring(prefix.length(), fileName.length() - SUFFIX_JAR.length());
		return new VersionedBundleFile(jarFile, new Version(versionStr));
	}
	
	public static VersionedBundleFile findHighestVersionBundle(File dir, String id, char separator) {
		VersionedBundleFile selectedJarFile = null;
		
		File[] files = dir.listFiles();
		for (File file : files) {
			try {
				VersionedBundleFile versionedFile = fromBundleJar(id, file, separator);
				if(versionedFile != null) {
					if(selectedJarFile == null || selectedJarFile.getVersion().compareTo(versionedFile.getVersion()) < 0) {
						selectedJarFile = versionedFile;
					}
				}
			} catch (IllegalArgumentException e) {
				// Ignore JARs with invalid version strings.
			}
		}
		return selectedJarFile;
	}
	

	
	private VersionedBundleFile(File file, Version version) {
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
