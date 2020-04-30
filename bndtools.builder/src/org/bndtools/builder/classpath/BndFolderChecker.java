package org.bndtools.builder.classpath;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CompilationParticipant;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.service.reporter.Reporter.SetLocation;
import bndtools.central.Central;

public class BndFolderChecker extends CompilationParticipant {
	private Project					project;

	@Override
	public int aboutToBuild(IJavaProject javaProject) {

		try {
			project = Central.getProject(javaProject.getProject());
			if (project != null) {
				project.getSourcePath()
					.forEach(f -> check(f, "src"));
				project.getTestpath()
					.stream()
					.map(Container::getFile)
					.forEach(f -> check(f, "test"));
			}
		} catch (Exception e) {
			return NEEDS_FULL_BUILD;
		}
		return READY_FOR_BUILD;
	}

	void check(File folder, String header) {
		if (folder == null) {
			error(folder, header, "null src/test folder");
			return;
		}

		if (folder.isDirectory())
			return;

		if (folder.isFile()) {
			error(folder, header, "expected a src/test folder %s, but it is a file, deleting and creating a folder",
				folder);
			try {
				IO.deleteWithException(folder);
			} catch (IOException e) {
				error(folder, header, "cannot delete file %s to create a folder, cause %s", folder,
					Exceptions.causes(e));
			}
			if (folder.isFile()) {
				error(folder, header, "could not delete src/test folder %s", folder);
				return;
			}
		}

		if (!folder.exists()) {
			try {
				IO.mkdirs(folder);
			} catch (IOException e) {
				error(folder, header, "cannot create folder %s, cause %s", folder, Exceptions.causes(e));
			}
			if (!folder.exists()) {
				error(folder, header, "cannot create folder %s", folder);
			}
		}
		try {
			Central.refreshFile(folder);
		} catch (CoreException e) {
			error(folder, header, "cannot refresh folder %s : %s", folder, Exceptions.causes(e));
		}
	}

	private void error(File folder, String header, String format, Object... args) {
		SetLocation error = project.error(format, args);
		error.header(header);
		error.reference(folder.getName());
	}

	@Override
	public boolean isActive(IJavaProject javaProject) {
		return true;
	}
}
