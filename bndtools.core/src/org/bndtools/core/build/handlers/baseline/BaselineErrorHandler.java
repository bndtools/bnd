package org.bndtools.core.build.handlers.baseline;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bndtools.build.api.AbstractBuildErrorDetailsHandler;
import org.bndtools.build.api.MarkerData;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.IMarkerResolution;
import bndtools.Logger;

import aQute.bnd.build.Project;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.properties.IRegion;
import aQute.bnd.properties.LineType;
import aQute.bnd.properties.PropertiesLineReader;
import aQute.lib.io.IO;
import aQute.service.reporter.Report.Location;

public class BaselineErrorHandler extends AbstractBuildErrorDetailsHandler {

    private static final String PACKAGEINFO = "packageinfo";
    private static final String PROP_SUGGESTED_VERSION = "suggestedVersion";

    public List<MarkerData> generateMarkerData(IProject project, Project model, Location location) throws Exception {
        List<MarkerData> result = new LinkedList<MarkerData>();

        // Generate marker for the packageinfo
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
                    Map<String,Object> attribs = new HashMap<String,Object>();
                    attribs.put(IMarker.MESSAGE, location.message.trim());
                    attribs.put(PROP_SUGGESTED_VERSION, baselineInfo.suggestedVersion.toString());

                    LineLocation lineLoc = findVersionLocation(pkgInfoFile.getLocation().toFile());
                    if (lineLoc != null) {
                        attribs.put(IMarker.LINE_NUMBER, lineLoc.lineNum);
                        attribs.put(IMarker.CHAR_START, lineLoc.start);
                        attribs.put(IMarker.CHAR_END, lineLoc.end);
                    }

                    result.add(new MarkerData(pkgInfoFile, attribs, true));
                }
            }
        }

        // TODO: Generate marker for the semantic change

        return result;
    }

    @Override
    public List<IMarkerResolution> getResolutions(IMarker marker) {
        List<IMarkerResolution> result = new LinkedList<IMarkerResolution>();

        final String suggestedVersion = marker.getAttribute(PROP_SUGGESTED_VERSION, null);
        if (suggestedVersion != null) {
            result.add(new IMarkerResolution() {
                public void run(IMarker marker) {
                    final IFile file = (IFile) marker.getResource();
                    final IWorkspace workspace = file.getWorkspace();
                    try {
                        workspace.run(new IWorkspaceRunnable() {
                            public void run(IProgressMonitor monitor) throws CoreException {
                                String input = "version " + suggestedVersion;
                                ByteArrayInputStream stream = new ByteArrayInputStream(input.getBytes());
                                file.setContents(stream, false, true, monitor);
                            }
                        }, null);
                    } catch (CoreException e) {
                        Logger.getLogger().logError("Error applying baseline version quickfix.", e);
                    }
                }

                public String getLabel() {
                    return "Change package version to " + suggestedVersion;
                }
            });

        }

        return result;
    }

    @Override
    public List<ICompletionProposal> getProposals(IMarker marker) {
        List<ICompletionProposal> proposals = new LinkedList<ICompletionProposal>();

        String suggestedVersion = marker.getAttribute(PROP_SUGGESTED_VERSION, null);
        int start = marker.getAttribute(IMarker.CHAR_START, 0);
        int end = marker.getAttribute(IMarker.CHAR_END, 0);
        CompletionProposal proposal = new CompletionProposal("version " + suggestedVersion, start, end - start, end, null, "Change package version to " + suggestedVersion, null, null);
        proposals.add(proposal);

        return proposals;
    }

    private LineLocation findVersionLocation(File file) {
        String content;
        try {
            content = IO.collect(file);
            PropertiesLineReader reader = new PropertiesLineReader(content);

            int lineNum = 1;
            LineType type = reader.next();
            while (type != LineType.eof) {
                if (type == LineType.entry) {
                    String key = reader.key();
                    if ("version".equals(key)) {
                        LineLocation loc = new LineLocation();
                        loc.lineNum = lineNum;
                        IRegion region = reader.region();
                        loc.start = region.getOffset();
                        loc.end = region.getOffset() + region.getLength();
                        return loc;
                    }
                }
                type = reader.next();
                lineNum++;
            }
        } catch (Exception e) {
            // ignore
        }

        return null;
    }
}
