package name.neilbartlett.eclipse.bndtools.builder;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

public class CompositeResourceDeltaVisitor implements IResourceDeltaVisitor {
	
	private static final List<IResourceDeltaVisitor> delegates = new LinkedList<IResourceDeltaVisitor>();
	
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
