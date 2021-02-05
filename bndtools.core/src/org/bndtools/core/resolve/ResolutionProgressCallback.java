package org.bndtools.core.resolve;

import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import biz.aQute.resolve.ResolutionCallback;

public class ResolutionProgressCallback implements ResolutionCallback {

	final private SubMonitor subMonitor;

	public ResolutionProgressCallback(IProgressMonitor monitor) {
		this.subMonitor = SubMonitor.convert(monitor);
	}

	@Override
	public void processCandidates(Requirement requirement, Set<Capability> wired, List<Capability> candidates) {
		subMonitor.setTaskName("Resolving requirement: " + requirement);
		// split() also does the cancel check for us and throws
		// OperationCanceledException
		subMonitor.setWorkRemaining(1000)
			.split(1);
	}

}
