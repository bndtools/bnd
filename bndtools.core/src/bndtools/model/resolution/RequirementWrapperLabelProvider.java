package bndtools.model.resolution;

import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.bndtools.core.ui.icons.Icons;
import org.bndtools.core.ui.resource.R5LabelFormatter;
import org.bndtools.core.ui.resource.RequirementLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.Clazz;
import aQute.bnd.service.resource.SupportingResource;

public class RequirementWrapperLabelProvider extends RequirementLabelProvider {

	private final Styler resolved = StyledString.QUALIFIER_STYLER;
	private static final Pattern FEATURE_PATTERN = Pattern.compile(".*type=org\\.eclipse\\.update\\.feature.*");

	public RequirementWrapperLabelProvider(boolean shortenNamespaces) {
		super(shortenNamespaces);
	}

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		if (element instanceof RequirementWrapper) {
			RequirementWrapper rw = (RequirementWrapper) element;

			// Check if this is an osgi.identity requirement (bundle or feature)
			String namespace = rw.requirement.getNamespace();
			String filter = rw.requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
			boolean isOsgiIdentity = IdentityNamespace.IDENTITY_NAMESPACE.equals(namespace);
			boolean isEclipseFeature = isOsgiIdentity 
				&& filter != null 
				&& FEATURE_PATTERN.matcher(filter).matches();
			
			Image icon;
			if (isEclipseFeature) {
				icon = Icons.image("feature");
			} else if (isOsgiIdentity) {
				// Bundle (osgi.identity without feature type)
				icon = Icons.image("bundle");
			} else {
				icon = Icons.image(R5LabelFormatter.getNamespaceImagePath(namespace));
			}
			cell.setImage(icon);

			StyledString label = getLabel(rw.requirement);
			if (rw.resolved || rw.java)
				label.setStyle(0, label.length(), resolved);

			cell.setText(label.getString());
			cell.setStyleRanges(label.getStyleRanges());
		} else if (element instanceof Clazz) {
			cell.setImage(Icons.image("class", false));

			String pkg;
			String className;

			String fqn = ((Clazz) element).getFQN();
			int dot = fqn.lastIndexOf('.');
			if (dot >= 0) {
				pkg = fqn.substring(0, dot);
				className = fqn.substring(dot + 1);
			} else {
				pkg = "<default package>";
				className = fqn;
			}

			StyledString label = new StyledString(className);
			label.append(" - " + pkg, StyledString.QUALIFIER_STYLER);

			cell.setText(label.getString());
			cell.setStyleRanges(label.getStyleRanges());
		}
	}

	@Override
	public String getToolTipText(Object element) {
		if (element instanceof RequirementWrapper rw) {
			return tooltipText(rw);
		}

		return null;
	}


	static String tooltipText(RequirementWrapper rw) {
		Requirement req = rw.requirement;

		StringBuilder buf = new StringBuilder(300);
		if (rw.resolved)
			buf.append("RESOLVED:\n");
		if (rw.java)
			buf.append("JAVA:\n");

		Resource r = req.getResource();

		buf.append("FROM: ")
			.append(r)
			.append("\n");

		if (r instanceof SupportingResource sr) {
			int index = sr.getSupportingIndex();
			if (index >= 0) {
				buf.append("Requirement from a supporting resource ")
					.append(index)
					.append(" part of ")
					.append(sr.getParent())
					.append("\n");
			}
		}
		buf.append(req.getNamespace());

		for (Entry<String, Object> attr : req.getAttributes()
			.entrySet())
			buf.append(";\n\t")
				.append(attr.getKey())
				.append(" = ")
				.append(attr.getValue());

		for (Entry<String, String> directive : req.getDirectives()
			.entrySet())
			buf.append(";\n\t")
				.append(directive.getKey())
				.append(" := ")
				.append(directive.getValue());

		return buf.toString();
	}

}
