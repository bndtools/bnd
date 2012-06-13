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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class PkgPatternProposalLabelProvider extends LabelProvider {

    private Image singleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_obj.gif").createImage();
    private Image multiImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/packages.gif").createImage();

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
