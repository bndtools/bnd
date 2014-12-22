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
package bndtools.utils;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class JavaContentProposalLabelProvider extends LabelProvider {

    private final Image classImg = Icons.desc("class").createImage();
    private final Image interfaceImg = Icons.desc("interface").createImage();

    @Override
    public Image getImage(Object element) {
        Image result = null;

        if (element instanceof JavaContentProposal) {
            result = classImg;
            if (((JavaContentProposal) element).isInterface()) {
                result = interfaceImg;
            } else {
                result = classImg;
            }
        }

        return result;
    }

    @Override
    public String getText(Object element) {
        IContentProposal proposal = (IContentProposal) element;

        return proposal.getLabel();
    }

    @Override
    public void dispose() {
        super.dispose();
        classImg.dispose();
        interfaceImg.dispose();
    }
}