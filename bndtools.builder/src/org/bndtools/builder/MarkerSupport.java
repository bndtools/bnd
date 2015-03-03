package org.bndtools.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.IValidator;
import org.bndtools.api.Logger;
import org.bndtools.build.api.BuildErrorDetailsHandler;
import org.bndtools.build.api.BuildErrorDetailsHandlers;
import org.bndtools.build.api.DefaultBuildErrorDetailsHandler;
import org.bndtools.build.api.MarkerData;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaModelMarker;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Processor;
import aQute.service.reporter.Report.Location;

class MarkerSupport {
    private static final ILogger logger = Logger.getLogger(BndtoolsBuilder.class);
    private final IProject project;
    private final MultiStatus validationResults = new MultiStatus(BndtoolsBuilder.PLUGIN_ID, 0, "Validation errors in bnd project", null);

    MarkerSupport(IProject project) {
        this.project = project;
    }

    boolean hasBlockingErrors(DeltaWrapper dw) {
        try {
            if (containsError(dw, project.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE)))
                return true;

            if (containsError(dw, project.findMarkers(BndtoolsConstants.MARKER_BND_PATH_PROBLEM, true, IResource.DEPTH_INFINITE)))
                return true;

            return false;
        } catch (CoreException e) {
            logger.logError("Error looking for project problem markers", e);
            return false;
        }
    }

    void setMarkers(Processor model, String markerType) throws Exception {
        deleteMarkers(markerType);
        createMarkers(model, IMarker.SEVERITY_ERROR, model.getErrors(), markerType);
        createMarkers(model, IMarker.SEVERITY_WARNING, model.getWarnings(), markerType);
    }

    void deleteMarkers(String markerType) throws CoreException {
        if (markerType.equals("*")) {
            deleteMarkers(BndtoolsConstants.MARKER_BND_PROBLEM);
            deleteMarkers(BndtoolsConstants.MARKER_BND_PATH_PROBLEM);
            deleteMarkers(BndtoolsConstants.MARKER_BND_WORKSPACE_PROBLEM);
        } else
            project.deleteMarkers(markerType, true, IResource.DEPTH_INFINITE);
    }

    void createValidationResultMarkers(Processor model) throws Exception {
        if (!validationResults.isOK()) {
            for (IStatus status : validationResults.getChildren()) {
                createMarkers(model, status);
            }
        }
    }

    private void createMarkers(Processor model, int severity, Collection<String> msgs, String markerType) throws Exception {
        for (String msg : msgs) {
            createMarker(model, severity, msg, markerType);
        }
    }

    private void createMarkers(Processor model, IStatus status) throws Exception {
        if (status.isMultiStatus()) {
            for (IStatus child : status.getChildren()) {
                createMarkers(model, child);
            }
            return;
        }

        createMarker(model, iStatusSeverityToIMarkerSeverity(status), status.getMessage(), BndtoolsConstants.MARKER_BND_PROBLEM);
    }

    void createMarker(Processor model, int severity, String formatted, String markerType) throws Exception {
        Location location = model != null ? model.getLocation(formatted) : null;
        if (location != null) {
            String type = location.details != null ? location.details.getClass().getName() : null;
            BuildErrorDetailsHandler handler = BuildErrorDetailsHandlers.INSTANCE.findHandler(type);

            List<MarkerData> markers = handler.generateMarkerData(project, model, location);
            for (MarkerData markerData : markers) {
                IResource resource = markerData.getResource();
                if (resource != null) {
                    IMarker marker = resource.createMarker(markerType);
                    marker.setAttribute(IMarker.SEVERITY, severity);
                    marker.setAttribute("$bndType", type);
                    marker.setAttribute(BuildErrorDetailsHandler.PROP_HAS_RESOLUTIONS, markerData.hasResolutions());
                    for (Entry<String,Object> attrib : markerData.getAttribs().entrySet())
                        marker.setAttribute(attrib.getKey(), attrib.getValue());
                }
            }
            return;
        }

        String defaultResource = model instanceof Project ? Project.BNDFILE : model instanceof Workspace ? Workspace.BUILDFILE : "";
        IMarker marker = DefaultBuildErrorDetailsHandler.getDefaultResource(project, defaultResource).createMarker(markerType);
        marker.setAttribute(IMarker.SEVERITY, severity);
        marker.setAttribute(IMarker.MESSAGE, formatted);
    }

    private int iStatusSeverityToIMarkerSeverity(IStatus status) {
        int severity;
        switch (status.getSeverity()) {
        case IStatus.CANCEL :
        case IStatus.ERROR :
            severity = IMarker.SEVERITY_ERROR;
            break;
        case IStatus.WARNING :
            severity = IMarker.SEVERITY_WARNING;
            break;
        default :
            severity = IMarker.SEVERITY_INFO;
        }

        return severity;
    }

    private static boolean containsError(DeltaWrapper dw, IMarker[] markers) {
        if (markers != null)
            for (IMarker marker : markers) {

                int severity = marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
                if (severity == IMarker.SEVERITY_ERROR) {

                    //
                    // markers in the test folder don't count.
                    // They should not rebuild nor stop building
                    // the target
                    //

                    if (dw.isTestBin(marker.getResource()))
                        continue;

                    return true;
                }
            }
        return false;
    }

    static List<IValidator> loadValidators() {
        List<IValidator> validators = null;
        IConfigurationElement[] validatorElems = Platform.getExtensionRegistry().getConfigurationElementsFor(BndtoolsConstants.CORE_PLUGIN_ID, "validators");
        if (validatorElems != null && validatorElems.length > 0) {
            validators = new ArrayList<IValidator>(validatorElems.length);
            for (IConfigurationElement elem : validatorElems) {
                try {
                    validators.add((IValidator) elem.createExecutableExtension("class"));
                } catch (Exception e) {
                    logger.logError("Unable to instantiate validator: " + elem.getAttribute("name"), e);
                }
            }
        }
        return validators;
    }

    void validate(Project model) throws Exception {
        List<IValidator> validators = loadValidators();
        if (validators != null) {
            for (Builder builder : model.getSubBuilders()) {
                validate(builder, validators);
            }
        }
    }

    void validate(Builder builder, List<IValidator> validators) {
        for (IValidator validator : validators) {
            IStatus status = validator.validate(builder);
            if (!status.isOK())
                validationResults.add(status);
        }
    }

}
