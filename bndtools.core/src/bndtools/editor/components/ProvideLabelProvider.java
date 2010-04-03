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
package bndtools.editor.components;


import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class ProvideLabelProvider extends LabelProvider {
	
	private Image interfaceImage = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/interface_obj.gif").createImage();
	
	@Override
	public Image getImage(Object element) {
		return interfaceImage;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		interfaceImage.dispose();
	}
}
