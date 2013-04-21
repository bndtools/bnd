package org.bndtools.core.utils.eclipse;

import java.io.Serializable;
import java.util.Comparator;

import org.eclipse.core.runtime.IConfigurationElement;

public class PriorityConfigurationElementComparator implements Comparator<IConfigurationElement>, Serializable {
    private static final long serialVersionUID = -6344234922378864566L;

    private final boolean descending;

    public PriorityConfigurationElementComparator(boolean descending) {
        this.descending = descending;
    }

    public int compare(IConfigurationElement o1, IConfigurationElement o2) {
        int result = getPriority(o1) - getPriority(o2);
        if (descending)
            result *= -1;

        return result;
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
