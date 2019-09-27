package bndtools.launch;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

import bndtools.launch.util.LaunchUtils;

public class LaunchPropertyTester extends PropertyTester {
	private static final ILogger	logger						= Logger.getLogger(LaunchPropertyTester.class);

	public static final String		PROP_IS_IN_BND_JAVA_PROJECT	= "isInBndJavaProject";

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (PROP_IS_IN_BND_JAVA_PROJECT.equals(property)) {
			try {
				IJavaElement elem = (IJavaElement) receiver;
				IJavaProject javaProject = elem.getJavaProject();

				return javaProject != null && javaProject.exists()
					&& LaunchUtils.isInBndWorkspaceProject(javaProject.getProject());
			} catch (CoreException e) {
				logger.logError("Error testing '" + PROP_IS_IN_BND_JAVA_PROJECT + "' property on java element.", e);
				return false;
			}
		}

		return false;
	}

}
