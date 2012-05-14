package bndtools.model.obr;

import java.util.Comparator;

import org.apache.felix.bundlerepository.Reason;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

/**
 * Sorts a flat list of {@link Reason} objects, with "INITIAL" coming first.
 * 
 * @author Neil Bartlett
 */
public class ReasonSorter extends ViewerSorter {

    private final Comparator<Reason> comparator = new ReasonComparator(new ResourceComparator(), new RequirementComparator());

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        Reason r1 = (Reason) e1;
        Reason r2 = (Reason) e2;

        return comparator.compare(r1, r2);
    }

}
