package bndtools.launch.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.ResourceUtil;

import aQute.bnd.build.Project;
import bndtools.Plugin;
import bndtools.editor.BndEditor;
import bndtools.editor.pages.ProjectRunPage;
import bndtools.launch.LaunchConstants;

public abstract class AbstractLaunchShortcut implements ILaunchShortcut2 {
	private static final ILogger	logger	= Logger.getLogger(AbstractLaunchShortcut.class);

	private final String			launchId;

	public AbstractLaunchShortcut(String launchId) {
		this.launchId = launchId;
	}

	@Override
	public void launch(ISelection selection, String mode) {
		IStructuredSelection is = (IStructuredSelection) selection;
		int size = is.size();

		if (size == 1 && is.getFirstElement() != null) {
			try {
				Object selected = is.getFirstElement();
				launchSelectedObject(selected, mode);
			} catch (CoreException e) {
				ErrorDialog.openError(null, "Error", null,
					new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error configuring launch.", e));
			}
		} else if (size > 1) {
			// only support multiple IJavaElements
			List<IJavaElement> elements = new ArrayList<>();
			Iterator<?> iterator = is.iterator();

			while (iterator.hasNext()) {
				Object element = iterator.next();
				if (element instanceof IJavaElement) {
					elements.add((IJavaElement) element);
				}
			}

			if (!elements.isEmpty()) {
				try {
					launchJavaElements(elements, mode);
				} catch (CoreException e) {
					ErrorDialog.openError(null, "Error", null,
						new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error launching elements.", e));
				}
			}
		}
	}

	protected void launchSelectedObject(Object selected, String mode) throws CoreException {
		if (selected instanceof IJavaElement) {
			launchJavaElements(Collections.singletonList((IJavaElement) selected), mode);
		} else if (selected instanceof IResource && Project.BNDFILE.equals(((IResource) selected).getName())) {
			IProject project = ((IResource) selected).getProject();
			launchProject(project, mode);
		} else if (selected instanceof IFile && ((IFile) selected).getName()
			.endsWith(LaunchConstants.EXT_BNDRUN)) {
			IFile bndRunFile = (IFile) selected;
			launchBndRun(bndRunFile, mode);
		} else if (selected instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) selected;
			IJavaElement javaElement = adaptable.getAdapter(IJavaElement.class);
			if (javaElement != null) {
				launchJavaElements(Collections.singletonList(javaElement), mode);
			} else {
				IResource resource = adaptable.getAdapter(IResource.class);
				if (resource != null && resource != selected)
					launchSelectedObject(resource, mode);
			}
		}
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
		/*
		 * unfortunately, the editor is the ProjectRunPage and the doSave and
		 * isDirty methods are not properly implemented. We therefore need to do
		 * some deep casting :-(
		 */
		if (editor instanceof ProjectRunPage) {
			BndEditor editor2 = (BndEditor) ((ProjectRunPage) editor).getEditor();
			editor2.commitDirtyPages();
			if (editor2.getModel()
				.isDirty()) {
				Display display = Display.findDisplay(Thread.currentThread());
				Shell activeShell = display.getActiveShell();
				MessageDialog dialog = new MessageDialog(activeShell, "Properties have changed", null,
					"Save properties before launch?", MessageDialog.CONFIRM, new String[] {
						"Save", "Launch Previous", "Cancel Launch"
					}, 0);
				int result = dialog.open();
				if (result == 0) {
					/*
					 * Doing the save in a background job but this throws a
					 * thread exception for SWT, would be better if this worked
					 */
					// Job job = new Job("Save the properties before launching")
					// {
					// @Override
					// protected IStatus run(IProgressMonitor m) {
					editor2.doSave(null);
					launch0(editor, mode);
					// return Status.OK_STATUS;
					// }
					// };
					// job.schedule();
					return;
				}
			}
		}
		launch0(editor, mode);
	}

	private void launch0(IEditorPart editor, String mode) {
		IEditorInput input = editor.getEditorInput();
		IJavaElement element = input.getAdapter(IJavaElement.class);
		if (element != null) {
			IJavaProject jproject = element.getJavaProject();
			if (jproject != null) {
				launchProject(jproject.getProject(), mode);
			}
		} else {
			IFile file = ResourceUtil.getFile(input);
			if (file != null) {
				if (file.getName()
					.endsWith(LaunchConstants.EXT_BNDRUN)) {
					launchBndRun(file, mode);
				} else if (file.getName()
					.equals(Project.BNDFILE)) {
					launch(file.getProject()
						.getFullPath(), file.getProject(), mode);
				}
			}
		}
	}

	@SuppressWarnings("unused")
	protected void launchJavaElements(List<IJavaElement> elements, String mode) throws CoreException {
		IProject targetProject = elements.get(0)
			.getJavaProject()
			.getProject();
		launchProject(targetProject, mode);
	}

	protected void launchProject(IProject project, String mode) {
		IPath fullPath = project.getFullPath();
		IPath bndfile = fullPath.append(Project.BNDFILE);
		launch(bndfile, project, mode);
	}

	protected void launchBndRun(IFile bndRunFile, String mode) {
		launch(bndRunFile.getFullPath(), bndRunFile.getProject(), mode);
	}

	protected void launch(IPath targetPath, IProject targetProject, String mode) {
		IPath tp = targetPath == null ? null : targetPath.makeRelative();
		try {
			ILaunchConfiguration config = findLaunchConfig(tp, targetProject);
			if (config == null) {
				ILaunchConfigurationWorkingCopy wc = createConfiguration(tp, targetProject);
				config = wc.doSave();
			}
			DebugUITools.launch(config, mode);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected ILaunchConfiguration findLaunchConfig(IPath targetPath, IProject targetProject) throws CoreException {
		List<ILaunchConfiguration> candidateConfigs = new ArrayList<>();
		ILaunchManager manager = DebugPlugin.getDefault()
			.getLaunchManager();

		ILaunchConfigurationType configType = manager.getLaunchConfigurationType(launchId);
		ILaunchConfiguration[] configs = manager.getLaunchConfigurations(configType);

		for (int i = 0; i < configs.length; i++) {
			ILaunchConfiguration config = configs[i];

			String configTargetProject = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
				(String) null);
			if (configTargetProject != null && configTargetProject.equals(targetProject.getName())) {
				if (targetPath != null) {
					String configTargetName = config.getAttribute(LaunchConstants.ATTR_LAUNCH_TARGET, (String) null);
					if (configTargetName != null && configTargetName.equals(targetPath.toString())) {
						candidateConfigs.add(config);
					}
				} else {
					candidateConfigs.add(config);
				}
			}
		}

		// Return the latest (last in the list)
		return !candidateConfigs.isEmpty() ? candidateConfigs.get(candidateConfigs.size() - 1) : null;
	}

	protected ILaunchConfigurationWorkingCopy createConfiguration(IPath targetPath, IProject targetProject)
		throws CoreException {
		ILaunchManager manager = DebugPlugin.getDefault()
			.getLaunchManager();
		ILaunchConfigurationType configType = manager.getLaunchConfigurationType(launchId);

		ILaunchConfigurationWorkingCopy wc;
		wc = configType.newInstance(null, manager.generateLaunchConfigurationName(targetPath.lastSegment()));
		wc.setAttribute(LaunchConstants.ATTR_LAUNCH_TARGET, targetPath.toString());
		wc.setAttribute(LaunchConstants.ATTR_CLEAN, LaunchConstants.DEFAULT_CLEAN);
		wc.setAttribute(LaunchConstants.ATTR_DYNAMIC_BUNDLES, LaunchConstants.DEFAULT_DYNAMIC_BUNDLES);

		if (targetProject != null) {
			IJavaProject javaProject = JavaCore.create(targetProject);
			if (javaProject.exists()) {
				wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, targetProject.getName());
			}
		}

		return wc;
	}

	protected static IProject getProject(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) selection).getFirstElement();
			if (element instanceof IResource)
				return ((IResource) element).getProject();
			else if (element instanceof IAdaptable) {
				IResource resource = ((IAdaptable) element).getAdapter(IResource.class);
				if (resource != null)
					return resource.getProject();
			}
		}
		return null;
	}

	protected static IProject getProject(IEditorPart editorPart) {
		IResource resource = ResourceUtil.getResource(editorPart);
		return resource != null ? resource.getProject() : null;
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
		IProject project = getProject(selection);
		return project != null ? getLaunchConfigsForProject(project) : null;
	}

	protected ILaunchConfiguration[] getLaunchConfigsForProject(@SuppressWarnings("unused") IProject project) {
		ILaunchManager manager = DebugPlugin.getDefault()
			.getLaunchManager();

		ILaunchConfigurationType type = manager.getLaunchConfigurationType(launchId);
		try {
			ILaunchConfiguration[] all = manager.getLaunchConfigurations(type);
			List<ILaunchConfiguration> result = new ArrayList<>(all.length);

			return result.toArray(new ILaunchConfiguration[0]);
		} catch (CoreException e) {
			logger.logError("Error retrieving launch configurations.", e);
			return null;
		}
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorPart) {
		IProject project = getProject(editorPart);
		return project != null ? getLaunchConfigsForProject(project) : null;
	}

	@Override
	public IResource getLaunchableResource(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structSel = (IStructuredSelection) selection;
			if (structSel.size() == 1) {
				Object selected = structSel.getFirstElement();
				if (!(selected instanceof IJavaElement) && selected instanceof IAdaptable) {
					selected = ((IAdaptable) selected).getAdapter(IJavaElement.class);
				}
				if (selected instanceof IJavaElement) {
					return ((IJavaElement) selected).getResource();
				}
			}
		}
		return null;
	}

	@Override
	public IResource getLaunchableResource(IEditorPart editorPart) {
		ITypeRoot element = JavaUI.getEditorInputTypeRoot(editorPart.getEditorInput());
		if (element != null) {
			try {
				return element.getCorrespondingResource();
			} catch (JavaModelException e) {}
		}
		return null;
	}

}
