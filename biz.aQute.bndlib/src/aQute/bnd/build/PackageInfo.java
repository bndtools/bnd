package aQute.bnd.build;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;

/*
 * Hides the differences and handling of packageinfo and package-info.java
 * where we store versions and attributes.
 */
class PackageInfo {
	private static final String		PACKAGE_INFO_JAVA	= "package-info.java";
	private static final String		PACKAGEINFO			= "packageinfo";
	private final static Pattern	MODERN_P			= Pattern
		.compile("@\\s*[a-zA-Z0-9_$.]*\\s*Version\\((?:\\s*value\\s*=\\s*)?\"(" + Verifier.VERSION_S + ")\"\\)");
	private final static Pattern	CLASSIC_P			= Pattern
		.compile("\\s*version\\s*(?:\\s|:|=)\\s*(" + Verifier.VERSION_S + ")");
	private final static Pattern	MODERN_PACKAGE_P	= Pattern.compile("package[^;]*;");
	private final Project			project;

	PackageInfo(Project project) {
		this.project = project;
	}

	/**
	 * Get the version for a package name. This traverse the source paths and
	 * will stop at the first source directory that has a packageinfo or
	 *
	 * @param packageName
	 * @throws Exception
	 */
	public Version getPackageInfo(String packageName) throws Exception {
		File target = getFile(packageName);
		if (target != null && target.isFile()) {
			Version v = getVersion(target, getPattern(target));
			if (v == null && isModern(target)) {
				target = new File(target.getParentFile(), PACKAGEINFO);
				v = getVersion(target, getPattern(target));
			}
			if (v != null)
				return v;
		}

		return Version.emptyVersion;
	}

	/**
	 * Sets the package version on an exported package. If package-info.java
	 * exists then we use that one, otherwise we try the packageinfo file. If
	 * neither exists, we create a package-info.java file. You can set the
	 * annotation to use. Default is bnd. setting it to 'osgi' sets it to the
	 * OSGi annotations.
	 *
	 * @param packageName The package name
	 * @param version The new package version
	 * @throws Exception
	 */
	public boolean setPackageInfo(String packageName, Version version) throws Exception {

		File target = getFile(packageName);

		//
		// The directory MUST exist. we should not add packages for
		// packages that do not exist.
		//

		if (target == null || !target.getParentFile()
			.isDirectory())
			return false;

		//
		// If the file already exists then we use a modern/classic
		// style pattern to replace the version.
		//

		String content;
		if (target.isFile()) {
			if (replace(target, version, getPattern(target)))
				return true;

			//
			// if have a modern file without a version then we only add the
			// version
			// if we are instructed by the version annotation.
			//

			if (isModern(target)) {
				String versionAnnotation = getVersionAnnotation();
				if (versionAnnotation != null) {
					content = IO.collect(target);
					Matcher m = MODERN_PACKAGE_P.matcher(content);
					if (m.find()) {
						content = m
							.replaceFirst("@Version(\"" + version + "\")\n$0\nimport " + versionAnnotation + ";");
						IO.store(content, target);
						return true;
					}
					return false;
				}
			}

			//
			// If we failed, we always overwrite packageinfo
			//

			target = new File(target.getParentFile(), PACKAGEINFO);
		}

		content = getContent(isModern(target), packageName, version);
		IO.store(content, target);
		return true;
	}

	/**
	 * Check what version annotation to use for new content:
	 * <ul>
	 * <li>not set -> use packageinfo
	 * <li>osgi -> use the OSGi Version ann.
	 * <li>bnd -> use the bnd version ann.
	 * <li>other -> use the content as the version annotation, must have the
	 * same prototype as the bnd/osgi ann.
	 * </ul>
	 */
	private String getVersionAnnotation() {
		String versionAnnotation = project.getProperty(Constants.PACKAGEINFOTYPE);
		if (versionAnnotation == null)
			return null;

		if ("osgi".equals(versionAnnotation))
			return "org.osgi.annotation.versioning.Version";
		else if ("bnd".equals(versionAnnotation))
			return "aQute.bnd.annotation.Version";
		else if ("packageinfo".equals(versionAnnotation)) {
			return null;
		} else
			return versionAnnotation;
	}

	/*
	 * Calculate the new content for a package info file.
	 */
	private String getContent(boolean modern, String packageName, Version version) {
		try (Formatter f = new Formatter()) {
			if (modern) {
				f.format("@Version(\"%s\")\n", version);
				f.format("package %s;\n", packageName);
				f.format("import %s;\n", getVersionAnnotation());
			} else {
				f.format("version %s\n", version);
			}
			return f.toString();
		}
	}

	/*
	 * get the pattern to find the version.
	 */
	private Pattern getPattern(File target) {
		if (isModern(target))
			return MODERN_P;
		else
			return CLASSIC_P;
	}

	private boolean isModern(File target) {
		return target.getName()
			.endsWith(".java");
	}

	/*
	 * Replace a version in a file based on a pattern. We search the pattern and
	 * if found we replace group 1 with the new version. If the found version
	 * matches the new version we bail out early.
	 */
	private boolean replace(File target, final Version newVersion, Pattern pattern) throws IOException {
		String content = IO.collect(target);
		Matcher m = pattern.matcher(content);
		if (!m.find()) {
			return false;
		}

		Version oldVersion = new Version(m.group(1));

		if (newVersion.compareTo(oldVersion) == 0) {
			return true;
		}

		return replace(newVersion, content, m, target);
	}

	private boolean replace(Version newVersion, String content, Matcher m, File target) throws IOException {
		StringBuilder output = new StringBuilder();
		output.append(content, 0, m.start(1));
		output.append(newVersion);
		output.append(content, m.end(1), m.regionEnd());
		IO.store(output, target);
		return true;
	}

	/*
	 * Try to locate the file for the given package name and file type.
	 */
	private File getFile(String packageName) throws Exception {

		String relativePackagePath = packageName.replace('.', '/');
		File first = null;

		for (File srcDir : project.getSourcePath()) {

			if (!srcDir.isDirectory())
				continue;

			File packageDir = IO.getFile(srcDir, relativePackagePath);
			if (!packageDir.isDirectory())
				continue;

			if (first == null)
				first = packageDir;

			File target = new File(packageDir, PACKAGE_INFO_JAVA);
			if (target.isFile())
				return target;

			target = new File(packageDir, PACKAGEINFO);
			if (target.isFile())
				return target;
		}

		//
		// See if the package directory actually exists
		//

		if (first == null)
			return null;

		//
		// In the old times, the default was PACKAGEINFO
		// we now allow this to be controlled with the -versionannotation
		//

		String versionAnnotation = getVersionAnnotation();
		if (versionAnnotation == null)
			return new File(first, PACKAGEINFO);
		else
			return new File(first, PACKAGE_INFO_JAVA);
	}

	/*
	 * Get the version from the file using the pattern
	 */
	private Version getVersion(File source, Pattern pattern) throws IOException {
		if (!source.isFile())
			return null;

		String content = IO.collect(source);

		Matcher m = pattern.matcher(content);
		if (!m.find())
			return null;

		return new Version(m.group(1));
	}

}
