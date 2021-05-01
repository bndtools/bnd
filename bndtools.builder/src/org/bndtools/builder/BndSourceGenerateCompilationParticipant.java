package org.bndtools.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CompilationParticipant;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Processor.FileLine;
import aQute.bnd.result.Result;
import aQute.bnd.exceptions.RunnableWithException;
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
			markers.deleteMarkers(MARKER_BND_GENERATE);
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
			IProject project = javaProject.getProject();
			MarkerSupport markers = new MarkerSupport(project);
			Workspace workspace = Central.getWorkspaceIfPresent();
			if (workspace == null || workspace.isDefaultWorkspace()) {
				return READY_FOR_BUILD;
			}

			List<RunnableWithException> after = new ArrayList<>();
			Processor processor = new Processor();

			Central.bndCall(() -> {
				Project model = workspace.getProject(project.getName());
				if (model != null) {

					Result<Set<File>, String> result = model.getGenerate()
						.generate(true);

					processor.getInfo(model, "generate: ");

					if (result.isErr()) {
						SetLocation loc = processor.error(result.error()
							.get());
						FileLine header = model.getHeader(Constants.GENERATE);
						if (header != null) {
							header.set(loc);
						}
					}
					after.add(() -> {
						markers.setMarkers(processor, MARKER_BND_GENERATE);
					});

					Set<File> outputs = model.getGenerate()
						.getOutputDirs();

					after.add(() -> {
						for (File f : outputs) {
							IResource r = Central.toResource(f);
							if (r != null) {
								r.refreshLocal(IResource.DEPTH_INFINITE, null);
							}
						}
					});

				}
				return null;
			});
			for (RunnableWithException r : after) {
				r.run();
			}
		} catch (Exception e) {
			logger.logError("generating phase, aboutToBuild", e);
		}
		return READY_FOR_BUILD;
	}
}
