package org.bndtools.builder;

import java.io.File;
import java.util.Set;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CompilationParticipant;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Processor.FileLine;
import aQute.bnd.service.result.Result;
import aQute.service.reporter.Reporter.SetLocation;
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

			return result;
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

			Processor processor = new Processor();
			Set<File> result = Central.bndCall(() -> {
				Project project = Central.getProject(javaProject.getProject());
				if (project == null)
					return null;

				Result<Set<File>, String> r = project.getGenerate()
					.generate(true);
				processor.getInfo(project);

				if (r.isErr()) {
					SetLocation loc = project.error("%s", r.error()
						.get());
					loc.header(Constants.GENERATE);
					FileLine header = project.getHeader(Constants.GENERATE);
					if (header != null) {
						header.set(loc);
					}
				}
				processor.getInfo(project);
				return project.getGenerate()
					.getOutputDirs();
			});

			if (result != null) {
				for (File f : result) {
					IResource resource = Central.toResource(f);
					if (resource != null) {
						resource.refreshLocal(IResource.DEPTH_INFINITE, null);
					}
				}
			}
			markers.setMarkers(processor, MARKER_BND_GENERATE);

			return READY_FOR_BUILD;
		} catch (Exception e) {
			logger.logError("generating phase, aboutToBuild", e);
		}
		return READY_FOR_BUILD;
	}
}
