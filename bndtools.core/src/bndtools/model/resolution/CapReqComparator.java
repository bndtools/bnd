package bndtools.model.resolution;

import java.util.Collection;
import java.util.Comparator;

import org.bndtools.core.ui.resource.R5LabelFormatter;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import aQute.bnd.osgi.resource.ResourceUtils;

public class CapReqComparator implements Comparator<Object> {

	@Override
	public int compare(Object o1, Object o2) {
		if (o1 instanceof Requirement)
			return compareReqToObj((Requirement) o1, o2);

		if (o1 instanceof RequirementWrapper)
			return compareReqToObj(((RequirementWrapper) o1).requirement, o2);

		if (o1 instanceof Capability)
			return compareCapToObj((Capability) o1, o2);

		return 0;
	}

	private int compareReqToObj(Requirement r1, Object o2) {
		if (o2 instanceof Requirement)
			return compareReqToReq(r1, (Requirement) o2);

		if (o2 instanceof RequirementWrapper)
			return compareReqToReq(r1, ((RequirementWrapper) o2).requirement);

		// requirements sort before other things
		return -1;
	}

	private int compareCapToObj(Capability c1, Object o2) {
		if (o2 instanceof Capability)
			return compareCapToCap(c1, (Capability) o2);

		// capabilities sort after other things
		return 1;
	}

	private int compareCapToCap(Capability c1, Capability c2) {
		// Compare namespaces
		String ns1 = c1.getNamespace();
		String ns2 = c2.getNamespace();
		int nsDiff = ns1.compareTo(ns2);
		if (nsDiff != 0)
			return nsDiff;

		// Compare the main attribute
		String attribName = R5LabelFormatter.getMainAttributeName(ns1);
		Object attrib1 = c1.getAttributes()
			.get(attribName);
		Object attrib2 = c2.getAttributes()
			.get(attribName);

		if (attrib1 != null && attrib2 != null) {
			int attribDiff = attrib1.toString()
				.compareTo(attrib2.toString());
			if (attribDiff != 0)
				return attribDiff;
		}

		// Compare the versions
		String versionAttribName = R5LabelFormatter.getVersionAttributeName(ns1);
		if (versionAttribName == null)
			return 0;

		Version v1 = highestVersion(c1.getAttributes()
			.get(versionAttribName));
		Version v2 = highestVersion(c2.getAttributes()
			.get(versionAttribName));

		return v1.compareTo(v2);
	}

	private static Version highestVersion(Object attr) {

		if (attr instanceof Version v) {
			return v;
		}

		if (attr instanceof Collection<?> col) {
			// e.g. namespace 'osgi.ee' can contain List<Version>
			// see
			// https://osgi.github.io/osgi/core/framework.namespaces.html#framework.namespaces-ee.namespace
			// so we compare the highest versions
			return col.stream()
				.filter(Version.class::isInstance)
				.map(Version.class::cast)
				.max(Version::compareTo)
				.orElse(Version.emptyVersion);
		}
		return Version.emptyVersion; // null or wrong type
	}

	private int compareReqToReq(Requirement r1, Requirement r2) {
		return ResourceUtils.REQUIREMENT_COMPARATOR.compare(r1, r2);
	}
}
