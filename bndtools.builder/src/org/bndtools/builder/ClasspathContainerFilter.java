package org.bndtools.builder;

import org.bndtools.api.BndtoolsConstants;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.launching.JavaRuntime;
import org.osgi.util.function.Predicate;

/**
 * Filter for {@link IClasspathContainer} instances that removes the Bnd and JRE
 * containers
 *
 * @author Neil Bartlett
 */
class ClasspathContainerFilter implements Predicate<IClasspathContainer> {

	@Override
	public boolean test(IClasspathContainer container) {
		boolean result = true;
		if (BndtoolsConstants.BND_CLASSPATH_ID.equals(container.getPath())) {
			result = false;
		} else if (JavaRuntime.JRE_CONTAINER.equals(container.getPath()
			.segment(0))) {
			result = false;
		}
		return result;
	}

}
