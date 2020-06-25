package bndtools.core.test.utils;

import org.eclipse.core.runtime.IProgressMonitor;

public interface MonitoredTask {
	void run(IProgressMonitor monitor) throws Exception;
}