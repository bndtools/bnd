package bndtools.utils;

import org.eclipse.core.resources.IResource;

public interface IResourceConverter<T> {
	T convert(IResource resource);
}
