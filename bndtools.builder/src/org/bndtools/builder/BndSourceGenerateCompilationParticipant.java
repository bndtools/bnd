package org.bndtools.builder;

import java.io.File;
import java.util.Set;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CompilationParticipant;

import aQute.bnd.build.Project;
import aQute.bnd.service.result.Result;
import bndtools.central.Central;

public class BndSourceGenerateCompilationParticipant extends CompilationParticipant {
	private static final ILogger	logger				= Logger
		.getLogger(BndSourceGenerateCompilationParticipant.class);
	public static final String		MARKER_BND_GENERATE	= "bndtools.builder.generate";
	private Project					project;

	@Override
	public boolean isActive(IJavaProject javaProject) {
		try {
			project = Central.getProject(javaProject.getProject());
			if (project == null)
				return false;

			Set<File> inputs = project.getGenerate()
				.getInputs();
			return !inputs.isEmpty();
		} catch (Exception e) {
			logger.logError("generating phase, isActive", e);
			return false;
		}
	}

	@Override
	public int aboutToBuild(IJavaProject javaProject) {
		try {
			MarkerSupport markers = new MarkerSupport(javaProject.getProject());
			markers.deleteMarkers(MARKER_BND_GENERATE);
			project.clear();

			Result<Set<File>, String> result = project.getGenerate()
				.generate(true);
			if (result.isErr()) {
				markers.createMarker(project, IMarker.SEVERITY_ERROR, result.error()
					.get(), MARKER_BND_GENERATE);
			} else {
				Set<File> outputs = result.unwrap();
				Central.refreshFiles(project, outputs, null, true);
			}
			markers.setMarkers(project, MARKER_BND_GENERATE);
			return READY_FOR_BUILD;
		} catch (Exception e) {
			logger.logError("generating phase, aboutToBuild", e);
		}
		return READY_FOR_BUILD;
	}
}
