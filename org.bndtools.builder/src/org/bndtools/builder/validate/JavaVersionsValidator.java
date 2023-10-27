package org.bndtools.builder.validate;

import java.util.Map;

import org.bndtools.api.IProjectValidator;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Constants;
import bndtools.central.Central;

@Component
public class JavaVersionsValidator implements IProjectValidator {
	@Activate
	public JavaVersionsValidator(@Reference
	Central notused) {}

	@Override
	public void validateProject(Project model) throws Exception {
		IJavaProject javaProject = Central.getJavaProject(model);
		if (javaProject == null) {
			model.error("Eclipse: The project in %s is not linked with a Java project.", model.getBase());
			return;
		}

		Map<String, String> options = javaProject.getOptions(true);

		String javacSource = model.getProperty(Constants.JAVAC_SOURCE);
		if (javacSource != null) {
			String eclipseSource = options.get(JavaCore.COMPILER_SOURCE);
			if (eclipseSource == null)
				eclipseSource = options.get(JavaCore.COMPILER_COMPLIANCE);

			if (!javacSource.equals(eclipseSource)) {
				model
					.warning("Eclipse: javac.source inconsistency between bnd & Eclipse. bnd is %s and Eclipse is %s",
						javacSource, eclipseSource)
					.header("javac.source");
			}
		}

		String javacTarget = model.getProperty(Constants.JAVAC_TARGET);
		if (javacTarget != null) {
			String eclipseTarget = options.get(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM);
			if (!javacTarget.equals(eclipseTarget)) {
				model
					.warning("Eclipse: javac.target inconsistency between bnd & Eclipse. bnd is %s and Eclipse is %s",
						javacTarget, eclipseTarget)
					.header("javac.target");
			}
		}
	}

}
