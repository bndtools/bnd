package bndtools.model.obr;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.felix.bundlerepository.Resource;

public class ResourceComparator implements Comparator<Resource>, Serializable {
    private static final long serialVersionUID = 3013988551450867308L;

    public int compare(Resource res1, Resource res2) {
        int diff;

        diff = ComparatorUtils.safeCompare(res1.getSymbolicName(), res2.getSymbolicName());
        if (diff != 0)
            return diff;

        diff = ComparatorUtils.safeCompare(res1.getVersion(), res2.getVersion());

        return diff;
    }

}
