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
package bndtools.internal.pkgselection;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class PackageNameLabelProvider extends LabelProvider {

    private final Image packageImg = Icons.desc("package").createImage();

    @Override
    public String getText(Object element) {
        String result;
        if (element instanceof String)
            result = (String) element;
        else if (element instanceof IPath)
            result = ((IPath) element).toString().replace('/', '.');
        else
            result = "<error>";
        return result;
    }

    @Override
    public Image getImage(Object element) {
        return packageImg;
    }

    @Override
    public void dispose() {
        super.dispose();
        packageImg.dispose();
    }
}
