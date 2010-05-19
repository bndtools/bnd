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
package bndtools.views.impexp;

import static bndtools.views.impexp.ImportsExportsTreeContentProvider.EXPORTS_PLACEHOLDER;
import static bndtools.views.impexp.ImportsExportsTreeContentProvider.IMPORTS_PLACEHOLDER;

import java.util.Map;
import java.util.Map.Entry;


import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.UIConstants;
import bndtools.editor.model.HeaderClause;
import bndtools.tasks.analyse.ImportPackage;
import bndtools.views.impexp.ImportsExportsTreeContentProvider.ExportUsesPackage;
import bndtools.views.impexp.ImportsExportsTreeContentProvider.ImportUsedByClass;
import bndtools.views.impexp.ImportsExportsTreeContentProvider.ImportUsedByPackage;

import aQute.lib.osgi.Constants;

public class ImportsExportsTreeLabelProvider extends StyledCellLabelProvider {
	
	private final Image pkgFolderImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/packagefolder_obj.gif").createImage();
	private final Image classImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/class_obj.gif").createImage();
	
	private final ImageDescriptor packageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_obj.gif");
	private final Image packageImg = packageDescriptor.createImage();
	
//	private final ImageDescriptor questionOverlay = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/question_overlay.gif");
//	private final Image packageOptImg = new DecorationOverlayIcon(packageImg, questionOverlay, IDecoration.TOP_LEFT).createImage();
	private final Image packageOptImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_opt.gif").createImage();
	private final Image packageImpExpImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_impexp.gif").createImage();
	
	public ImportsExportsTreeLabelProvider() {
	}
	
	public void dispose() {
		super.dispose();
		pkgFolderImg.dispose();
		classImg.dispose();
		packageImg.dispose();
		packageOptImg.dispose();
		packageImpExpImg.dispose();
	};
	
	@Override
	public void update(ViewerCell cell) {
		if(cell.getElement() == IMPORTS_PLACEHOLDER) {
			if(cell.getColumnIndex() == 0) {
				cell.setImage(pkgFolderImg);
				cell.setText("Import Packages");
			}
		} else if(cell.getElement() == EXPORTS_PLACEHOLDER) {
			if(cell.getColumnIndex() == 0) {
				cell.setImage(pkgFolderImg);
				cell.setText("Export Packages");
			}
		} else if(cell.getElement() instanceof ImportUsedByPackage) {
			if(cell.getColumnIndex() == 0) {
				StyledString styledString = new StyledString("Used By: ", UIConstants.ITALIC_QUALIFIER_STYLER);
				styledString.append(((ImportUsedByPackage) cell.getElement()).usedByName);
				cell.setText(styledString.getString());
				cell.setStyleRanges(styledString.getStyleRanges());
			}
		} else if(cell.getElement() instanceof ImportUsedByClass) {
			if(cell.getColumnIndex() == 0) {
				ImportUsedByClass importUsedBy = (ImportUsedByClass) cell.getElement();
				String fqn = importUsedBy.clazz.getFQN();
				String className = fqn.substring(fqn.lastIndexOf('.') + 1);
				cell.setText(className);
				cell.setImage(classImg);
			}
		} else if(cell.getElement() instanceof ExportUsesPackage) {
			if(cell.getColumnIndex() == 0) {
				StyledString styledString = new StyledString("Uses: ", UIConstants.ITALIC_QUALIFIER_STYLER);
				styledString.append(((ExportUsesPackage) cell.getElement()).name);
				cell.setText(styledString.getString());
				cell.setStyleRanges(styledString.getStyleRanges());
			}
		} else if(cell.getElement() instanceof HeaderClause) {
			HeaderClause entry = (HeaderClause) cell.getElement();
			switch(cell.getColumnIndex()) {
			case 0:
				boolean selfImport = false;
				if(entry instanceof ImportPackage) {
					selfImport =  ((ImportPackage) entry).isSelfImport();
				}
				
				StyledString styledString;
				if(selfImport) {
					styledString = new StyledString(entry.getName(), StyledString.QUALIFIER_STYLER);
				} else {
					styledString = new StyledString(entry.getName());
				}
				
				String version = entry.getAttribs().get(Constants.VERSION_ATTRIBUTE);
				if(version != null)
					styledString.append(" " + version, StyledString.COUNTER_STYLER);
				
				String resolution = entry.getAttribs().get(Constants.RESOLUTION_DIRECTIVE);
				boolean optional = org.osgi.framework.Constants.RESOLUTION_OPTIONAL.equals(resolution);
				if(resolution != null)
					styledString.append(" <" + resolution + ">", UIConstants.ITALIC_QUALIFIER_STYLER);
				
				cell.setText(styledString.getString());
				cell.setStyleRanges(styledString.getStyleRanges());
				if(optional) {
					cell.setImage(packageOptImg);
				} else if(selfImport) {
					cell.setImage(packageImpExpImg);
				} else {
					cell.setImage(packageImg);
				}
				break;
			case 1:
				// Show the attributes excluding "resolution:", "version" and "uses:"
				Map<String, String> attribs = entry.getAttribs();
				StringBuilder builder = new StringBuilder();
				boolean first = true;
				for (Entry<String,String> attribEntry : attribs.entrySet()) {
					if(!first) builder.append(';');
					if(!Constants.VERSION_ATTRIBUTE.equals(attribEntry.getKey())
							&& !Constants.RESOLUTION_DIRECTIVE.equals(attribEntry.getKey())
							&& !Constants.USES_DIRECTIVE.equals(attribEntry.getKey())) {
						builder.append(attribEntry.getKey()).append('=').append(attribEntry.getValue());
					}
				}
				cell.setText(builder.toString());
				break;
			}
		}
	}
}
