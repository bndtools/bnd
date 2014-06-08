package org.bndtools.utils.eclipse;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class CategorisedPrioritisedConfigurationElementTreeContentProvider implements ITreeContentProvider {

    private final SortedMap<ConfigurationElementCategory,List<IConfigurationElement>> data = new TreeMap<ConfigurationElementCategory,List<IConfigurationElement>>();
    private final boolean flattenSingleCategory;

    public CategorisedPrioritisedConfigurationElementTreeContentProvider(boolean flattenSingleCategory) {
        this.flattenSingleCategory = flattenSingleCategory;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        data.clear();

        // Dump input into categories
        if (newInput instanceof IConfigurationElement[]) {
            IConfigurationElement[] array = (IConfigurationElement[]) newInput;
            for (IConfigurationElement element : array)
                categorise(element);
        } else if (newInput instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<IConfigurationElement> coll = (Collection<IConfigurationElement>) newInput;
            for (IConfigurationElement element : coll)
                categorise(element);
        }

        // Sort within each category
        CategorisedConfigurationElementComparator comparator = new CategorisedConfigurationElementComparator(true);
        for (Entry<ConfigurationElementCategory,List<IConfigurationElement>> entry : data.entrySet()) {
            List<IConfigurationElement> list = entry.getValue();
            Collections.sort(list, comparator);
        }
    }

    private void categorise(IConfigurationElement element) {
        ConfigurationElementCategory category = ConfigurationElementCategory.parse(element.getAttribute("category"));
        List<IConfigurationElement> list = data.get(category);
        if (list == null) {
            list = new LinkedList<IConfigurationElement>();
            data.put(category, list);
        }
        list.add(element);
    }

    @Override
    public Object[] getElements(Object inputElement) {
        Object[] result;

        Set<ConfigurationElementCategory> keys = data.keySet();
        if (keys.size() == 1 && flattenSingleCategory) {
            List<IConfigurationElement> elements = data.get(keys.iterator().next());
            result = elements.toArray(new Object[elements.size()]);
        } else {
            result = keys.toArray(new Object[keys.size()]);
        }
        return result;
    }

    @Override
    public void dispose() {}

    @Override
    public Object[] getChildren(Object parentElement) {
        List<IConfigurationElement> list = data.get(parentElement);
        return list.toArray(new IConfigurationElement[list.size()]);
    }

    @Override
    public Object getParent(Object element) {
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        boolean result = false;
        if (element instanceof ConfigurationElementCategory) {
            List<IConfigurationElement> list = data.get(element);
            result = !list.isEmpty();
        }
        return result;
    }
}
