package org.bndtools.build.api;

import java.util.Map;

import org.eclipse.core.resources.IResource;

public class MarkerData {

    private final IResource resource;
    private final Map<String,Object> attribs;
    private final boolean hasResolutions;

    public MarkerData(IResource resource, Map<String,Object> attribs, boolean hasResolutions) {
        this.resource = resource;
        this.attribs = attribs;
        this.hasResolutions = hasResolutions;
    }

    public IResource getResource() {
        return resource;
    }

    public Map<String,Object> getAttribs() {
        return attribs;
    }

    public boolean hasResolutions() {
        return hasResolutions;
    }

}
