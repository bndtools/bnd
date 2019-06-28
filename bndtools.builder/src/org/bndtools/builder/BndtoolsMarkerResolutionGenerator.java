package org.bndtools.builder;

import java.util.List;

import org.bndtools.build.api.BuildErrorDetailsHandler;
import org.bndtools.build.api.BuildErrorDetailsHandlers;
import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.IMarkerResolutionGenerator2;

public class BndtoolsMarkerResolutionGenerator implements IMarkerResolutionGenerator, IMarkerResolutionGenerator2 {

	@Override
	public boolean hasResolutions(IMarker marker) {
		boolean attrib = marker.getAttribute(BuildErrorDetailsHandler.PROP_HAS_RESOLUTIONS, false);
		return attrib;
	}

	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		String type = marker.getAttribute("$bndType", (String) null);
		if (type == null)
			return new IMarkerResolution[0];

		BuildErrorDetailsHandler handler = BuildErrorDetailsHandlers.INSTANCE.findHandler(type);
		if (handler == null)
			return new IMarkerResolution[0];

		List<IMarkerResolution> resolutions = handler.getResolutions(marker);
		return resolutions != null ? resolutions.toArray(new IMarkerResolution[0]) : new IMarkerResolution[0];
	}

}
