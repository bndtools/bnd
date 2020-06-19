package org.bndtools.core.resolve;

import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import biz.aQute.resolve.ResolutionCallback;

public class CancelOperationCallback implements ResolutionCallback {

	private final IProgressMonitor monitor;

	public CancelOperationCallback(IProgressMonitor monitor) {
		this.monitor = monitor;
	}

	@Override
	public void processCandidates(Requirement requirement, Set<Capability> wired, List<Capability> candidates) {
		if (monitor.isCanceled()) {
			throw new ResolveCancelledException();
		}
	}

}
