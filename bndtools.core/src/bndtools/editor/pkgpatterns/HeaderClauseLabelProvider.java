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

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import aQute.bnd.build.model.clauses.HeaderClause;

public abstract class HeaderClauseLabelProvider<C extends HeaderClause> extends StyledCellLabelProvider {

    private final Image packageImg;

    public HeaderClauseLabelProvider() {
        packageImg = Icons.desc("package").createImage();
    }

    @Override
    public void update(ViewerCell cell) {
        @SuppressWarnings("unchecked")
        C clause = (C) cell.getElement();

        cell.setImage(packageImg);

        StyledString styledString = new StyledString(clause.getName());
        String version = clause.getAttribs().get(org.osgi.framework.Constants.VERSION_ATTRIBUTE);
        if (version != null) {
            styledString.append(": " + version, StyledString.COUNTER_STYLER);
        }

        decorate(styledString, clause);

        cell.setText(styledString.getString());
        cell.setStyleRanges(styledString.getStyleRanges());
    }

    @Override
    public void dispose() {
        super.dispose();
        packageImg.dispose();
    }

    @SuppressWarnings("unused")
    protected void decorate(StyledString label, C clause) {}
}
