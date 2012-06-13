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

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

public class JavaTypeContentProposal extends JavaContentProposal {

    private final IType element;

    public JavaTypeContentProposal(IType element) throws JavaModelException {
        super(element.getPackageFragment().getElementName(), element.getElementName(), element.isInterface());
        this.element = element;
    }

    public IType getType() {
        return element;
    }
}
