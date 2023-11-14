package bndtools.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Project;
import bndtools.Activator;
import bndtools.Plugin;
import bndtools.central.Central;

@Component(property = {
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
						return adapterType.cast(bndproject);
					}
				} catch (Exception e) {
					if (e instanceof CoreException core) {
						Activator.getDefault()
							.getLog()
							.log(core.getStatus());

					} else {
						Activator.getDefault()
						.getLog()
							.error("Adaption of " + adaptableObject + " as a bndtools project failed", e);
					}
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
