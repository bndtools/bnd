package org.bndtools.builder.classpath;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CompilationParticipant;

public class BndContainerCompilationParticipant extends CompilationParticipant {
    private static final ILogger logger = Logger.getLogger(BndContainerCompilationParticipant.class);

    public BndContainerCompilationParticipant() {
        super();
    }

    @Override
    public int aboutToBuild(IJavaProject javaProject) {
        try {
            return BndContainerInitializer.requestClasspathContainerUpdate(javaProject) ? NEEDS_FULL_BUILD : READY_FOR_BUILD;
        } catch (CoreException e) {
            logger.logWarning(String.format("Failed to update classpath container for project %s", javaProject.getProject()
                .getName()), e);
        }
        return READY_FOR_BUILD;
    }

    @Override
    public boolean isActive(IJavaProject javaProject) {
        return BndContainerInitializer.getClasspathContainer(javaProject) != null;
    }
}
