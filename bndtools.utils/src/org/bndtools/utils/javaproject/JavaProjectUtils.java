package org.bndtools.utils.javaproject;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

public class JavaProjectUtils {
	/**
	 * Access the project's (Eclipse) classpath to determine the source
	 * directories and their output directories
	 * 
	 * @param project
	 *            the project
	 * @return a map of source directories and their output directories, all
	 *         relative to the project directory. null when project is null or
	 *         when an error occurred.
	 */
	static public Map<String, String> getSourceOutputLocations(IJavaProject project) {
		if (project == null) {
			return null;
		}

		IClasspathEntry[] classPathEntries = null;
		IPath defaultOutputLocation = null;
		try {
			classPathEntries = project.getRawClasspath();
			defaultOutputLocation = project.getOutputLocation();
		} catch (Throwable e) {
			return null;
		}

		if (classPathEntries == null || defaultOutputLocation == null) {
			return null;
		}

		IPath projectPath = project.getPath();

		Map<String, String> sourceOutputLocations = new LinkedHashMap<String, String>();
		for (IClasspathEntry classPathEntry : classPathEntries) {
			if (classPathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPath src = classPathEntry.getPath();
				IPath bin = classPathEntry.getOutputLocation();

				if (bin == null) {
					bin = defaultOutputLocation;
				}

				assert (src != null);
				assert (bin != null);

				sourceOutputLocations.put(src.makeRelativeTo(projectPath).toString(), bin.makeRelativeTo(projectPath).toString());
			}
		}

		return sourceOutputLocations;
	}
}