package bndtools.internal;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IPath;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Workspace;
import bndtools.Plugin;
import bndtools.central.Central;

@Component(service = IAdapterFactory.class, property = {
	IAdapterFactory.SERVICE_PROPERTY_ADAPTABLE_CLASS + "=org.eclipse.core.resources.IProject",
	IAdapterFactory.SERVICE_PROPERTY_ADAPTER_NAMES + "=aQute.bnd.build.Workspace"
})
public class WorkspaceAdapter implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType == Workspace.class) {
			if (adaptableObject instanceof IProject project) {
				try {
					if (project.hasNature(Plugin.BNDTOOLS_NATURE)) {
						// this project is for sure a bndtools project ...
						return adapterType.cast(Central.getWorkspace());
					}
					// for other cases simply return the nearest one...
					IPath location = project.getLocation();
					if (location != null) {
						File file = location.toFile();
						return adapterType.cast(Workspace.findWorkspace(file));
					}
				} catch (Exception e) {
					// can't use that project then?!?
				}
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] {
			Workspace.class
		};
	}

}
