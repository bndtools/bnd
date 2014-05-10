package org.bndtools.builder.handlers.activator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bndtools.build.api.AbstractBuildErrorDetailsHandler;
import org.bndtools.build.api.MarkerData;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.IMarkerResolution;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Verifier.BundleActivatorError;
import aQute.service.reporter.Report.Location;

public class BundleActivatorErrorHandler extends AbstractBuildErrorDetailsHandler {

    @Override
    public List<MarkerData> generateMarkerData(IProject project, Project model, Location location) throws Exception {
        List<MarkerData> result = new ArrayList<MarkerData>();

        BundleActivatorError baError = (BundleActivatorError) location.details;

        IJavaProject javaProject = JavaCore.create(project);

        Map<String,Object> attribs = createMessageMarkerAttributes(baError, location.message);
        //Eclipse line numbers are 1 indexed
        attribs.put(IMarker.LINE_NUMBER, location.line + 1);

        // Add a marker to the bnd file on the BundleActivator line
        result.add(new MarkerData(getDefaultResource(project), attribs, false));

        MarkerData md;
        switch (baError.errorType) {
        case NO_SUITABLE_CONSTRUCTOR :
            md = createMethodMarkerData(javaProject, baError.activatorClassName, "<init>", "()V", createMessageMarkerAttributes(baError, location.message), false);
            if (md != null) {
                result.add(md);
                break;
            }
            //$FALL-THROUGH$
        case IS_INTERFACE :
        case IS_ABSTRACT :
        case NOT_PUBLIC :
        case NOT_AN_ACTIVATOR :
        case DEFAULT_PACKAGE :
        case IS_IMPORTED :
            md = createTypeMarkerData(javaProject, baError.activatorClassName, createMessageMarkerAttributes(baError, location.message), false);
            if (md != null)
                result.add(md);
            break;
        case NOT_ACCESSIBLE :
        default :
            //No file to mark
            break;
        }

        return result;
    }

    private Map<String,Object> createMessageMarkerAttributes(BundleActivatorError baError, String message) {
        Map<String,Object> attribs = new HashMap<String,Object>();
        attribs.put("BundleActivatorError.activatorClassName", baError.activatorClassName);
        attribs.put("BundleActivatorError.errorType", baError.errorType.toString());
        attribs.put(IMarker.MESSAGE, message.trim());
        return attribs;
    }

    @Override
    public List<IMarkerResolution> getResolutions(IMarker marker) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ICompletionProposal> getProposals(IMarker marker) {
        // TODO Auto-generated method stub
        return null;
    }

}
