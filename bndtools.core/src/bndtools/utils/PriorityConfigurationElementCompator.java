package bndtools.utils;

import java.util.Comparator;

import org.eclipse.core.runtime.IConfigurationElement;

public class PriorityConfigurationElementCompator implements Comparator<IConfigurationElement> {

    private final boolean invert;

    public PriorityConfigurationElementCompator(boolean invert) {
        this.invert = invert;
    }

    public int compare(IConfigurationElement o1, IConfigurationElement o2) {
        int result = getPriority(o1) - getPriority(o2);
        if (invert)
            result *= -1;

        return result;
    }

    private int getPriority(IConfigurationElement elem) {
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
