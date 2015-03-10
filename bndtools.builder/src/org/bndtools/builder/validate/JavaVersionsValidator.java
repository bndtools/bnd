package org.bndtools.builder.validate;

import java.util.Map;

import org.bndtools.api.IProjectValidator;
import org.bndtools.api.IValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Builder;
import bndtools.central.Central;

public class JavaVersionsValidator implements IValidator, IProjectValidator {

    @Override
    public IStatus validate(Builder builder) {
        return Status.OK_STATUS;
    }

    @Override
    public void validateProject(Project model) throws Exception {
        IJavaProject javaProject = Central.getJavaProject(model);

        @SuppressWarnings("unchecked")
        Map<String,String> options = javaProject.getOptions(true);

        String javacSource = model.getProperty("javac.source"); // TODO added constants in bnd
        if (javacSource != null) {
            String eclipseSource = options.get(JavaCore.COMPILER_SOURCE);
            if (eclipseSource == null)
                eclipseSource = options.get(JavaCore.COMPILER_COMPLIANCE);

            if (!javacSource.equals(eclipseSource)) {
                model.warning("Eclipse: javac.source inconsistency between bnd & Eclipse. bnd is %s and Eclipse is %s", javacSource, eclipseSource).header("javac.source");
            }
        }

        String javacTarget = model.getProperty("javac.target"); // TOOD added constant in bnd
        if (javacTarget != null) {
            String eclipseTarget = options.get(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM);
            if (!javacTarget.equals(eclipseTarget)) {
                model.warning("Eclipse: javac.target inconsistency between bnd & Eclipse. bnd is %s and Eclipse is %s", javacTarget, eclipseTarget).header("javac.target");
            }
        }
    }

}