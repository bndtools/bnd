package bndtools.editor.workspace;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class PluginClassSorter extends ViewerSorter {

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        IConfigurationElement elem1 = (IConfigurationElement) e1;
        IConfigurationElement elem2 = (IConfigurationElement) e2;
        
        // Sort undeprecated plugins before deprecated ones.
        int result = sortDeprecation(elem1, elem2);
        if (result != 0)
            return result;
        
        // Sort by rank
        result = sortByRank(elem1, elem2);
        if (result != 0)
            return result;
        
        // Finally sort on name
        return sortName(elem1, elem2);
    }

    private int sortDeprecation(IConfigurationElement elem1, IConfigurationElement elem2) {
        if (isDeprecated(elem1))
            return isDeprecated(elem2) ? 0 : 1;
        return isDeprecated(elem2) ? -1 : 0;
    }

    private boolean isDeprecated(IConfigurationElement elem) {
        return elem.getAttribute("deprecated") != null;
    }
    
    private int sortByRank(IConfigurationElement elem1, IConfigurationElement elem2) {
        int r1 = getRank(elem1);
        int r2 = getRank(elem2);
        return r2 - r1;
    }

    private int getRank(IConfigurationElement elem1) {
        String rankStr = elem1.getAttribute("rank");
        int rank = 0;
        try {
            rank = Integer.parseInt(rankStr);
        } catch (NumberFormatException e) {
            // ignore
        }
        return rank;
    }

    private int sortName(IConfigurationElement elem1, IConfigurationElement elem2) {
        String name1 = elem1.getAttribute("name");
        String name2 = elem2.getAttribute("name");
        
        return name1.compareTo(name2);
    }

}
