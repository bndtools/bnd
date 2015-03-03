package org.bndtools.build.api;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
    static String HEADER_S = "\\s*(\\s|:|=)[^\\\\\n]*(\\\\\n[^\\\\\n]*)*";

    public static final DefaultBuildErrorDetailsHandler INSTANCE = new DefaultBuildErrorDetailsHandler();

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

        if (location.header != null && location.line == 0) {
            Pattern pattern = getPattern(location.header);
            FileLine fl = model.getHeader(pattern);
            if (fl.file.isFile()) {
                file = fl.file;
                location.line = fl.line;
                location.length = fl.length;
            }
        }

        IResource resource = Central.toResource(file);
        if (resource == null)
            resource = AbstractBuildErrorDetailsHandler.getDefaultResource(p);

        Map<String,Object> attribs = new HashMap<String,Object>();
        attribs.put(IMarker.MESSAGE, location.message.trim());
        attribs.put(IMarker.LINE_NUMBER, location.line + 1);

        return new MarkerData(resource, attribs, false);
    }

    private static Pattern getPattern(String header) {
        StringBuilder sb = new StringBuilder();
        sb.append("^").append(Pattern.quote(header));
        sb.append(HEADER_S);
        return Pattern.compile(sb.toString(), Pattern.MULTILINE);
    }

}