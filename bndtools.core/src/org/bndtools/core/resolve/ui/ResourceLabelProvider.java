package org.bndtools.core.resolve.ui;

import java.net.URI;

import org.bndtools.core.ui.icons.Icons;
import org.bndtools.utils.resources.ResourceUtils;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

public class ResourceLabelProvider extends StyledCellLabelProvider {

	private final Image bundleImg = Icons.desc("bundle")
		.createImage();

	@Override
	public void update(ViewerCell cell) {
		Resource resource = (Resource) cell.getElement();

		StyledString label = new StyledString();
		try {
			Capability identityCap = ResourceUtils.getIdentityCapability(resource);

			String name = ResourceUtils.getIdentity(identityCap);
			label.append(name);

			Version version = ResourceUtils.getVersion(identityCap);
			label.append(" " + version, StyledString.COUNTER_STYLER);
		} catch (IllegalArgumentException e) {
			label.append("<unknown>");
		}

		try {
			URI uri = ResourceUtils.getURI(ResourceUtils.getContentCapability(resource));
			label.append(" [" + uri + "]", StyledString.QUALIFIER_STYLER);
		} catch (IllegalArgumentException e) {
			// ignore
		}

		cell.setText(label.getString());
		cell.setStyleRanges(label.getStyleRanges());
		cell.setImage(bundleImg);
	}

	@Override
	public void dispose() {
		super.dispose();
		bundleImg.dispose();
	}
}
