package bndtools.utils;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

public class DeltaAccumulator<T> implements IResourceDeltaVisitor {

	private final int								acceptKinds;
	private final Collection<? super T>				collector;
	private final Map<? super T, Integer>			deltaKinds	= new HashMap<>();
	private final IFilter<? super T>				filter;
	private final IResourceConverter<? extends T>	converter;

	public static final DeltaAccumulator<File> fileAccumulator(int acceptKinds, Collection<? super File> collector,
		final FileFilter filter) {
		return new DeltaAccumulator<>(acceptKinds, collector, file -> filter.accept(file),
			resource -> resource.getLocation()
				.toFile());
	}

	public static final DeltaAccumulator<IResource> resourceAccumulator(int acceptKinds,
		Collection<? super IResource> collector, IFilter<IResource> filter) {
		return new DeltaAccumulator<>(acceptKinds, collector, filter, resource -> resource);
	}

	public DeltaAccumulator(int acceptKinds, Collection<? super T> collector, IFilter<? super T> filter,
		IResourceConverter<? extends T> converter) {
		this.acceptKinds = acceptKinds;
		this.collector = collector;
		this.filter = filter;
		this.converter = converter;
	}

	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		if ((acceptKinds & delta.getKind()) == 0)
			return false;
		IResource resource = delta.getResource();
		if (resource.getType() == IResource.FILE) {
			T result = converter.convert(resource);
			if (filter == null || filter.select(result)) {
				collector.add(result);
				deltaKinds.put(result, delta.getKind());
			}
			return false;
		}
		// Must be a container type, so return true in order to recurse in.
		return true;
	}

	public int queryDeltaKind(T t) {
		Integer kind = deltaKinds.get(t);
		return kind != null ? kind.intValue() : IResourceDelta.NO_CHANGE;
	}
}
