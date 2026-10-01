package org.bndtools.jdtls;

import java.io.File;
import java.util.Optional;

public class BndWorkspaceDetector {
	public Optional<File> findWorkspaceRoot(File start) {
		if (start == null) {
			return Optional.empty();
		}

		File current = start.isDirectory() ? start : start.getParentFile();
		while (current != null) {
			File cnf = new File(current, "cnf");
			if (new File(cnf, "build.bnd").isFile()) {
				return Optional.of(current);
			}
			current = current.getParentFile();
		}
		return Optional.empty();
	}

	public boolean isWorkspaceRoot(File folder) {
		return folder != null && new File(new File(folder, "cnf"), "build.bnd").isFile();
	}

	public boolean isBndProject(File folder) {
		return folder != null && new File(folder, "bnd.bnd").isFile();
	}

	public boolean isBndBuildFile(File file) {
		return file != null && file.isFile()
			&& ("bnd.bnd".equals(file.getName()) || "build.bnd".equals(file.getName()) || file.getName()
				.endsWith(".bnd"));
	}
}
