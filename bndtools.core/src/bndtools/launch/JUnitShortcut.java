package bndtools.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import bndtools.launch.api.AbstractLaunchShortcut;

public class JUnitShortcut extends AbstractLaunchShortcut {

    public JUnitShortcut() {
        super(LaunchConstants.LAUNCH_ID_OSGI_JUNIT);
    }

    @Override
    protected void launchJavaElement(IJavaElement element, String mode) throws CoreException {
        IPath projectPath = element.getJavaProject().getProject().getFullPath().makeRelative();

        ILaunchConfiguration config = findLaunchConfig(projectPath);
        if (config == null) {
            ILaunchConfigurationWorkingCopy wc = createConfiguration(projectPath);
            wc.doSave();

            customise(element, wc);
            config = wc;
        } else {
            ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
            customise(element, wc);
            config = wc;
        }
        DebugUITools.launch(config, mode);
    }

    private static void customise(IJavaElement element, ILaunchConfigurationWorkingCopy config) throws JavaModelException {
        String className = null;
        String methodName = null;

        if (element.getElementType() == IJavaElement.COMPILATION_UNIT) {
            for (IType type : ((ICompilationUnit) element).getTypes()) {
                if (Flags.isPublic(type.getFlags())) {
                    className = type.getFullyQualifiedName();
                    break;
                }
            }
        } else if (element.getElementType() == IJavaElement.TYPE) {
            className = ((IType) element).getFullyQualifiedName();
        } else if (element.getElementType() == IJavaElement.METHOD) {
            IMethod method = (IMethod) element;
            className = ((IType) method.getParent()).getFullyQualifiedName();
            methodName = method.getElementName();
        }

        if (className != null) {
            config.setAttribute("org.eclipse.jdt.launching.MAIN_TYPE", className);
            if (methodName != null) {
                config.setAttribute("org.eclipse.jdt.junit.TESTNAME", methodName);
            }
        }
    }

}