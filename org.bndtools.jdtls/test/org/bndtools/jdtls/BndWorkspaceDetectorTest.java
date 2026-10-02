package org.bndtools.jdtls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BndWorkspaceDetectorTest {
	@TempDir
	File tmp;

	@Test
	public void detectsWorkspaceRootAndProject() throws Exception {
		File cnf = new File(tmp, "cnf");
		assertThat(cnf.mkdirs()).isTrue();
		assertThat(new File(cnf, "build.bnd").createNewFile()).isTrue();

		File project = new File(tmp, "demo");
		assertThat(project.mkdirs()).isTrue();
		File bndFile = new File(project, "bnd.bnd");
		assertThat(bndFile.createNewFile()).isTrue();

		BndWorkspaceDetector detector = new BndWorkspaceDetector();
		assertThat(detector.isWorkspaceRoot(tmp)).isTrue();
		assertThat(detector.isBndProject(project)).isTrue();
		assertThat(detector.isBndBuildFile(bndFile)).isTrue();
		assertThat(detector.findWorkspaceRoot(bndFile)).contains(tmp);
	}
}
