package bndtools.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdapterFactory;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Project;
import bndtools.Plugin;
import bndtools.central.Central;

@Component(service = IAdapterFactory.class, property = {
	IAdapterFactory.SERVICE_PROPERTY_ADAPTABLE_CLASS + "=org.eclipse.core.resources.IProject",
	IAdapterFactory.SERVICE_PROPERTY_ADAPTER_NAMES + "=aQute.bnd.build.Project"
})
public class BndAdapter implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType == Project.class) {
			if (adaptableObject instanceof IProject project) {
				try {
					if (project.hasNature(Plugin.BNDTOOLS_NATURE)) {
						// this project is for sure a bndtools project ...
						Project bndproject = Central.getProject(project);
						return adapterType.cast(project);
					}
				} catch (Exception e) {
					// can't adapt that project then...
				}
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] {
			Project.class
		};
	}

}
