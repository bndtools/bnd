package org.bndtools.api;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;

public class ProjectPaths {

    /*
     * Static part
     */

    public static final ProjectPaths DEFAULT;
    static {
        /* This call MUST NOT access any remotes */
        Processor defaults = Workspace.getDefaults();
        DEFAULT = new ProjectPaths( //
            "bnd", //
            defaults.getProperty(Constants.DEFAULT_PROP_SRC_DIR), //
            defaults.getProperty(Constants.DEFAULT_PROP_BIN_DIR), //
            defaults.getProperty(Constants.DEFAULT_PROP_TESTSRC_DIR), //
            defaults.getProperty(Constants.DEFAULT_PROP_TESTBIN_DIR), //
            defaults.getProperty(Constants.DEFAULT_PROP_TARGET_DIR));
    }

    /*
     * Instance part
     */

    private final String title;
    private final String src;
    private final String bin;
    private final String testSrc;
    private final String testBin;
    private final String targetDir;
    private final String toolTip;

    private ProjectPaths(String title, String src, String bin, String testSrc, String testBin, String targetDir) {
        this.title = title;
        this.src = src;
        this.bin = bin;
        this.testSrc = testSrc;
        this.testBin = testBin;
        this.targetDir = targetDir;

        try {
            validate();
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Could not construct Project Paths: " + e.getMessage());
        }

        this.toolTip = constructToolTip();
    }

    private String constructToolTip() {
        return String.format("Main sources directory: %s (%s)%n" + "Test sources directory: %s (%s)%n" + "Target directory: %s", src, bin, testSrc, testBin, targetDir);
    }

    private void validate() throws Exception {
        if (title == null || title.length() == 0)
            throw new Exception("Invalid title");
        if (src == null || src.length() == 0)
            throw new Exception("Invalid source dir");
        if (bin == null || bin.length() == 0)
            throw new Exception("Invalid bin dir");
        if (testSrc == null || testSrc.length() == 0)
            throw new Exception("Invalid test source dir");
        if (testBin == null || testBin.length() == 0)
            throw new Exception("Invalid test bin dir");
        if (targetDir == null || targetDir.length() == 0)
            throw new Exception("Invalid target dir");
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