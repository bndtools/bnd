package org.bndtools.jdtls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.bndtools.jdtls.dto.BndPackageInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BndJdtLsBridgeTest {
	@TempDir
	File tmp;

	@Test
	public void listsPackagesFromSourceLayoutWithoutParsingJava() throws Exception {
		File cnf = new File(tmp, "cnf");
		assertThat(cnf.mkdirs()).isTrue();
		Files.writeString(new File(cnf, "build.bnd").toPath(), "-buildpath:\n", StandardCharsets.UTF_8);

		File project = new File(tmp, "demo");
		assertThat(new File(project, "src/com/acme/api").mkdirs()).isTrue();
		Files.writeString(new File(project, "bnd.bnd").toPath(), "Bundle-SymbolicName: demo\n", StandardCharsets.UTF_8);
		Files.writeString(new File(project, "src/com/acme/api/Foo.java").toPath(),
			"package com.acme.api; public class Foo {}\n", StandardCharsets.UTF_8);

		BndJdtLsBridge bridge = new BndJdtLsBridge();
		List<BndPackageInfo> packages = bridge.getPackages(new File(project, "bnd.bnd"));

		assertThat(packages).extracting(BndPackageInfo::getPackageName)
			.contains("com.acme.api");
	}
}
