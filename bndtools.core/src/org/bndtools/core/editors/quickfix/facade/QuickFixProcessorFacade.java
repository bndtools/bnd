package org.bndtools.core.editors.quickfix.facade;

import static aQute.bnd.exceptions.FunctionWithException.asFunction;

import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class QuickFixProcessorFacade implements IQuickFixProcessor {

	static ServiceTracker<IQuickFixProcessor, IQuickFixProcessor> processors;

	static public void setup(BundleContext bc) {
		processors = new ServiceTracker<>(bc, IQuickFixProcessor.class, null);
		processors.open();
	}

	static public void cleanup() {
		processors.close();
	}

	Stream<IQuickFixProcessor> streamOf() {
		ServiceReference<IQuickFixProcessor>[] ref = processors.getServiceReferences();
		if (ref == null) {
			return Stream.empty();
		}
		return Stream.of(ref)
			.map(processors::getService);
	}

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return streamOf().anyMatch(processor -> processor.hasCorrections(unit, problemId));
	}

	@Override
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations)
		throws CoreException {
		IJavaCompletionProposal[] retval = streamOf()
			.map(asFunction(processor -> processor.getCorrections(context, locations)))
			.filter(Objects::nonNull)
			.flatMap(Stream::of)
			.toArray(IJavaCompletionProposal[]::new);
		return retval.length == 0 ? null : retval;
	}
}
