package bndtools.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.ResourceUtil;

import aQute.bnd.build.Project;
import bndtools.Plugin;

public abstract class AbstractLaunchShortcut implements ILaunchShortcut, ILaunchShortcut2 {

    private final String launchId;

    public AbstractLaunchShortcut(String launchId) {
        this.launchId = launchId;
    }

    public void launch(ISelection selection, String mode) {
        IStructuredSelection is = (IStructuredSelection) selection;
        if ( is.getFirstElement() != null ) {
            try {
                Object selected = is.getFirstElement();
                launchSelectedObject(selected, mode);
            } catch (CoreException e) {
                ErrorDialog.openError(null, "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error configuring launch.", e));
            }
        }
    }

    private void launchSelectedObject(Object selected, String mode) throws CoreException {
        if (selected instanceof IJavaElement) {
            launchJavaElement((IJavaElement) selected, mode);
        } else if (selected instanceof IResource && Project.BNDFILE.equals(((IResource) selected).getName())) {
            IProject project = ((IResource) selected).getProject();
            launchProject(project, mode);
        } else if(selected instanceof IFile && ((IFile) selected).getName().endsWith(LaunchConstants.EXT_BNDRUN)) {
            IFile bndRunFile = (IFile) selected;
            launchBndRun(bndRunFile, mode);
        }
        else if (selected instanceof IAdaptable) {
            IAdaptable adaptable = (IAdaptable) selected;
            IJavaElement javaElement = (IJavaElement) adaptable.getAdapter(IJavaElement.class);
            if(javaElement != null) {
                launchJavaElement(javaElement, mode);
            } else {
                IResource resource = (IResource) adaptable.getAdapter(IResource.class);
                if (resource != null && resource != selected)
                    launchSelectedObject(resource, mode);
            }
        }
    }

    public void launch(IEditorPart editor, String mode) {
        IEditorInput input = editor.getEditorInput();
        IJavaElement element = (IJavaElement) input.getAdapter(IJavaElement.class);
        if (element != null) {
            IJavaProject jproject = element.getJavaProject();
            if (jproject != null) {
                launch(jproject.getProject().getFullPath(), mode);
            }
        } else {
            IFile file = ResourceUtil.getFile(input);
            if (file != null) {
                if (file.getName().endsWith(LaunchConstants.EXT_BNDRUN)) {
                    launch(file.getFullPath(), mode);
                } else if (file.getName().equals(Project.BNDFILE)) {
                    launch(file.getProject().getFullPath(), mode);
                }
            }
        }
    }

    protected void launchJavaElement(IJavaElement element, String mode) throws CoreException {
        launch(element.getJavaProject().getProject().getFullPath(), mode);
    }

    protected void launchProject(IProject project, String mode) {
        launch(project.getFullPath(), mode);
    }

    protected void launchBndRun(IFile bndRunFile, String mode) {
        launch(bndRunFile.getFullPath(), mode);
    }

    void launch(IPath targetPath, String mode) {
        targetPath = targetPath.makeRelative();
        try {
            ILaunchConfiguration config = findLaunchConfig(targetPath);
            if (config == null) {
                ILaunchConfigurationWorkingCopy wc = createConfiguration(targetPath);
                config = wc.doSave();
            }
            DebugUITools.launch(config, mode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ILaunchConfiguration findLaunchConfig(IPath targetPath) throws CoreException {
        List<ILaunchConfiguration> candidateConfigs = new ArrayList<ILaunchConfiguration>();
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

        ILaunchConfigurationType configType = manager.getLaunchConfigurationType(launchId);
        ILaunchConfiguration[] configs = manager.getLaunchConfigurations(configType);

        for (int i = 0; i < configs.length; i++) {
            ILaunchConfiguration config = configs[i];
            String configTargetName = config.getAttribute(LaunchConstants.ATTR_LAUNCH_TARGET, (String) null);
            if(configTargetName != null && configTargetName.equals(targetPath.toString())) {
                candidateConfigs.add(config);
            }
        }

        // Return the latest (last in the list)
        return !candidateConfigs.isEmpty()
            ? candidateConfigs.get(candidateConfigs.size() - 1)
            : null;
    }

    ILaunchConfigurationWorkingCopy createConfiguration(IPath targetPath) throws CoreException {
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType configType = manager.getLaunchConfigurationType(launchId);

        ILaunchConfigurationWorkingCopy wc;
        wc = configType.newInstance(null, manager.generateUniqueLaunchConfigurationNameFrom(targetPath.lastSegment()));
        wc.setAttribute(LaunchConstants.ATTR_LAUNCH_TARGET, targetPath.toString());
        wc.setAttribute(LaunchConstants.ATTR_CLEAN, LaunchConstants.DEFAULT_CLEAN);
        wc.setAttribute(LaunchConstants.ATTR_DYNAMIC_BUNDLES, LaunchConstants.DEFAULT_DYNAMIC_BUNDLES);

        IResource targetResource = ResourcesPlugin.getWorkspace().getRoot().findMember(targetPath);
        if (targetResource != null && targetResource.exists()) {
            wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, LaunchUtils.getLaunchProjectName(targetResource));
        }

        return wc;
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

        ILaunchConfigurationType type = manager.getLaunchConfigurationType(launchId);
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
