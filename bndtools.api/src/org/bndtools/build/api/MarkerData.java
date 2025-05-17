package org.bndtools.build.api;

import java.util.Map;

import org.eclipse.core.resources.IResource;

public class MarkerData {

	private final IResource				resource;
	private final Map<String, Object>	attribs;
	private final boolean				hasResolutions;
	private final String				typeOverride;

	public MarkerData(IResource resource, Map<String, Object> attribs, boolean hasResolutions) {
		this(resource, attribs, hasResolutions, null);
	}

	public MarkerData(IResource resource, Map<String, Object> attribs, boolean hasResolutions, String typeOverride) {
		this.resource = resource;
		this.attribs = attribs;
		this.hasResolutions = hasResolutions;
		this.typeOverride = typeOverride;
	}

	public IResource getResource() {
		return resource;
	}

	public Map<String, Object> getAttribs() {
		return attribs;
	}

	public boolean hasResolutions() {
		return hasResolutions;
	}

	public String getTypeOverride() {
		return typeOverride;
	}

}
