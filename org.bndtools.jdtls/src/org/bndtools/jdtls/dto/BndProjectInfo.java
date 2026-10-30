package org.bndtools.jdtls.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BndProjectInfo {
	private final String		name;
	private final String		basePath;
	private final String		bndFile;
	private final String		outputPath;
	private final String		testOutputPath;
	private final List<String>	sourcePaths;
	private final List<String>	testSourcePaths;
	private final List<String>	buildPath;
	private final List<String>	testPath;

	public BndProjectInfo(String name, String basePath, String bndFile, String outputPath, String testOutputPath,
		List<String> sourcePaths, List<String> testSourcePaths, List<String> buildPath, List<String> testPath) {
		this.name = name;
		this.basePath = basePath;
		this.bndFile = bndFile;
		this.outputPath = outputPath;
		this.testOutputPath = testOutputPath;
		this.sourcePaths = copy(sourcePaths);
		this.testSourcePaths = copy(testSourcePaths);
		this.buildPath = copy(buildPath);
		this.testPath = copy(testPath);
	}

	public String getName() {
		return name;
	}

	public String getBasePath() {
		return basePath;
	}

	public String getBndFile() {
		return bndFile;
	}

	public String getOutputPath() {
		return outputPath;
	}

	public String getTestOutputPath() {
		return testOutputPath;
	}

	public List<String> getSourcePaths() {
		return sourcePaths;
	}

	public List<String> getTestSourcePaths() {
		return testSourcePaths;
	}

	public List<String> getBuildPath() {
		return buildPath;
	}

	public List<String> getTestPath() {
		return testPath;
	}

	private static List<String> copy(List<String> values) {
		if (values == null || values.isEmpty()) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(new ArrayList<>(values));
	}
}
