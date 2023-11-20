package bndtools.facades.jdt;

import java.util.Map;

import org.eclipse.core.resources.IIncrementalProjectBuilder2;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.osgi.annotation.versioning.ConsumerType;

import bndtools.facades.util.EclipseBinder;

@ConsumerType
public class IncrementalProjectBuilderFacade extends IncrementalProjectBuilder
	implements IIncrementalProjectBuilder2, IExecutableExtension {

	final EclipseBinder<Delegate> binder = new EclipseBinder<>(Delegate.class, this);

	public interface Delegate {
		ISchedulingRule getRule(IncrementalProjectBuilderFacade pb, int kind, Map<String, String> args);

		IProject[] build(IncrementalProjectBuilderFacade pb, int kind, Map<String, String> args,
			IProgressMonitor monitor)
			throws CoreException;

		void clean(IncrementalProjectBuilderFacade incrementalProjectBuilderFacade, Map<String, String> args,
			IProgressMonitor monitor);
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
		throws CoreException {
		binder.setInitializationData(config, propertyName, data);
	}

	@Override
	public ISchedulingRule getRule(int kind, Map<String, String> args) {
		return binder.get()
			.getRule(this, kind, args);
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		return binder.get()
			.build(this, kind, args, monitor);
	}

	@Override
	public void clean(Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		binder.get()
			.clean(this, args, monitor);
	}

}
