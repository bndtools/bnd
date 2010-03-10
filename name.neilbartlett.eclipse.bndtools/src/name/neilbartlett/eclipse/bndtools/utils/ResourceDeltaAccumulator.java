package name.neilbartlett.eclipse.bndtools.utils;

import java.io.File;
import java.util.Collection;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

public class ResourceDeltaAccumulator implements IResourceDeltaVisitor {
	
	private final int acceptKinds;
	private final Collection<? super File> collector;

	public ResourceDeltaAccumulator(int acceptKinds, Collection<? super File> collector) {
		this.acceptKinds = acceptKinds;
		this.collector = collector;
	}
	public boolean visit(IResourceDelta delta) throws CoreException {
		if((acceptKinds & delta.getKind()) == 0)
			return false;
		IResource resource = delta.getResource();
		if(resource.getType() == IResource.FILE) {
			File file = resource.getLocation().toFile();
			collector.add(file);
			return false;
		}
		// Must be a container type, so return true in order to recurse in.
		return true;
	}
}
