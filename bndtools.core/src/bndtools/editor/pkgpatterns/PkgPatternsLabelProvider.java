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
package bndtools.editor.pkgpatterns;


import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.UIConstants;
import bndtools.model.clauses.HeaderClause;

import aQute.lib.osgi.Constants;

public class PkgPatternsLabelProvider extends StyledCellLabelProvider {
	
	private final Image packageImg;
	
	public PkgPatternsLabelProvider() {
		packageImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_obj.gif").createImage();
	}
	@Override
	public void update(ViewerCell cell) {
		HeaderClause clause = (HeaderClause) cell.getElement();
		cell.setImage(packageImg);
		
		StyledString styledString = new StyledString(clause.getName());
		String resolution = clause.getAttribs().get(Constants.RESOLUTION_DIRECTIVE);
		if(org.osgi.framework.Constants.RESOLUTION_OPTIONAL.equals(resolution)) {
			styledString.append(" <optional>", UIConstants.ITALIC_QUALIFIER_STYLER);
		}
		String version = clause.getAttribs().get(org.osgi.framework.Constants.VERSION_ATTRIBUTE);
		if(version != null) {
			styledString.append(": " + version, StyledString.COUNTER_STYLER);
		}
		cell.setText(styledString.getString());
		cell.setStyleRanges(styledString.getStyleRanges());
	}
	@Override
	public void dispose() {
		super.dispose();
		packageImg.dispose();
	}
}
