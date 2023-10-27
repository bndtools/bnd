package org.bndtools.builder.impl;

import java.io.File;
import java.util.Set;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Processor.FileLine;
import aQute.bnd.result.Result;
import aQute.service.reporter.Reporter.SetLocation;
import bndtools.central.Central;

@Component(service = CompilationParticipant.class)
public class BndSourceGenerateCompilationParticipant extends CompilationParticipant {
	private static final ILogger	logger				= Logger
		.getLogger(BndSourceGenerateCompilationParticipant.class);
	public static final String		MARKER_BND_GENERATE	= "bndtools.builder.bndgenerate";

	@Activate
	public BndSourceGenerateCompilationParticipant(@Reference
	IWorkspace notused) {}

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

			Processor processor = new Processor();

			Central.bndCall(workspace::readLocked, after -> {
				Project model = workspace.getProject(project.getName());
				if (model != null) {

					Result<Set<File>> result = model.getGenerate()
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
					after.accept("Decorating " + project, () -> {
						markers.setMarkers(processor, MARKER_BND_GENERATE);
					});

					Set<File> outputs = model.getGenerate()
						.getOutputDirs();

					after.accept("Refreshing outputs", () -> {
						for (File f : outputs) {
							IResource r = Central.toResource(f);
							if (r != null) {
								r.refreshLocal(IResource.DEPTH_INFINITE, null);
							}
						}
					});
				}
				return null;
			}, null);
		} catch (Exception e) {
			logger.logError("generating phase, aboutToBuild", e);
		}
		return READY_FOR_BUILD;
	}
}
