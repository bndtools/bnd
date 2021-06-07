package org.bndtools.core.resolve;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;

import biz.aQute.resolve.ResolverLogger;
import biz.aQute.resolve.RunResolution;

public class ResolutionResult {

	private final Outcome			outcome;
	private final IStatus			status;
	private final RunResolution		resolution;
	private final ResolverLogger	logger;

	public enum Outcome {
		Resolved,
		Unresolved,
		Error,
		Cancelled
	}

	public ResolutionResult(Outcome outcome, RunResolution resolution, IStatus status, ResolverLogger logger) {
		this.outcome = requireNonNull(outcome);
		this.resolution = resolution;
		this.status = requireNonNull(status);
		this.logger = requireNonNull(logger);
	}

	public Outcome getOutcome() {
		return outcome;
	}

	public Map<Resource, List<Wire>> getResourceWirings() {
		if (resolution != null) {
			return resolution.required;
		}
		return Collections.emptyMap();
	}

	public Map<Resource, List<Wire>> getOptionalResources() {
		if (resolution != null) {
			return resolution.optional;
		}
		return Collections.emptyMap();
	}

	public ResolutionException getResolutionException() {
		if ((resolution != null) && (resolution.exception instanceof ResolutionException)) {
			return (ResolutionException) resolution.exception;
		}
		return null;
	}

	public IStatus getStatus() {
		return status;
	}

	public String getLog() {
		if (resolution != null) {
			return resolution.log;
		}
		return "";
	}

	public RunResolution getResolution() {
		return resolution;
	}

	public ResolverLogger getLogger() {
		return logger;
	}
}
