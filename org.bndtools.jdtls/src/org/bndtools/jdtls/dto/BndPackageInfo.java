package org.bndtools.jdtls.dto;

public class BndPackageInfo {
	private final String	packageName;
	private final String	sourcePath;
	private final boolean	testPackage;

	public BndPackageInfo(String packageName, String sourcePath, boolean testPackage) {
		this.packageName = packageName;
		this.sourcePath = sourcePath;
		this.testPackage = testPackage;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public boolean isTestPackage() {
		return testPackage;
	}
}
