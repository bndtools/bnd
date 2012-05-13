package bndtools.model.obr;

import java.util.Comparator;

import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class ResolutionFailureTreeSorter extends ViewerSorter implements Comparator<Object> {
    
    private Comparator<Requirement> reqComp = new RequirementComparator();
    private Comparator<Resource> resComp = new ResourceComparator();
    
    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        return compare(e1, e2);
    }

    public int compare(Object e1, Object e2) {
        if (e1 instanceof PotentialMatch && e2 instanceof PotentialMatch) {
            PotentialMatch pm1 = (PotentialMatch) e1;
            PotentialMatch pm2 = (PotentialMatch) e2;
            return reqComp.compare(pm1.getRequirement(), pm2.getRequirement());
        }
        
        if (e1 instanceof Resource && e2 instanceof Resource) {
            Resource r1 = (Resource) e1;
            Resource r2 = (Resource) e2;
            return resComp.compare(r1, r2);
        }
        
        // How did we end up here??
        if (e1.equals(e2))
            return 0;
        return e1.hashCode() - e2.hashCode();
    }
}
