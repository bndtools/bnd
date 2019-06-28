package org.bndtools.builder.decorator.ui;

import org.bndtools.build.api.IProjectDecorator;
import org.bndtools.builder.ComponentMarker;
import org.eclipse.core.resources.IProject;
import org.osgi.annotation.bundle.Capability;
import org.osgi.namespace.service.ServiceNamespace;

@Capability(namespace = ServiceNamespace.SERVICE_NAMESPACE, attribute = {
	"objectClass:List<String>=org.bndtools.build.api.IProjectDecorator"
}, uses = IProjectDecorator.class)
public class ProjectDecoratorImpl implements IProjectDecorator {

	@Override
	public void updateDecoration(IProject project, BndProjectInfo model) throws Exception {
		PackageDecorator.updateDecoration(project, model);
		ComponentMarker.updateComponentMarkers(project, model);
	}

}
