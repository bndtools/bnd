package bndtools.editor.contents;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Version;

import aQute.lib.io.IO;

/**
 * Models the three available styles for versioning exported packages.
 */
public enum PackageInfoStyle {

	/**
	 * The legacy, flat file format supported by bnd. This is still used for
	 * Java 4 (and earlier) projects).
	 */
	Legacy("packageinfo", "version %s%n", null),

	/**
	 * Annotated package-info.java, using the bnd annotations.
	 */
	BndAnnotation("package-info.java", "@aQute.bnd.annotation.Version(\"%s\")%npackage %s;%n",
		"aQute.bnd.annotation.Version"),

	/**
	 * Annotated package-info.java, using the OSGi spec annotations from R6.
	 */
	SpecAnnotation("package-info.java", "@org.osgi.annotation.versioning.Version(\"%s\")%npackage %s;%n",
		"org.osgi.annotation.versioning.Version");

	private static final ILogger		logger	= Logger.getLogger(PackageInfoStyle.class);

	private static final Set<String>	PRE_JAVA5_VERSIONS;

	static {
		PRE_JAVA5_VERSIONS = new HashSet<>();
		PRE_JAVA5_VERSIONS.add(JavaCore.VERSION_CLDC_1_1);
		PRE_JAVA5_VERSIONS.add(JavaCore.VERSION_1_1);
		PRE_JAVA5_VERSIONS.add(JavaCore.VERSION_1_2);
		PRE_JAVA5_VERSIONS.add(JavaCore.VERSION_1_3);
		PRE_JAVA5_VERSIONS.add(JavaCore.VERSION_1_4);
	}

	private final String	fileName;
	private final String	contentPattern;
	private final String	annotationTypeName;
	private final Pattern	searchRegex;

	PackageInfoStyle(String fileName, String contentPattern, String annotationTypeName) {
		this.fileName = fileName;
		this.contentPattern = contentPattern;
		this.annotationTypeName = annotationTypeName;
		this.searchRegex = annotationTypeName != null ? Pattern.compile(annotationTypeName, Pattern.LITERAL) : null;
	}

	public String getFileName() {
		return fileName;
	}

	public String format(Version version, String packageName) {
		return String.format(contentPattern, version, packageName);
	}

	public static PackageInfoStyle findExisting(File dir) throws IOException {
		File packageInfoJava = new File(dir, SpecAnnotation.fileName);
		if (packageInfoJava.exists()) {
			String content = IO.collect(packageInfoJava);
			if (SpecAnnotation.searchRegex.matcher(content)
				.find())
				return SpecAnnotation;
			if (BndAnnotation.searchRegex.matcher(content)
				.find())
				return BndAnnotation;
			return null;
		}

		File legacyPackageInfo = new File(dir, Legacy.fileName);
		if (legacyPackageInfo.exists())
			return Legacy;

		return null;
	}

	public static PackageInfoStyle calculatePackageInfoStyle(IProject project) {
		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject == null)
			return Legacy;

		// JDK Compliance and Java Source level must both be at least 1.5 to
		// support
		// annotated package-info.java
		String compliance = javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);
		if (compliance == null || PRE_JAVA5_VERSIONS.contains(compliance))
			return Legacy;
		String sourceLevel = javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
		if (sourceLevel == null || PRE_JAVA5_VERSIONS.contains(sourceLevel))
			return Legacy;

		try {
			IType specAnnotation = javaProject.findType(SpecAnnotation.annotationTypeName);
			if (specAnnotation != null)
				return SpecAnnotation;
			IType bndAnnotation = javaProject.findType(BndAnnotation.annotationTypeName);
			if (bndAnnotation != null)
				return BndAnnotation;
		} catch (JavaModelException e) {
			logger.logError("Could not determine classpath visibility for versioning annotations.", e);
		}
		return Legacy;
	}

}
