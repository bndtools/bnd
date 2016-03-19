package bndtools.wizards.project;

public enum ProjectTemplateParam {

    PROJECT_NAME("projectName"), VERSION("version"), SRC_DIR("srcDir"), BIN_DIR("binDir"), TEST_SRC_DIR("testSrcDir"), TEST_BIN_DIR("testBinDir"), BASE_PACKAGE_NAME("basePackageName"), BASE_PACKAGE_DIR("basePackageDir"), TARGET_DIR(
            "targetDir"), VERSION_OUTPUTMASK("outputmask"), JAVA_LEVEL("javaLevel");

    private final String string;

    private ProjectTemplateParam(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

}
