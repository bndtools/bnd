package bndtools.builder;

import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.launching.JavaRuntime;

import bndtools.classpath.BndContainerInitializer;
import bndtools.utils.Predicate;

/**
 * Filter for {@linkIClasspathContainer} instances that removes the Bnd and JRE
 * containers
 * 
 * @author Neil Bartlett
 * 
 */
class ClasspathContainerFilter implements Predicate<IClasspathContainer> {

    public boolean select(IClasspathContainer container) {
        boolean result = true;
        if (BndContainerInitializer.PATH_ID.equals(container.getPath())) {
            result = false;
        } else if (JavaRuntime.JRE_CONTAINER.equals(container.getPath().segment(0))) {
            result = false;
        }
        return result;
    }

}