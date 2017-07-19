package org.bndtools.builder;

import static org.bndtools.api.BndtoolsConstants.BNDTOOLS_MARKER_CONTEXT_ATTR;
import static org.bndtools.api.BndtoolsConstants.BNDTOOLS_MARKER_FILE_ATTR;
import static org.bndtools.api.BndtoolsConstants.BNDTOOLS_MARKER_HEADER_ATTR;
import static org.bndtools.api.BndtoolsConstants.BNDTOOLS_MARKER_REFERENCE_ATTR;
import static org.bndtools.api.BndtoolsConstants.CORE_PLUGIN_ID;
import static org.bndtools.api.BndtoolsConstants.MARKER_BND_MISSING_WORKSPACE;
import static org.bndtools.api.BndtoolsConstants.MARKER_BND_PATH_PROBLEM;
import static org.bndtools.api.BndtoolsConstants.MARKER_BND_PROBLEM;
import static org.bndtools.api.BndtoolsConstants.MARKER_BND_WORKSPACE_PROBLEM;
import static org.bndtools.api.BndtoolsConstants.MARKER_JAVA_BASELINE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.bndtools.api.ILogger;
import org.bndtools.api.IProjectValidator;
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
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaModelMarker;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Processor;
import aQute.service.reporter.Report.Location;
import aQute.service.reporter.Reporter.SetLocation;

class MarkerSupport {
    private static final ILogger logger = Logger.getLogger(BndtoolsBuilder.class);
    private final IProject project;

    MarkerSupport(IProject project) {
        this.project = project;
    }

    boolean hasBlockingErrors(DeltaWrapper dw) {
        try {
            if (containsError(dw, findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER)))
                return true;

            if (containsError(dw, findMarkers(MARKER_BND_PATH_PROBLEM)))
                return true;

            if (containsError(dw, findMarkers(MARKER_BND_MISSING_WORKSPACE)))
                return true;

            return false;
        } catch (CoreException e) {
            logger.logError("Error looking for project problem markers", e);
            return false;
        }
    }

    private IMarker[] findMarkers(String markerType) throws CoreException {
        IMarker[] markers = project.findMarkers(markerType, false, IResource.DEPTH_INFINITE);
        return markers;
    }

    void setMarkers(Processor model, String markerType) throws Exception {
        deleteMarkers(markerType);
        createMarkers(model, IMarker.SEVERITY_ERROR, model.getErrors(), markerType);
        createMarkers(model, IMarker.SEVERITY_WARNING, model.getWarnings(), markerType);
    }

    void deleteMarkers(String markerType) throws CoreException {
        if (markerType.equals("*")) {
            deleteMarkers(MARKER_BND_PROBLEM);
            deleteMarkers(MARKER_BND_PATH_PROBLEM);
            deleteMarkers(MARKER_BND_WORKSPACE_PROBLEM);
            deleteMarkers(MARKER_BND_MISSING_WORKSPACE);
            deleteMarkers(MARKER_JAVA_BASELINE);
        } else
            project.deleteMarkers(markerType, true, IResource.DEPTH_INFINITE);
    }

    private void createMarkers(Processor model, int severity, Collection<String> msgs, String markerType) throws Exception {
        for (String msg : msgs.toArray(new String[0])) {
            createMarker(model, severity, msg, markerType);
        }
    }

    void createMarker(Processor model, int severity, String formatted, String markerType) throws Exception {
        Location location = model != null ? model.getLocation(formatted) : null;
        if (location != null) {
            String type = location.details != null ? location.details.getClass().getName() : null;
            BuildErrorDetailsHandler handler = BuildErrorDetailsHandlers.INSTANCE.findHandler(type);

            List<MarkerData> markers = handler.generateMarkerData(project, model, location);
            for (MarkerData markerData : markers) {
                IResource resource = markerData.getResource();
                if (resource != null && resource.exists()) {
                    String typeOverride = markerData.getTypeOverride();
                    IMarker marker = resource.createMarker(typeOverride != null ? typeOverride : markerType);
                    marker.setAttribute(IMarker.SEVERITY, severity);
                    marker.setAttribute("$bndType", type);

                    //
                    // Set location information
                    if (location.header != null)
                        marker.setAttribute(BNDTOOLS_MARKER_HEADER_ATTR, location.header);
                    if (location.context != null)
                        marker.setAttribute(BNDTOOLS_MARKER_CONTEXT_ATTR, location.context);
                    if (location.file != null)
                        marker.setAttribute(BNDTOOLS_MARKER_FILE_ATTR, location.file);
                    if (location.reference != null)
                        marker.setAttribute(BNDTOOLS_MARKER_REFERENCE_ATTR, location.reference);

                    marker.setAttribute(BuildErrorDetailsHandler.PROP_HAS_RESOLUTIONS, markerData.hasResolutions());
                    for (Entry<String,Object> attrib : markerData.getAttribs().entrySet())
                        marker.setAttribute(attrib.getKey(), attrib.getValue());
                }
            }
            return;
        }

        String defaultResource = model instanceof Project ? Project.BNDFILE : model instanceof Workspace ? Workspace.BUILDFILE : null;
        IResource resource = DefaultBuildErrorDetailsHandler.getDefaultResource(project, defaultResource);
        if (resource.exists()) {
            IMarker marker = resource.createMarker(markerType);
            marker.setAttribute(IMarker.SEVERITY, severity);
            marker.setAttribute(IMarker.MESSAGE, formatted);
        }
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
        IConfigurationElement[] validatorElems = Platform.getExtensionRegistry().getConfigurationElementsFor(CORE_PLUGIN_ID, "validators");
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

            for (IValidator v : validators) {
                try {
                    if (v instanceof IProjectValidator) {
                        ((IProjectValidator) v).validateProject(model);
                    } else {
                        try (ProjectBuilder pb = model.getBuilder(null)) {
                            for (Builder builder : pb.getSubBuilders()) {
                                IStatus status = v.validate(builder);
                                report(builder, status);
                                model.getInfo(builder);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.logError("Validator error", e);
                }
            }
        }
    }

    private void report(Processor reporter, IStatus status) {
        if (status == null || status.isOK())
            return;

        if (status.isMultiStatus()) {
            for (IStatus s : status.getChildren())
                report(reporter, s);
        } else {
            SetLocation location;
            Throwable exception = status.getException();
            if (exception != null)
                if (status.getSeverity() == IStatus.ERROR)
                    location = reporter.exception(exception, status.getMessage());
                else
                    location = reporter.warning(status.getMessage() + ": " + exception);
            else {
                if (status.getSeverity() == IStatus.ERROR) {
                    location = reporter.error(status.getMessage());
                } else {
                    location = reporter.warning(status.getMessage());
                }
            }
            location.file(reporter.getPropertiesFile().getAbsolutePath());
        }
    }
}
