package bndtools.utils;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

public class ResourceDeltaAccumulator implements IResourceDeltaVisitor {
	
	private final int acceptKinds;
	private final Collection<? super File> collector;
	private final Map<File, Integer> deltaKinds = new HashMap<File, Integer>();

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
			deltaKinds.put(file, delta.getKind());
			return false;
		}
		// Must be a container type, so return true in order to recurse in.
		return true;
	}
	/**
	 * Queries the kind of change which occurred on the specified file.
	 * 
	 * @param file
	 *            The kind of change (e.g. {@link IResourceDelta#ADDED},
	 *            {@link IResourceDelta#REMOVED} etc for the specified file, or
	 *            {@link IResourceDelta#NO_CHANGE} if the file was not affected
	 *            by the delta.
	 * @return
	 */
	public int queryDeltaKind(File file) {
		Integer kind = deltaKinds.get(file);
		return kind != null ? kind.intValue() : IResourceDelta.NO_CHANGE;
	}
}
