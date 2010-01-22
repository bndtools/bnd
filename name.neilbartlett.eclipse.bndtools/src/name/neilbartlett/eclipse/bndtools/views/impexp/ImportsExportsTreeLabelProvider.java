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
package name.neilbartlett.eclipse.bndtools.views.impexp;

import static name.neilbartlett.eclipse.bndtools.views.impexp.ImportsExportsTreeContentProvider.EXPORTS_PLACEHOLDER;
import static name.neilbartlett.eclipse.bndtools.views.impexp.ImportsExportsTreeContentProvider.IMPORTS_PLACEHOLDER;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.UIConstants;
import name.neilbartlett.eclipse.bndtools.editor.model.HeaderClause;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.lib.osgi.Constants;

public class ImportsExportsTreeLabelProvider extends StyledCellLabelProvider {
	
	private final Image pkgFolderImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/packagefolder_obj.gif").createImage();
	
	private final Image packageImg;
	private final Image packageOptImg;
	
	public ImportsExportsTreeLabelProvider() {
		ImageDescriptor packageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_obj.gif");
		packageImg = packageDescriptor.createImage();
		
		ImageDescriptor questionOverlay = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/question_overlay.gif");
		packageOptImg = new DecorationOverlayIcon(packageImg, questionOverlay, IDecoration.TOP_LEFT).createImage();
	}
	
	public void dispose() {
		super.dispose();
		pkgFolderImg.dispose();
		packageImg.dispose();
		packageOptImg.dispose();
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
		} else {
			@SuppressWarnings("unchecked")
			HeaderClause entry = (HeaderClause) cell.getElement();
			switch(cell.getColumnIndex()) {
			case 0:
				StyledString styledString = new StyledString(entry.getName());
				String resolution = entry.getAttribs().get(Constants.RESOLUTION_DIRECTIVE);
				if(resolution != null)
					styledString.append(" <" + resolution + ">", UIConstants.ITALIC_QUALIFIER_STYLER);
				cell.setText(styledString.getString());
				cell.setStyleRanges(styledString.getStyleRanges());
				cell.setImage(packageImg);
				break;
			case 1:
				cell.setText(entry.getAttribs().get(Constants.VERSION_ATTRIBUTE));
				break;
			case 2:
				Collection<? extends String> uses = null;
				if(entry instanceof ImportPackage) {
					uses = ((ImportPackage) entry).getUsedBy();
				} else if(entry instanceof ExportPackage) {
					uses = ((ExportPackage) entry).getUses();
				}
				if(uses != null) {
					StringBuilder builder = new StringBuilder();
					for(Iterator<? extends String> iter = uses.iterator(); iter.hasNext(); ) {
						builder.append(iter.next());
						if(iter.hasNext())
							builder.append(',');
					}
					cell.setText(builder.toString());
				}
				break;
			case 3:
				// Show the attributes excluding "resolution:" and "version"
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
