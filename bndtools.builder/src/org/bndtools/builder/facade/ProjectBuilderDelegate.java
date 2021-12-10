package org.bndtools.builder.facade;

import java.util.Map;

import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IBuildContext;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

public abstract class ProjectBuilderDelegate {

	// Called "zuper" as in "super", it's kind-of the loosely-coupled superclass
	// of this class.
	// Obviously can't use "super" because it is a Java keyword.
	protected BuilderFacade zuper;

	public void setSuper(BuilderFacade zuper) {
		this.zuper = zuper;
	}

	protected abstract IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor)
		throws CoreException;

	protected void clean(IProgressMonitor monitor) throws CoreException {}

	public ISchedulingRule getRule(int kind, Map<String, String> args) {
		return zuper.superGetRule(kind, args);
	}

	public final void forgetLastBuiltState() {
		zuper.forgetLastBuiltState();
	}

	public final void rememberLastBuiltState() {
		zuper.rememberLastBuiltState();
	}

	public final ICommand getCommand() {
		return zuper.getCommand();
	}

	public final IResourceDelta getDelta(IProject project) {
		return zuper.getDelta(project);
	}

	public final IProject getProject() {
		return zuper.getProject();
	}

	public final IBuildConfiguration getBuildConfig() {
		return zuper.getBuildConfig();
	}

	public final boolean hasBeenBuilt(IProject project) {
		return zuper.hasBeenBuilt(project);
	}

	public final boolean isInterrupted() {
		return zuper.isInterrupted();
	}

	public final void needRebuild() {
		zuper.needRebuild();
	}

	protected void startupOnInitialize() {
		zuper.superStartupOnInitialize();
	}

	public final IBuildContext getContext() {
		return zuper.getContext();
	}

}
