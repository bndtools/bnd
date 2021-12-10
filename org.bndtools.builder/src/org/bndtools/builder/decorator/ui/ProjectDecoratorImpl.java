package org.bndtools.builder.decorator.ui;

import org.bndtools.api.builder.IProjectDecorator;
import org.bndtools.builder.handlers.component.ComponentMarker;
import org.eclipse.core.resources.IProject;
import org.osgi.service.component.annotations.Component;

@Component
public class ProjectDecoratorImpl implements IProjectDecorator {

	@Override
	public void updateDecoration(IProject project, BndProjectInfo model) throws Exception {
		PackageDecorator.updateDecoration(project, model);
		ComponentMarker.updateComponentMarkers(project, model);
	}

}
