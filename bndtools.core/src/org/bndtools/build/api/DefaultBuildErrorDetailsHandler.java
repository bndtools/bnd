package org.bndtools.build.api;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Processor.FileLine;
import aQute.service.reporter.Report.Location;
import bndtools.central.Central;

public final class DefaultBuildErrorDetailsHandler extends AbstractBuildErrorDetailsHandler {
	static String										HEADER_S	= "\\s*(\\s|:|=)[^\\\\\n]*(\\\\\n[^\\\\\n]*)*";

	public static final DefaultBuildErrorDetailsHandler	INSTANCE	= new DefaultBuildErrorDetailsHandler();

	@Override
	public List<MarkerData> generateMarkerData(IProject project, Processor model, Location location) throws Exception {

		return Collections.singletonList(getMarkerData(project, model, location));
	}

	/**
	 * Use the location object to find the line number and text part.
	 */
	public static MarkerData getMarkerData(IProject p, Processor model, Location location) throws Exception {

		File file = model.getPropertiesFile();
		if (file == null) {
			if (model instanceof Project) {
				file = model.getFile(Project.BNDFILE);
			} else if (model instanceof Workspace) {
				file = model.getFile(Workspace.BUILDFILE);
			}
		}

		if (location.file != null)
			file = new File(location.file);

		int start = -1, end = -1, line = location.line;

		if (location.header != null && location.line == 0) {
			Processor rover = model;
			if (location.file != null) {
				File props = new File(location.file);
				if (props.isFile() && !props.equals(model.getPropertiesFile())) {
					rover = new Processor(model);
					rover.setProperties(props);
				}
			}

			try {
				FileLine fl = rover.getHeader(location.header, location.context);
				if (fl != null) {
					file = fl.file;
					line = fl.line;
					start = fl.start;
					end = fl.end;
				}
			} catch (Exception e) {
				// ignore, this was a bug in bnd. It could not handle if certain
				// files were not there during starting
			}
		}

		IResource resource = Central.toResource(file);
		String extra = "";

		if (resource == null)
			resource = AbstractBuildErrorDetailsHandler.getDefaultResource(p);
		else if (resource.getProject() != p) {
			//
			// Make sure we never escape our project
			//
			extra = ": see " + resource + "#" + line + 1;
			resource = p;
			line = 1;
			end = start = -1;
		}

		Map<String, Object> attribs = new HashMap<>();
		attribs.put(IMarker.MESSAGE, location.message.trim() + extra);
		attribs.put(IMarker.LINE_NUMBER, line + 1);
		if (end != -1 && start != -1) {
			attribs.put(IMarker.CHAR_START, start);
			attribs.put(IMarker.CHAR_END, end);
		}

		return new MarkerData(resource, attribs, false);
	}

}
