package aQute.bnd.resource.repository;

import java.lang.reflect.*;
import java.util.*;

import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;

public class ResourceDescriptorImpl extends ResourceDescriptor implements Comparable<ResourceDescriptorImpl> {

	public Set<String>	repositories = new HashSet<String>();
	
	public ResourceDescriptorImpl() {}

	public ResourceDescriptorImpl(ResourceDescriptor ref) throws IllegalAccessException {
		for (Field f : ref.getClass().getFields()) {
			f.set(this, f.get(ref));
		}
	}

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

	public int compareTo(ResourceDescriptorImpl var0) {
		for (int i = 0; i < id.length; i++) {
			if (i >= var0.id.length)
				return 1;

			if (id[i] > var0.id[i])
				return 1;

			if (id[i] < var0.id[i])
				return -1;
		}
		if (var0.id.length > id.length)
			return -1;

		return 0;
	}

	public String toString() {
		return bsn + "-" + version;
	}
}
