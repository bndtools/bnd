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

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.fieldassist.IContentProposal;

public class PkgPatternProposal implements IContentProposal {

    private final IPackageFragment pkg;
    private final boolean wildcard;

    private final int replaceFromPos;

    public PkgPatternProposal(IPackageFragment pkg, boolean wildcard, int replaceFromPos) {
        this.pkg = pkg;
        this.wildcard = wildcard;

        this.replaceFromPos = replaceFromPos;
    }

    public String getContent() {
        String content = pkg.getElementName();
        if (wildcard)
            content += "*";
        return content;
    }

    public int getCursorPosition() {
        int length = pkg.getElementName().length();
        if (wildcard)
            length++;
        return length + replaceFromPos;
    }

    public String getDescription() {
        return null;
    }

    public String getLabel() {
        return getContent();
    }

    public IPackageFragment getPackageFragment() {
        return pkg;
    }

    public boolean isWildcard() {
        return wildcard;
    }

    public int getReplaceFromPos() {
        return replaceFromPos;
    }
}