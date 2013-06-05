package org.bndtools.core.build.handlers.baseline;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bndtools.build.api.AbstractBuildErrorDetailsHandler;
import org.bndtools.build.api.MarkerData;
import org.bndtools.core.utils.parse.properties.LineType;
import org.bndtools.core.utils.parse.properties.PropertiesLineReader;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IRegion;
import org.osgi.framework.Constants;

import aQute.bnd.build.Project;
import aQute.bnd.differ.Baseline.BundleInfo;
import aQute.bnd.osgi.Builder;
import aQute.lib.io.IO;
import aQute.service.reporter.Report.Location;

public class BundleVersionErrorHandler extends AbstractBuildErrorDetailsHandler {

    private static final String PROP_SUGGESTED_VERSION = "suggestedVersion";

    public List<MarkerData> generateMarkerData(IProject project, Project model, Location location) throws Exception {
        List<MarkerData> result = new LinkedList<MarkerData>();

        BundleInfo info = (BundleInfo) location.details;
        for (Builder builder : model.getSubBuilders()) {
            if (builder.getBsn().equals(info.bsn)) {
                // no-sub bundles.. this is bnd.bnd
                IFile bndFile = project.getFile(Project.BNDFILE);
                LineLocation loc = findBundleVersionHeader(bndFile);

                if (loc != null) {
                    Map<String,Object> attribs = new HashMap<String,Object>();
                    attribs.put(IMarker.MESSAGE, location.message);
                    attribs.put(IMarker.LINE_NUMBER, loc.lineNum);
                    attribs.put(IMarker.CHAR_START, loc.start);
                    attribs.put(IMarker.CHAR_END, loc.end);

                    attribs.put(PROP_SUGGESTED_VERSION, info.suggestedVersion.toString());

                    result.add(new MarkerData(bndFile, attribs, true));
                }
            }
        }

        // TODO: what if it's on a sub-bundle

        return result;
    }

    private LineLocation findBundleVersionHeader(IFile bndFile) throws Exception {
        File file = bndFile.getLocation().toFile();
        String content = IO.collect(file);

        PropertiesLineReader reader = new PropertiesLineReader(content);
        int lineNum = 1;

        LineType type = reader.next();
        while (type != LineType.eof) {
            if (type == LineType.entry) {
                String entryKey = reader.key();
                if (Constants.BUNDLE_VERSION.equals(entryKey)) {
                    LineLocation loc = new LineLocation();
                    loc.lineNum = lineNum;
                    IRegion region = reader.region();
                    loc.start = region.getOffset();
                    loc.end = region.getOffset() + region.getLength();
                    return loc;
                }
            }

            lineNum++;
            type = reader.next();
        }

        return null;
    }

}
