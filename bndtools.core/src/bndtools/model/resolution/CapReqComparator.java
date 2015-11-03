package bndtools.model.resolution;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.core.ui.resource.R5LabelFormatter;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

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
        String attrib1 = c1.getAttributes().get(attribName).toString();
        String attrib2 = c2.getAttributes().get(attribName).toString();
        int attribDiff = attrib1.compareTo(attrib2);
        if (attribDiff != 0)
            return attribDiff;

        // Compare the versions
        String versionAttribName = R5LabelFormatter.getVersionAttributeName(ns1);
        if (versionAttribName == null)
            return 0;
        Version v1 = (Version) c1.getAttributes().get(versionAttribName);
        if (v1 == null)
            v1 = Version.emptyVersion;
        Version v2 = (Version) c2.getAttributes().get(versionAttribName);
        if (v2 == null)
            v2 = Version.emptyVersion;
        return v1.compareTo(v2);
    }

    private int compareReqToReq(Requirement r1, Requirement r2) {
        // Compare namespaces
        String ns1 = r1.getNamespace();
        String ns2 = r2.getNamespace();
        int nsDiff = ns1.compareTo(ns2);
        if (nsDiff != 0)
            return nsDiff;

        // Get the main attribute
        Pattern filterPattern = R5LabelFormatter.getFilterPattern(ns1);
        if (filterPattern == null)
            return 0;
        String filter1 = r1.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
        Matcher m1 = filterPattern.matcher(filter1);
        String filter2 = r2.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
        Matcher m2 = filterPattern.matcher(filter2);
        if (!m1.find() || !m2.find())
            return 0;
        String attrib1 = m1.group(1);
        String attrib2 = m2.group(1);
        if (attrib1 == null || attrib2 == null)
            return 0;
        return attrib1.compareTo(attrib2);
    }
}