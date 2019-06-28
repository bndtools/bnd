package bndtools.utils;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;

public interface Requestor<T> {
	T request(IProgressMonitor monitor) throws InvocationTargetException;
}
