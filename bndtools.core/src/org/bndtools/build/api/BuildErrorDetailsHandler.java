package org.bndtools.build.api;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.IMarkerResolution;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Processor;
import aQute.service.reporter.Report.Location;

/**
 * Implementations of this interface handle a specific type of build error detail as returned from bnd's
 * {@link Processor#getLocation(String)} method.
 * 
 * @author Neil Bartlett <njbartlett@gmail.com>
 */
public interface BuildErrorDetailsHandler {

    public static String PROP_HAS_RESOLUTIONS = "bndHasResolutions";

    List<MarkerData> generateMarkerData(IProject project, Project model, Location location) throws Exception;

    List<IMarkerResolution> getResolutions(IMarker marker);

    List<ICompletionProposal> getProposals(IMarker marker);

}
