package bndtools.model.obr;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;

public class CapabilityComparator implements Comparator<Capability> {
    
    private Comparator<Property> propertyComparator;

    public CapabilityComparator(Comparator<Property> propertyComparator) {
        this.propertyComparator = propertyComparator;
    }

    public int compare(Capability cap1, Capability cap2) {
        int diff;
        
        diff = ComparatorUtils.safeCompare(cap1.getName(), cap2.getName());
        if (diff != 0)
            return diff;

        diff = ComparatorUtils.arrayCompare(cap1.getProperties(), cap2.getProperties(), propertyComparator);
        
        return diff;
    }

}
