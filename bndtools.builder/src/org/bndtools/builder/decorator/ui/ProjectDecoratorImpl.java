package org.bndtools.builder.decorator.ui;

import org.bndtools.build.api.IProjectDecorator;
import org.bndtools.builder.ComponentMarker;
import org.eclipse.core.resources.IProject;

public class ProjectDecoratorImpl implements IProjectDecorator {

    @Override
    public void updateDecoration(IProject project, BndProjectInfo model) throws Exception {
        PackageDecorator.updateDecoration(project, model);
        ComponentMarker.updateComponentMarkers(project, model);
    }

}
