package org.bndtools.builder;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;

public class CompositeResourceProxyVisitor implements IResourceProxyVisitor {

	private final List<IResourceProxyVisitor> delegateList = new LinkedList<>();

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
