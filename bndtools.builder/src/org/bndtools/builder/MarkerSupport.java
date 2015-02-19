package org.bndtools.builder;

import java.util.ArrayList;
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
import aQute.bnd.osgi.Builder;
import aQute.service.reporter.Report.Location;

class MarkerSupport {
    private static final ILogger logger = Logger.getLogger(BndtoolsBuilder.class);
    private final IProject project;
    private final MultiStatus validationResults = new MultiStatus(BndtoolsBuilder.PLUGIN_ID, 0, "Validation errors in bnd project", null);

    MarkerSupport(BndtoolsBuilder builder) {
        this.project = builder.getProject();
    }

    boolean hasBlockingErrors(DeltaWrapper dw) {
        try {
            if (containsError(dw, project.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE)))
                return true;
            return false;
        } catch (CoreException e) {
            logger.logError("Error looking for project problem markers", e);
            return false;
        }
    }

    void createBuildMarkers(Project model) throws Exception {
        List<String> errors = model.getErrors();
        List<String> warnings = model.getWarnings();

        for (String error : errors) {
            addBuildMarkers(model, IMarker.SEVERITY_ERROR, error);
        }
        for (String warning : warnings) {
            addBuildMarkers(model, IMarker.SEVERITY_WARNING, warning);
        }

        if (!validationResults.isOK()) {
            for (IStatus status : validationResults.getChildren()) {
                addBuildMarkers(model, status);
            }
        }
    }

    void clearBuildMarkers() throws CoreException {
        project.deleteMarkers(BndtoolsConstants.MARKER_BND_PROBLEM, true, IResource.DEPTH_INFINITE);
    }

    private void addBuildMarkers(Project model, IStatus status) throws Exception {
        if (status.isMultiStatus()) {
            for (IStatus child : status.getChildren()) {
                addBuildMarkers(model, child);
            }
            return;
        }

        addBuildMarkers(model, iStatusSeverityToIMarkerSeverity(status), status.getMessage());
    }

    void addBuildMarkers(Project model, int severity, String message, Object... args) throws Exception {
        String formatted = String.format(message, args);
        addBuildMarkers(model, severity, formatted);
    }

    void addBuildMarkers(Project model, int severity, String formatted) throws Exception {
        Location location = model != null ? model.getLocation(formatted) : null;
        if (location != null) {
            String type = location.details != null ? location.details.getClass().getName() : null;
            BuildErrorDetailsHandler handler = BuildErrorDetailsHandlers.INSTANCE.findHandler(type);

            List<MarkerData> markers = handler.generateMarkerData(project, model, location);
            for (MarkerData markerData : markers) {
                IResource resource = markerData.getResource();
                if (resource != null) {
                    IMarker marker = resource.createMarker(BndtoolsConstants.MARKER_BND_PROBLEM);
                    marker.setAttribute(IMarker.SEVERITY, severity);
                    marker.setAttribute("$bndType", type);
                    marker.setAttribute(BuildErrorDetailsHandler.PROP_HAS_RESOLUTIONS, markerData.hasResolutions());
                    for (Entry<String,Object> attrib : markerData.getAttribs().entrySet())
                        marker.setAttribute(attrib.getKey(), attrib.getValue());
                }
            }
        } else {
            IMarker marker = DefaultBuildErrorDetailsHandler.getDefaultResource(project).createMarker(BndtoolsConstants.MARKER_BND_PROBLEM);
            marker.setAttribute(IMarker.SEVERITY, severity);
            marker.setAttribute(IMarker.MESSAGE, formatted);
        }
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
