/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
/**
 * 
 */
package name.neilbartlett.eclipse.bndtools.frameworks.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.frameworks.OSGiSpecLevel;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class OSGiFrameworkLabelProvider extends StyledCellLabelProvider {
	
	private final Device device;
	private final Map<OSGiSpecLevel, List<IFrameworkInstance>> specMappings;
	
	private final Image bookImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/book.png").createImage();
	
	private final List<Image> images = new ArrayList<Image>();
	private final ImageDescriptor errorOverlay = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/error_overlay.gif");
	private final ImageDescriptor warningOverlay = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/warning_overlay.gif");

	public OSGiFrameworkLabelProvider(Device device, Map<OSGiSpecLevel, List<IFrameworkInstance>> specMappings) {
		this.device = device;
		this.specMappings = specMappings;
	}
	
	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		if(element instanceof OSGiSpecLevel) {
			OSGiSpecLevel specLevel = (OSGiSpecLevel) element;
			boolean enabled = specMappings == null || specMappings.containsKey(specLevel);
			
			if(enabled) {
				cell.setText(specLevel.getFormattedName());
			} else {
				StyledString styledString = new StyledString(specLevel.getFormattedName(), StyledString.QUALIFIER_STYLER);
				cell.setText(styledString.getString());
				cell.setStyleRanges(styledString.getStyleRanges());
			}
			cell.setImage(bookImg);
		} else if(element instanceof IFrameworkInstance) {
			Image icon;
			IFrameworkInstance instance = (IFrameworkInstance) cell.getElement();
			icon = instance.createIcon(device);
			images.add(icon);
			
			IStatus instanceStatus = instance.getStatus();
			if(instanceStatus.getSeverity() == IStatus.ERROR) {
				DecorationOverlayIcon overlayIcon = new DecorationOverlayIcon(icon, errorOverlay, IDecoration.BOTTOM_RIGHT);
				icon = overlayIcon.createImage(device);
				images.add(icon);
			} else if(instanceStatus.getSeverity() == IStatus.WARNING) {
				DecorationOverlayIcon overlayIcon = new DecorationOverlayIcon(icon, warningOverlay, IDecoration.BOTTOM_RIGHT);
				icon = overlayIcon.createImage(device);
				images.add(icon);
			}
			if(icon != null) cell.setImage(icon);
			
			StyledString string = new StyledString(instance.getDisplayString());
			string.append(" " + instance.getInstancePath().toString(), StyledString.DECORATIONS_STYLER);
			cell.setText(string.getString());
			cell.setStyleRanges(string.getStyleRanges());
		}
	}
	@Override
	public void dispose() {
		super.dispose();
		bookImg.dispose();
		for (Image image : images) {
			image.dispose();
		}
	}
}