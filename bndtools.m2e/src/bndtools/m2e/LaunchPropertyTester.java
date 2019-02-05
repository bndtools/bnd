package bndtools.m2e;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.m2e.core.project.IMavenProjectFacade;

public class LaunchPropertyTester extends PropertyTester implements MavenRunListenerHelper {
    private static final ILogger logger = Logger.getLogger(LaunchPropertyTester.class);

    public static final String PROP_IS_IN_BND_MAVEN_PROJECT = "isInBndMavenProject";
    public static final String PROP_IS_RESOLVABLE_BND_MAVEN_PROJECT = "isResolvableBndMavenProject";
    public static final String PROP_IS_TESTABLE_BND_MAVEN_PROJECT = "isTestableBndMavenProject";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        IJavaElement elem = (IJavaElement) receiver;
        IJavaProject javaProject = elem.getJavaProject();

        if (javaProject == null) {
            return false;
        }

        switch (property) {
            case PROP_IS_IN_BND_MAVEN_PROJECT :
                try {
                    IMavenProjectFacade projectFacade = getMavenProjectFacade(javaProject.getResource());

                    return javaProject.exists() && (projectFacade != null) && hasBndMavenPlugin(projectFacade);
                } catch (CoreException e) {
                    logger.logError("Error testing '" + PROP_IS_IN_BND_MAVEN_PROJECT + "' property on java element.", e);
                    return false;
                }
            case PROP_IS_RESOLVABLE_BND_MAVEN_PROJECT :
                try {
                    IMavenProjectFacade projectFacade = getMavenProjectFacade(javaProject.getResource());

                    return javaProject.exists() && (projectFacade != null) && hasBndResolverMavenPlugin(projectFacade);
                } catch (CoreException e) {
                    logger.logError("Error testing '" + PROP_IS_RESOLVABLE_BND_MAVEN_PROJECT + "' property on java element.", e);
                    return false;
                }
            case PROP_IS_TESTABLE_BND_MAVEN_PROJECT :
                try {
                    IMavenProjectFacade projectFacade = getMavenProjectFacade(javaProject.getResource());

                    return javaProject.exists() && (projectFacade != null) && hasBndTestingMavenPlugin(projectFacade);
                } catch (CoreException e) {
                    logger.logError("Error testing '" + PROP_IS_TESTABLE_BND_MAVEN_PROJECT + "' property on java element.", e);
                    return false;
                }
            default :
                return false;
        }
    }

}
