package org.bndtools.api;

import java.util.HashMap;
import java.util.Map;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;

public class ProjectPaths {

    /*
     * Static part
     */

    private static final Map<ProjectLayout,ProjectPaths> map = new HashMap<ProjectLayout,ProjectPaths>();

    static {
        /*
         * BND
         */

        /* This call MUST NOT access any remotes */
        Processor defaults = Workspace.getDefaults();

        ProjectPaths projectPaths = new ProjectPaths( //
                ProjectLayout.BND, //
                "bnd", //
                defaults.getProperty(Constants.DEFAULT_PROP_SRC_DIR), //
                defaults.getProperty(Constants.DEFAULT_PROP_BIN_DIR), //
                defaults.getProperty(Constants.DEFAULT_PROP_TESTSRC_DIR), //
                defaults.getProperty(Constants.DEFAULT_PROP_TESTBIN_DIR), //
                defaults.getProperty(Constants.DEFAULT_PROP_TARGET_DIR));
        map.put(projectPaths.getLayout(), projectPaths);

        /*
         * MAVEN
         */

        projectPaths = new ProjectPaths( //
                ProjectLayout.MAVEN, //
                "maven", //
                "src/main/java", //
                "target/classes", //
                "src/main/test", //
                "target/test-classes", //
                "target");
        map.put(projectPaths.getLayout(), projectPaths);
    }

    public static ProjectPaths get(ProjectLayout projectLayout) {
        return map.get(projectLayout);
    }

    /*
     * Instance part
     */

    private final ProjectLayout layout;
    private final String title;
    private final String src;
    private final String bin;
    private final String testSrc;
    private final String testBin;
    private final String targetDir;
    private final String toolTip;

    private ProjectPaths(ProjectLayout layout, String title, String src, String bin, String testSrc, String testBin, String targetDir) {
        this.layout = layout;
        this.title = title;
        this.src = src;
        this.bin = bin;
        this.testSrc = testSrc;
        this.testBin = testBin;
        this.targetDir = targetDir;

        if (!validate()) {
            throw new ExceptionInInitializerError("Could not construct " + layout + " ProjectPaths");
        }

        this.toolTip = constructToolTip();
    }

    private String constructToolTip() {
        return String.format("Main sources directory: %s (%s)%n" + "Test sources directory: %s (%s)%n" + "Target directory: %s", src, bin, testSrc, testBin, targetDir);
    }

    private boolean validate() {
        boolean titleValid = (title != null && (title.length() != 0));
        boolean srcValid = (src != null && (src.length() != 0));
        boolean binValid = (bin != null && (bin.length() != 0));
        boolean testSrcValid = (testSrc != null && (testSrc.length() != 0));
        boolean testBinValid = (testBin != null && (testBin.length() != 0));
        boolean targetDirValid = (targetDir != null && (targetDir.length() != 0));

        return titleValid && srcValid && binValid && testSrcValid && testBinValid && targetDirValid;
    }

    public ProjectLayout getLayout() {
        return layout;
    }

    public String getTitle() {
        return title;
    }

    public String getSrc() {
        return src;
    }

    public String getBin() {
        return bin;
    }

    public String getTestSrc() {
        return testSrc;
    }

    public String getTestBin() {
        return testBin;
    }

    public String getTargetDir() {
        return targetDir;
    }

    public String getToolTip() {
        return toolTip;
    }
}