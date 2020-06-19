package org.bndtools.core.resolve;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;

import biz.aQute.resolve.ResolverLogger;
import biz.aQute.resolve.RunResolution;

public class ResolutionResult {

	private final Outcome		outcome;
	private final IStatus		status;
	private final RunResolution	resolution;
	private final ResolverLogger	logger;

	public enum Outcome {
		Resolved,
		Unresolved,
		Error,
		Cancelled
	}

	public ResolutionResult(Outcome outcome, RunResolution resolution, IStatus status, ResolverLogger logger) {
		this.outcome = outcome;
		this.resolution = resolution;
		this.status = status;
		this.logger = logger;
	}

	public Outcome getOutcome() {
		return outcome;
	}

	public Map<Resource, List<Wire>> getResourceWirings() {
		return resolution.required;
	}

	public Map<Resource, List<Wire>> getOptionalResources() {
		return resolution.optional;
	}

	public ResolutionException getResolutionException() {
		if (resolution.exception instanceof ResolutionException)
			return (ResolutionException) resolution.exception;
		return null;
	}

	public IStatus getStatus() {
		return status;
	}

	public String getLog() {
		return resolution.log;
	}

	public RunResolution getResolution() {
		return resolution;
	}

	public ResolverLogger getLogger() {
		return logger;
	}
}
