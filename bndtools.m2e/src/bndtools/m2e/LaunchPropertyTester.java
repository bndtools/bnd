package bndtools.m2e;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.m2e.core.project.IMavenProjectFacade;

public class LaunchPropertyTester extends PropertyTester implements MavenRunListenerHelper {
	private static final ILogger	logger									= Logger
		.getLogger(LaunchPropertyTester.class);

	public static final String		PROP_IS_IN_BND_MAVEN_PROJECT			= "isInBndMavenProject";
	public static final String		PROP_IS_RESOLVABLE_BND_MAVEN_PROJECT	= "isResolvableBndMavenProject";
	public static final String		PROP_IS_TESTABLE_BND_MAVEN_PROJECT		= "isTestableBndMavenProject";
	static final String				MAVEN_NATURE							= "org.eclipse.m2e.core.maven2Nature";

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		IResource resource = null;

		if (receiver instanceof IJavaElement) {
			IJavaElement elem = (IJavaElement) receiver;
			resource = elem.getJavaProject()
				.getResource();
		} else if (receiver instanceof IResource) {
			resource = (IResource) receiver;
		}

		if ((resource == null) || !resource.exists()) {
			return false;
		}

		try {
			IProjectNature mavenNature = resource.getProject()
				.getNature(MAVEN_NATURE);

			if (mavenNature == null) {
				return false;
			}
		} catch (CoreException e1) {
			return false;
		}

		switch (property) {
			case PROP_IS_IN_BND_MAVEN_PROJECT :
				try {
					IMavenProjectFacade projectFacade = getMavenProjectFacade(resource);

					return (projectFacade != null) && hasBndMavenPlugin(projectFacade);
				} catch (CoreException e) {
					logger.logError("Error testing '" + PROP_IS_IN_BND_MAVEN_PROJECT + "' property on java element.",
						e);
					return false;
				}
			case PROP_IS_RESOLVABLE_BND_MAVEN_PROJECT :
				try {
					IMavenProjectFacade projectFacade = getMavenProjectFacade(resource);

					return (projectFacade != null) && hasBndResolverMavenPlugin(projectFacade);
				} catch (CoreException e) {
					logger.logError(
						"Error testing '" + PROP_IS_RESOLVABLE_BND_MAVEN_PROJECT + "' property on java element.", e);
					return false;
				}
			case PROP_IS_TESTABLE_BND_MAVEN_PROJECT :
				try {
					IMavenProjectFacade projectFacade = getMavenProjectFacade(resource);

					return (projectFacade != null) && hasBndTestingMavenPlugin(projectFacade);
				} catch (CoreException e) {
					logger.logError(
						"Error testing '" + PROP_IS_TESTABLE_BND_MAVEN_PROJECT + "' property on java element.", e);
					return false;
				}
			default :
				return false;
		}
	}

}
