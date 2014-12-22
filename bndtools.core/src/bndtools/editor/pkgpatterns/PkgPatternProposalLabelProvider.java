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
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class PkgPatternProposalLabelProvider extends LabelProvider {

    private final Image singleImg = Icons.desc("package").createImage();
    private final Image multiImg = Icons.desc("packages").createImage();

    @Override
    public String getText(Object element) {
        return ((PkgPatternProposal) element).getLabel();
    }

    @Override
    public Image getImage(Object element) {
        boolean wildcard = ((PkgPatternProposal) element).isWildcard();
        return wildcard ? multiImg : singleImg;
    }

    @Override
    public void dispose() {
        super.dispose();
        singleImg.dispose();
        multiImg.dispose();
    }
}
