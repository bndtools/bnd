package org.bndtools.core.ui.wizards.shared;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bndtools.templating.Category;
import org.bndtools.templating.Template;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class LatestTemplateFilter extends ViewerFilter {

    @Override
    public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
        Object[] result;
        if (parent instanceof Category) {
            Map<String,Template> selected = new LinkedHashMap<>(); // Preserves the order of names, as they were already sorted by the content provider.
            for (Object element : elements) {
                Template template = (Template) element;
                Template existing = selected.get(template.getName());

                if (existing == null)
                    // no selected template for this name -> add
                    selected.put(template.getName(), template);

                else if (template.getVersion().compareTo(existing.getVersion()) > 0)
                    // existing selected template for this name is lower -> replace
                    selected.put(template.getName(), template);
            }
            result = selected.values().toArray(new Object[selected.size()]);
        } else {
            result = elements;
        }
        return result;
    }

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        // not invoked
        throw new UnsupportedOperationException();
    }

}
