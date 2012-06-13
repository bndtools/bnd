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
package bndtools.internal.testcaseselection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class TestCaseLabelProvider extends LabelProvider {

    private Image junitImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/test.gif").createImage();

    @Override
    public String getText(Object element) {
        String result;
        if (element instanceof String)
            result = (String) element;
        else if (element instanceof IPath)
            result = ((IPath) element).toString().replace('/', '.');
        else
            result = "<error>"; //$NON-NLS-1$
        return result;
    }

    @Override
    public Image getImage(Object element) {
        return junitImg;
    }

    @Override
    public void dispose() {
        super.dispose();
        junitImg.dispose();
    }
}
