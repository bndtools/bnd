package bndtools.wizards.workspace;

import java.util.Arrays;
import java.util.List;

import org.bndtools.build.api.AbstractBuildErrorDetailsHandler;
import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

public class MissingWorkspaceBuildErrorHandler extends AbstractBuildErrorDetailsHandler {

    @Override
    public List<IMarkerResolution> getResolutions(IMarker marker) {
        IMarkerResolution[] resolutions = new MissingWorkspaceMarkerResolutionGenerator().getResolutions(marker);
        return Arrays.asList(resolutions);
    }
}
