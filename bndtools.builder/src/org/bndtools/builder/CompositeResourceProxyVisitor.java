/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package org.bndtools.builder;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;

public class CompositeResourceProxyVisitor implements IResourceProxyVisitor {

    private final List<IResourceProxyVisitor> delegateList = new LinkedList<IResourceProxyVisitor>();

    public void addDelegate(IResourceProxyVisitor delegate) {
        delegateList.add(delegate);
    }

    public void removeDelegate(IResourceProxyVisitor delegate) {
        delegateList.remove(delegate);
    }

    @Override
    public boolean visit(IResourceProxy proxy) throws CoreException {
        boolean recurse = false;

        for (IResourceProxyVisitor delegate : delegateList) {
            recurse = delegate.visit(proxy) || recurse;
        }

        return recurse;
    }

}
