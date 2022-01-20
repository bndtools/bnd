package bndtools.central;

import java.util.Map;

import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This component adds whiteboard facility to ResourceListeners.
 */
@Component(immediate = true)
public class ResourceWhiteboard {
	final IWorkspace	eclipseWorkspace;

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addResourceChangeListener(IResourceChangeListener listener, Map<String, Object> properties) {
		Integer mask = (Integer) properties.get("org.eclipse.core.resources.eventmask");
		if (mask == null) {
			eclipseWorkspace.addResourceChangeListener(listener);
		} else {
			eclipseWorkspace.addResourceChangeListener(listener, mask);
		}
	}

	void updateResourceChangeListener(IResourceChangeListener listener, Map<String, Object> properties) {
		removeResourceChangeListener(listener);
		addResourceChangeListener(listener, properties);
	}

	void removeResourceChangeListener(IResourceChangeListener listener) {
		eclipseWorkspace.removeResourceChangeListener(listener);
	}

	@Activate
	public ResourceWhiteboard(@Reference
	IWorkspace eclipseWorkspace) {
		this.eclipseWorkspace = eclipseWorkspace;
	}
}
