package org.bndtools.core.ui.resource;

import java.util.regex.Pattern;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

import aQute.bnd.osgi.resource.CapReqBuilder;

public class RequirementLabelProvider extends StyledCellLabelProvider {

	private static final Pattern FEATURE_PATTERN = Pattern.compile(".*type=org\\.eclipse\\.update\\.feature.*");

	protected final boolean shortenNamespaces;

	public RequirementLabelProvider() {
		this(true);
	}

	public RequirementLabelProvider(boolean shortenNamespaces) {
		this.shortenNamespaces = shortenNamespaces;
	}

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		if (element instanceof Requirement) {
			Requirement requirement = (Requirement) element;
			Requirement iconRequirement = requirement;
			try {
				iconRequirement = CapReqBuilder.unalias(requirement);
			} catch (Exception e) {
				// Use original requirement when unaliasing fails
			}

			StyledString label = getLabel(requirement);

			cell.setText(label.getString());
			cell.setStyleRanges(label.getStyleRanges());

			String namespace = iconRequirement.getNamespace();
			String filter = iconRequirement.getDirectives()
				.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);

			Image icon;
			if (IdentityNamespace.IDENTITY_NAMESPACE.equals(namespace)) {
				boolean isFeature = (filter != null) && FEATURE_PATTERN.matcher(filter)
					.matches();
				icon = Icons.image(isFeature ? "feature" : "bundle");
			} else {
				icon = Icons.image(R5LabelFormatter.getNamespaceImagePath(namespace));
			}
			cell.setImage(icon);
		}
	}

	protected StyledString getLabel(Requirement requirement) {
		StyledString label = new StyledString();
		return getLabel(label, requirement);
	}

	protected StyledString getLabel(StyledString label, Requirement requirement) {
		R5LabelFormatter.appendRequirementLabel(label, requirement, shortenNamespaces);
		return label;
	}
}
