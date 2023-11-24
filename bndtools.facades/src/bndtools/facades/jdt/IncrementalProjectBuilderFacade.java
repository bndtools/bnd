package bndtools.facades.jdt;

import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.core.resources.IIncrementalProjectBuilder2;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import bndtools.facades.util.FacadeUtil;

public class IncrementalProjectBuilderFacade extends FacadeUtil {

	class Delegate extends IncrementalProjectBuilder implements IIncrementalProjectBuilder2 {

		final Supplier<Object> binder;

		Delegate(Function<Object, Supplier<Object>> bind) {
			this.binder = bind.apply(this);
		}

		static MethodHandle m1 = lookup(Delegate.class, "setInitializationData", void.class,
			IConfigurationElement.class, String.class, Object.class);
		@Override
		public void setInitializationData(IConfigurationElement arg0, String arg1, Object arg2) {
			invokeVoid(() -> m1.invoke(binder.get(), arg0, arg1, arg2));
		}

		static MethodHandle	m2	= lookup(Delegate.class, "getRule", ISchedulingRule.class, int.class, Map.class);
		@Override
		public ISchedulingRule getRule(int arg0, Map<String, String> arg1) {
			return (ISchedulingRule) invokeReturn(() -> m2.invoke(binder.get(), arg0, arg1));
		}

		static MethodHandle	m3	= lookup(Delegate.class, "build", IProject[].class, int.class, Map.class,
			IProgressMonitor.class);
		@Override
		protected IProject[] build(int arg0, Map<String, String> arg1, IProgressMonitor arg2) {
			return (IProject[]) invokeReturn(() -> m3.invoke(binder.get(), arg0, arg1, arg2));
		}

		static MethodHandle m4 = lookup(Delegate.class, "clean", void.class, Map.class, IProgressMonitor.class);
		@Override
		public void clean(Map<String, String> arg0, IProgressMonitor arg1) {
			invokeVoid(() -> m4.invoke(binder.get(), arg0, arg1));
		}
	}

	@Override
	protected Object createDelegate(Function<Object, Supplier<Object>> bind) {
		return new Delegate(bind);
	}

}
