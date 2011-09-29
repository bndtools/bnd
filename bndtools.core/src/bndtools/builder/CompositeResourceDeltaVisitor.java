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
package bndtools.builder;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

public class CompositeResourceDeltaVisitor implements IResourceDeltaVisitor {
	
	private final List<IResourceDeltaVisitor> delegates = new LinkedList<IResourceDeltaVisitor>();
	
	public void addDelegate(IResourceDeltaVisitor delegate) {
		this.delegates.add(delegate);
	}
	public void removeDelegate(IResourceDeltaVisitor delegate) {
		this.delegates.remove(delegate);
	}

	public boolean visit(IResourceDelta delta) throws CoreException {
		boolean recurse = false;
		
		for (IResourceDeltaVisitor delegate : delegates) {
			recurse = delegate.visit(delta) || recurse;
		}
		
		return recurse;
	}

}
