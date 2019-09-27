package org.bndtools.build.api;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.IMarkerResolution;
import org.osgi.annotation.versioning.ProviderType;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Processor;
import aQute.service.reporter.Report.Location;

/**
 * Implementations of this interface handle a specific type of build error
 * detail as returned from bnd's {@link Processor#getLocation(String)} method.
 *
 * @author Neil Bartlett <njbartlett@gmail.com>
 */
@ProviderType
public interface BuildErrorDetailsHandler {

	String PROP_HAS_RESOLUTIONS = "bndHasResolutions";

	List<MarkerData> generateMarkerData(IProject project, Project model, Location location) throws Exception;

	/**
	 * Bridge method to convert a Processor model to a Project model. This is
	 * implemented in the abstract base class. This allows handlers to react to
	 * Builder, ProjectBuilder, Workspace, and Project.
	 * <p>
	 * This method returns a list of markers that have as many attributes set as
	 * possible.
	 *
	 * @param project The Eclipse project
	 * @param model The bnd Processor
	 * @param location the location of the error
	 * @return A list of marker data
	 */
	List<MarkerData> generateMarkerData(IProject project, Processor model, Location location) throws Exception;

	List<IMarkerResolution> getResolutions(IMarker marker);

	List<ICompletionProposal> getProposals(IMarker marker);

}
