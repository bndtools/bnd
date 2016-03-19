package org.bndtools.utils.javaproject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

public class JavaProjectUtils {

    /**
     * Access the project's (Eclipse) classpath to determine the source directories and their output directories
     *
     * @param project
     *            the project
     * @return a map of source directories and their output directories, all relative to the project directory. empty
     *         map when project is null or when an error occurred.
     */
    static public Map<String,String> getSourceOutputLocations(IJavaProject project) {
        if (project == null) {
            return Collections.emptyMap();
        }

        IClasspathEntry[] classPathEntries = null;
        IPath defaultOutputLocation = null;
        try {
            classPathEntries = project.getRawClasspath();
            defaultOutputLocation = project.getOutputLocation();
        } catch (Throwable e) {
            return Collections.emptyMap();
        }

        if (classPathEntries == null || defaultOutputLocation == null) {
            return Collections.emptyMap();
        }

        IPath projectPath = project.getPath();

        Map<String,String> sourceOutputLocations = new LinkedHashMap<String,String>();
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

    public static final String PATH_JRE_CONTAINER = "org.eclipse.jdt.launching.JRE_CONTAINER";
    public static final String PATH_JRE_STANDARD_VM = "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType";

    private static enum JavaLevel {
        J2SE_1_2("J2SE-1.2", "1.2"), J2SE_1_3("J2SE-1.3", "1.3"), J2SE_1_4("J2SE-1.4", "1.4"), J2SE_1_5("J2SE-1.5", "1.5"), JAVASE_1_6("JavaSE-1.6", "1.6"), JAVASE_1_7("JavaSE-1.7", "1.7"), JAVASE_1_8("JavaSE-1.8",
                "1.8"), JAVASE_1_9("JavaSE-1.9", "1.9");

        private final String label;
        private final String level;

        JavaLevel(String label, String level) {
            this.label = label;
            this.level = level;
        }

        String getLevel() {
            return level;
        }

        static JavaLevel fromLabel(String label) {
            for (JavaLevel level : JavaLevel.values()) {
                if (level.label.equals(label))
                    return level;
            }
            throw new IllegalArgumentException("Unrecognized Java compliance level: " + label);
        }
    }

    public static String getJavaLevel(IJavaProject project) throws Exception {
        IClasspathEntry[] classpathEntries = project.getRawClasspath();

        for (IClasspathEntry classpathEntry : classpathEntries) {
            if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                IPath path = classpathEntry.getPath();
                if (path == null || path.segmentCount() < 3)
                    continue;
                if (PATH_JRE_CONTAINER.equals(path.segment(0)) && PATH_JRE_STANDARD_VM.equals(path.segment(1))) {
                    String jreName = path.segment(2);
                    return JavaLevel.fromLabel(jreName).getLevel();
                }
            }
        }
        throw new IllegalArgumentException("No standard JRE container found on project classpath");
    }
}
