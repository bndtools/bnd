package bndtools.launch;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

import bndtools.Plugin;
import bndtools.builder.BndProjectNature;

public class LaunchPropertyTester extends PropertyTester {

    public static final String PROP_IS_IN_BND_JAVA_PROJECT = "isInBndJavaProject";

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (PROP_IS_IN_BND_JAVA_PROJECT.equals(property)) {
            try {
                IJavaElement elem = (IJavaElement) receiver;
                IJavaProject javaProject = elem.getJavaProject();

                return javaProject != null && javaProject.exists() && javaProject.getProject().isOpen() && javaProject.getProject().hasNature(BndProjectNature.NATURE_ID);
            } catch (CoreException e) {
                Plugin.getDefault().getLogger().logStatus(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error testing '" + PROP_IS_IN_BND_JAVA_PROJECT + "' property on java element.", e));
                return false;
            }
        }

        return false;
    }

}
