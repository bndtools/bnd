package org.bndtools.core.ui.resource;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.core.utils.resources.ResourceUtils;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

import bndtools.UIConstants;

public class RequirementLabelProvider extends AbstractR5LabelProvider {

    private final ConcurrentMap<String,Pattern> namespacePatterns = new ConcurrentHashMap<String,Pattern>();

    private void applyStyle(String name, Styler styler, StyledString label) {
        Matcher matcher = getFilterPattern(name).matcher(label.getString());
        while (matcher.find()) {
            int begin = matcher.start(1);
            int end = matcher.end(1);
            label.setStyle(begin, end - begin, styler);
        }
    }

    private Pattern getFilterPattern(String name) {
        Pattern pattern = namespacePatterns.get(name);
        if (pattern == null) {
            pattern = Pattern.compile("\\(" + name + "[<>]?=([^\\)]*)\\)");
            Pattern existing = namespacePatterns.putIfAbsent(name, pattern);
            if (existing != null)
                pattern = existing;
        }
        return pattern;
    }

    public StyledString getLabel(Requirement requirement) {
        StyledString label;

        String filter = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
        if (filter == null) {
            // not a proper requirement... maybe a substitution?
            label = new StyledString("[unparsed]", StyledString.QUALIFIER_STYLER);
        } else {
            label = new StyledString(filter, StyledString.QUALIFIER_STYLER);
            String namespace = requirement.getNamespace();
            if (namespace != null) {
                applyStyle(namespace, UIConstants.BOLD_STYLER, label);

                String versionAttrib = ResourceUtils.getVersionAttributeForNamespace(namespace);
                if (versionAttrib != null)
                    applyStyle(versionAttrib, UIConstants.BOLD_COUNTER_STYLER, label);
            }
        }

        return label;
    }

    @Override
    public void update(ViewerCell cell) {
        Object element = cell.getElement();
        if (element instanceof Requirement) {
            Requirement requirement = (Requirement) element;

            StyledString label = getLabel(requirement);

            cell.setText(label.getString());
            cell.setStyleRanges(label.getStyleRanges());

            cell.setImage(getIcon(requirement.getNamespace()));
        }
    }

}
