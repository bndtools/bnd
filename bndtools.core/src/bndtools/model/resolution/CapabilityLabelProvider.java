package bndtools.model.resolution;

import java.util.Map.Entry;

import org.bndtools.core.ui.icons.Icons;
import org.bndtools.core.ui.resource.R5LabelFormatter;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import aQute.bnd.service.resource.SupportingResource;

public class CapabilityLabelProvider extends StyledCellLabelProvider {

	private final boolean shortenNamespaces;

	public CapabilityLabelProvider() {
		this(false);
	}

	public CapabilityLabelProvider(boolean shortenNamespaces) {
		this.shortenNamespaces = shortenNamespaces;
	}

	@Override
	public void update(ViewerCell cell) {
		Capability cap = (Capability) cell.getElement();

		StyledString label = new StyledString();
		R5LabelFormatter.appendCapability(label, cap, shortenNamespaces);
		cell.setText(label.toString());
		cell.setStyleRanges(label.getStyleRanges());

		// Get the icon from the capability namespace
		Image icon = Icons.image(R5LabelFormatter.getNamespaceImagePath(cap.getNamespace()));
		cell.setImage(icon);
	}

	@Override
	public String getToolTipText(Object element) {
		if (element instanceof Capability cap) {
			return tooltipText(cap);
		}

		return null;
	}

	static String tooltipText(Capability cap) {
		// caps tooltips become quite large because of the bnd.hashes and uses
		StringBuilder buf = new StringBuilder(400);

		Resource r = cap.getResource();

		buf.append("FROM: ")
			.append(r)
			.append("\n");

		buf.append(cap.getNamespace());

		if (r instanceof SupportingResource sr) {
			int index = sr.getSupportingIndex();
			if (index >= 0) {
				buf.append("Capability from a supporting resource ")
					.append(index)
					.append(" part of ")
					.append(sr.getParent())
					.append("\n");
			}
		}

		for (Entry<String, Object> attribute : cap.getAttributes()
			.entrySet())
			buf.append(";\n\t")
				.append(attribute.getKey())
				.append(" = ")
				.append(attribute.getValue());

		for (Entry<String, String> directive : cap.getDirectives()
			.entrySet())
			buf.append(";\n\t")
				.append(directive.getKey())
				.append(" := ")
				.append(directive.getValue());

		return buf.toString();
	}

}
