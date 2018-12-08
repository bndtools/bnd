package org.bndtools.utils.eclipse;

import java.util.Comparator;

import org.eclipse.core.runtime.IConfigurationElement;

public class CategorisedConfigurationElementComparator implements Comparator<IConfigurationElement> {

    private final boolean descending;

    public CategorisedConfigurationElementComparator(boolean descending) {
        this.descending = descending;
    }

    @Override
    public int compare(IConfigurationElement e1, IConfigurationElement e2) {
        ConfigurationElementCategory c1 = ConfigurationElementCategory.parse(e1.getAttribute("category"));
        ConfigurationElementCategory c2 = ConfigurationElementCategory.parse(e2.getAttribute("category"));

        int diff = c1.compareTo(c2);
        if (diff == 0) {
            int p1 = getPriority(e1);
            int p2 = getPriority(e2);

            diff = p1 - p2;
            if (descending)
                diff *= -1;
        }

        return diff;
    }

    private static int getPriority(IConfigurationElement elem) {
        String string = elem.getAttribute("priority");
        if (string == null)
            string = "0";

        int number;
        try {
            number = Integer.parseInt(string);
        } catch (NumberFormatException e) {
            number = 0;
        }
        return number;
    }

}
