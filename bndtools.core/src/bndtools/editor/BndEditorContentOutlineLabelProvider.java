/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package bndtools.editor;


import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.editor.model.ServiceComponent;
import bndtools.model.clauses.ExportedPackage;
import bndtools.model.clauses.ImportPattern;

public class BndEditorContentOutlineLabelProvider extends StyledCellLabelProvider {

	final Image pageImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/page_white_text.png").createImage();
	final Image packageImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_obj.gif").createImage();
	final Image brickImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();

		if(element instanceof String) {
			// Top-level placeholder
			if(BndEditor.BUILD_PAGE.equals(element)) {
				cell.setText("Build");
			} else if(BndEditor.PROJECT_RUN_PAGE.equals(element)) {
			    cell.setText("Run");
			} else if(BndEditor.COMPONENTS_PAGE.equals(element)) {
				cell.setText("Components");
			} else if(BndEditorContentOutlineProvider.EXPORTS.equals(element)) {
				cell.setText("Exports");
			} else if(BndEditorContentOutlineProvider.PRIVATE_PKGS.equals(element)) {
			    cell.setText("Private Packages");
			} else if(BndEditor.IMPORTS_PAGE.equals(element)) {
				cell.setText("Imports");
			} else if(BndEditor.SOURCE_PAGE.equals(element)) {
				cell.setText("Source");
			}
			cell.setImage(pageImg);
		} else if(element instanceof ServiceComponent) {
			ServiceComponent component = (ServiceComponent) element;
			cell.setText(component.getName());
			cell.setImage(brickImg);
		} else if(element instanceof ExportedPackage) {
			cell.setText(((ExportedPackage) element).getName());
			cell.setImage(packageImg);
		} else if(element instanceof ImportPattern) {
			cell.setText(((ImportPattern) element).getName());
			cell.setImage(packageImg);
		} else if(element instanceof PrivatePkg) {
		    cell.setText(((PrivatePkg) element).pkg);
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		pageImg.dispose();
		packageImg.dispose();
		brickImg.dispose();
	}
}
