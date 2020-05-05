package org.bndtools.builder;

import java.io.File;
import java.util.Set;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CompilationParticipant;

import aQute.bnd.build.Project;
import aQute.bnd.service.result.Result;
import bndtools.central.Central;

public class BndSourceGenerateCompilationParticipant extends CompilationParticipant {
	private static final ILogger	logger				= Logger
		.getLogger(BndSourceGenerateCompilationParticipant.class);
	public static final String		MARKER_BND_GENERATE	= "bndtools.builder.bndgenerate";

	@Override
	public boolean isActive(IJavaProject javaProject) {
		try {
			MarkerSupport markers = new MarkerSupport(javaProject.getProject());
			Project project = Central.getProject(javaProject.getProject());
			if (project == null)
				return false;

			boolean result = project.getGenerate()
				.needsBuild();

			System.out.println("isActive " + project + " " + result);
			return result;
		} catch (Exception e) {
			logger.logError("generating phase, isActive", e);
			return false;
		}
	}

	@Override
	public int aboutToBuild(IJavaProject javaProject) {
		try {
			Project project = Central.getProject(javaProject.getProject());
			if (project == null)
				return READY_FOR_BUILD;

			System.out.println("aboutToBuild " + project);
			MarkerSupport markers = new MarkerSupport(javaProject.getProject());

			Result<Set<File>, String> result = project.getGenerate()
				.generate(true);
			if (result.isOk()) {
				Set<File> generated = result.unwrap();
				Set<File> outputs = project.getGenerate()
					.getOutputDirs();
				Central.refreshFiles(project, outputs, null, true);
			}
			markers.setMarkers(project, MARKER_BND_GENERATE);
			project.clear();
			return READY_FOR_BUILD;
		} catch (Exception e) {
			logger.logError("generating phase, aboutToBuild", e);
		}
		return READY_FOR_BUILD;
	}
}
