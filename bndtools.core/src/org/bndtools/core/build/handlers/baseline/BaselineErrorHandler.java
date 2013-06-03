package org.bndtools.core.build.handlers.baseline;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bndtools.build.api.AbstractBuildErrorDetailsHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.build.Project;
import aQute.bnd.differ.Baseline.Info;
import aQute.lib.collections.LineCollection;
import aQute.lib.io.IO;
import aQute.service.reporter.Report.Location;

public class BaselineErrorHandler extends AbstractBuildErrorDetailsHandler {

    private static final String PACKAGEINFO = "packageinfo";

    public IResource findMarkerTargetResource(IProject project, Project model, Location location) throws Exception {
        Info baselineInfo = (Info) location.details;
        IJavaProject javaProject = JavaCore.create(project);
        for (IClasspathEntry entry : javaProject.getRawClasspath()) {
            if (IClasspathEntry.CPE_SOURCE == entry.getEntryKind()) {
                IPath entryPath = entry.getPath();
                IPath pkgPath = entryPath.append(baselineInfo.packageName.replace('.', '/'));

                // TODO: handle package-info.java

                IPath pkgInfoPath = pkgPath.append(PACKAGEINFO);
                IFile pkgInfoFile = project.getWorkspace().getRoot().getFile(pkgInfoPath);
                if (pkgInfoFile != null && pkgInfoFile.exists()) {
                    return pkgInfoFile;
                }
            }
        }
        return null;
    }

    public Map<String,Object> createMarkerAttributes(IProject project, Project model, Location location, IResource resource) throws Exception {
        Map<String,Object> map = new HashMap<String,Object>();

        map.put(IMarker.MESSAGE, location.message.trim());
        findAndSetLocation(map, resource.getLocation().toFile());

        return map;
    }

    private void findAndSetLocation(Map<String,Object> attribs, File file) {
        int lineNum = 1;
        LineCollection lines = null;
        try {
            lines = new LineCollection(file);
            while (lines.hasNext()) {
                String line = lines.next();
                if (line.startsWith("version")) {
                    attribs.put(IMarker.LINE_NUMBER, lineNum);
                    attribs.put(IMarker.CHAR_START, 0);
                    attribs.put(IMarker.CHAR_END, line.length());
                    break;
                }
                lineNum++;
            }
        } catch (Exception e) {
            // ignore
        } finally {
            if (lines != null)
                IO.close(lines);
        }
    }
}
