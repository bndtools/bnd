package aQute.bnd.service.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;

public interface CompositeResource extends Resource {

	static List<Resource> expand(Collection<? extends Resource> resources) {
		List<Resource> result = new ArrayList<>();
		for (Resource r : resources) {
			if (r instanceof CompositeResource cr) {
				result.addAll(cr.all());
			} else
				result.add(r);
		}
		return result;
	}

	List<Resource> all();

	static List<Resource> expand(Resource resource) {
		if (resource instanceof CompositeResource cr)
			return cr.all();
		return Collections.singletonList(resource);
	}

	List<Resource> getSupportingResources();

	default Resource getPrimaryResource() {
		return this;
	}

	default boolean hasIdentity() {
		return !getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).isEmpty();
	}

}
