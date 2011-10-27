package org.osgi.impl.bundle.obr.resource;

import java.util.Comparator;

public class ResourceImplComparator implements Comparator<ResourceImpl> {
	private String getName(ResourceImpl impl) {
		String s = impl.getSymbolicName();
		if (s != null) {
			return s;
		} else {
			return "no-symbolic-name";
		}
	}

	public int compare(ResourceImpl r1, ResourceImpl r2) {
		String s1 = getName(r1);
		String s2 = getName(r2);
		return s1.compareTo(s2);
	}
}
