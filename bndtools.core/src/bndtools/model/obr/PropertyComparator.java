package bndtools.model.obr;

import java.util.Comparator;

import org.apache.felix.bundlerepository.Property;

public class PropertyComparator implements Comparator<Property> {

    public int compare(Property prop1, Property prop2) {
        int diff;

        diff = ComparatorUtils.safeCompare(prop1.getName(), prop2.getName());
        if (diff != 0)
            return diff;
        diff = ComparatorUtils.safeCompare(prop1.getValue(), prop2.getValue());
        if (diff != 0)
            return diff;
        
        diff = ComparatorUtils.safeCompare(prop1.getType(), prop2.getType());

        return diff;
    }

}
