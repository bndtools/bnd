package bndtools.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.ResourceUtil;

import aQute.bnd.build.Project;
import bndtools.Plugin;

public class RunShortcut implements ILaunchShortcut, ILaunchShortcut2 {

    public void launch(ISelection selection, String mode) {
        IStructuredSelection is = (IStructuredSelection) selection;
        if ( is.getFirstElement() != null ) {
            Object selected = is.getFirstElement();
            String projectName = null;
            if (selected instanceof IJavaElement )
                projectName = ((IJavaElement) selected).getJavaProject().getElementName();
            else if (selected instanceof IResource && Project.BNDFILE.equals(((IResource) selected).getName()))
                projectName = ((IResource) selected).getProject().getName();
            else if (selected instanceof IAdaptable) {
                IAdaptable adaptable = (IAdaptable) selected;
                IJavaElement javaElement = (IJavaElement) adaptable.getAdapter(IJavaElement.class);
                if(javaElement != null) {
                    IJavaProject javaProject = javaElement.getJavaProject();
                    if(javaProject != null)
                        projectName = javaProject.getElementName();
                }

                IResource resource = (IResource) adaptable.getAdapter(IResource.class);
                if(resource != null && Project.BNDFILE.equals(resource.getName()))
                    projectName = resource.getProject().getName();
            }

            if(projectName != null)
                launch(projectName, mode);
        }
    }

    public void launch(IEditorPart editor, String mode) {
        IEditorInput input = editor.getEditorInput();
        IJavaElement element = (IJavaElement) input.getAdapter(IJavaElement.class);
        if (element != null) {
            IJavaProject jproject = element.getJavaProject();
            if (jproject != null) {
                launch(jproject.getElementName(), mode);
            }
        }
    }

    void launch(String projectName, String mode) {
        try {
            ILaunchConfiguration config = findLaunchConfig(projectName);
            if (config == null)
                config = createConfiguration(projectName);
            DebugUITools.launch(config, mode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ILaunchConfiguration findLaunchConfig(String projectName) throws CoreException {
        List<ILaunchConfiguration> candidateConfigs = new ArrayList<ILaunchConfiguration>();
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

        ILaunchConfigurationType configType = manager.getLaunchConfigurationType(LaunchConstants.LAUNCH_ID_OSGI_RUNTIME);
        ILaunchConfiguration[] configs = manager.getLaunchConfigurations(configType);

        for (int i = 0; i < configs.length; i++) {
            ILaunchConfiguration config = configs[i];
            String configProjectName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
            if(configProjectName != null && configProjectName.equals(projectName)) {
                candidateConfigs.add(config);
            }
        }

        // Return the latest (last in the list)
        return !candidateConfigs.isEmpty()
            ? candidateConfigs.get(candidateConfigs.size() - 1)
            : null;
    }

    ILaunchConfiguration createConfiguration(String projectName) throws Exception {
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType configType = manager.getLaunchConfigurationType(LaunchConstants.LAUNCH_ID_OSGI_RUNTIME);

        ILaunchConfigurationWorkingCopy wc;
        wc = configType.newInstance(null, manager.generateUniqueLaunchConfigurationNameFrom(projectName));
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
        return wc.doSave();
    }

    IProject getProject(ISelection selection) {
        if(selection instanceof IStructuredSelection) {
            Object element = ((IStructuredSelection) selection).getFirstElement();
            if(element instanceof IResource)
                return ((IResource) element).getProject();
            else if(element instanceof IAdaptable) {
                IResource resource = (IResource) ((IAdaptable) element).getAdapter(IResource.class);
                if(resource != null)
                    return resource.getProject();
            }
        }
        return null;
    }

    IProject getProject(IEditorPart editorPart) {
        IResource resource = ResourceUtil.getResource(editorPart);
        return resource != null
            ? resource.getProject()
            : null;
    }

    public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
        IProject project = getProject(selection);
        return project != null
            ? getLaunchConfigsForProject(project)
            : null;
    }

    ILaunchConfiguration[] getLaunchConfigsForProject(IProject project) {
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

        ILaunchConfigurationType type = manager.getLaunchConfigurationType(LaunchConstants.LAUNCH_ID_OSGI_RUNTIME);
        try {
            ILaunchConfiguration[] all = manager.getLaunchConfigurations(type);
            List<ILaunchConfiguration> result = new ArrayList<ILaunchConfiguration>(all.length);

            return result.toArray(new ILaunchConfiguration[result.size()]);
        } catch (CoreException e) {
            Plugin.logError("Error retrieving launch configurations.", e);
            return null;
        }
    }

    public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorPart) {
        IProject project = getProject(editorPart);
        return project != null
            ? getLaunchConfigsForProject(project)
            : null;
    }

    public IResource getLaunchableResource(ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structSel= (IStructuredSelection) selection;
            if (structSel.size() == 1) {
                Object selected= structSel.getFirstElement();
                if (!(selected instanceof IJavaElement) && selected instanceof IAdaptable) {
                    selected= ((IAdaptable) selected).getAdapter(IJavaElement.class);
                }
                if (selected instanceof IJavaElement) {
                    return ((IJavaElement)selected).getResource();
                }
            }
        }
        return null;
    }

    public IResource getLaunchableResource(IEditorPart editorPart) {
        ITypeRoot element= JavaUI.getEditorInputTypeRoot(editorPart.getEditorInput());
        if (element != null) {
            try {
                return element.getCorrespondingResource();
            } catch (JavaModelException e) {
            }
        }
        return null;
    }

}
