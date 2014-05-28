package aQute.bnd.jpm;

import java.util.*;

import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.service.library.*;
import aQute.service.library.Library.RevisionRef;

public class ResourceDescriptorImpl extends ResourceDescriptor {

	public ResourceDescriptorImpl(RevisionRef ref) {
		this.revision = ref;
	}

	final Library.RevisionRef	revision;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(id);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourceDescriptorImpl other = (ResourceDescriptorImpl) obj;
		if (!Arrays.equals(id, other.id))
			return false;
		return true;
	}

}
