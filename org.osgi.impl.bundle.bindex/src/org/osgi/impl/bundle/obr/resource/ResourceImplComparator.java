package org.osgi.impl.bundle.obr.resource;

import java.util.Comparator;

public class ResourceImplComparator implements Comparator<ResourceImpl> {
	public int compare(ResourceImpl r1, ResourceImpl r2) {
		if (r2 == null) {
			return -1;
		}
		if (r1 == null) {
			return 1;
		}

		return r1.toString().compareTo(r2.toString());
	}
}
