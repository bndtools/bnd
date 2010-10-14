package bndtools.utils;

import org.eclipse.core.runtime.IProgressMonitor;

public interface Requestor<T> {
    public T request(IProgressMonitor monitor);
}
