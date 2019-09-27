package org.bndtools.core.ui.resource;

import org.bndtools.utils.jface.ImageCachingLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.osgi.resource.Requirement;

import bndtools.Plugin;

public class RequirementLabelProvider extends ImageCachingLabelProvider {

	protected final boolean shortenNamespaces;

	public RequirementLabelProvider() {
		this(true);
	}

	public RequirementLabelProvider(boolean shortenNamespaces) {
		super(Plugin.PLUGIN_ID);
		this.shortenNamespaces = shortenNamespaces;
	}

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		if (element instanceof Requirement) {
			Requirement requirement = (Requirement) element;

			StyledString label = getLabel(requirement);

			cell.setText(label.getString());
			cell.setStyleRanges(label.getStyleRanges());

			Image icon = getImage(R5LabelFormatter.getNamespaceImagePath(requirement.getNamespace()), true);
			if (icon != null)
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
