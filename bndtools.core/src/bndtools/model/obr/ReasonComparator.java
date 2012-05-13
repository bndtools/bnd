package bndtools.model.obr;

import java.util.Comparator;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;

public class ReasonComparator implements Comparator<Reason> {

    private Comparator<Resource> resourceComparator;
    private Comparator<Requirement> requirementComparator;

    public ReasonComparator(Comparator<Resource> resourceComparator, Comparator<Requirement> requirementComparator) {
        this.resourceComparator = resourceComparator;
        this.requirementComparator = requirementComparator;
    }

    public int compare(Reason reason1, Reason reason2) {
        int diff;

        diff = ComparatorUtils.safeCompare(reason1.getResource(), reason2.getResource(), resourceComparator);
        if (diff != 0)
            return diff;

        diff = ComparatorUtils.safeCompare(reason1.getRequirement(), reason2.getRequirement(), requirementComparator);

        return diff;
    }

}
