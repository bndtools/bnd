/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package name.neilbartlett.eclipse.bndtools.launch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
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
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class LaunchShortcut implements ILaunchShortcut, ILaunchShortcut2 {

	private static final String LAUNCH_GROUP_ID = Plugin.PLUGIN_ID + ".launchRunGroup";
	
	public void launch(ISelection selection, String mode) {
		if(selection instanceof IStructuredSelection) {
			Iterator<?> iterator = ((IStructuredSelection) selection).iterator();
			while(iterator.hasNext()) {
				Object item = iterator.next();
				if(item instanceof IResource) {
					launchBndProject(((IResource) item).getProject(), mode);
					return;
				}
				if(item instanceof IAdaptable) {
					IResource resource = (IResource) ((IAdaptable) item).getAdapter(IResource.class);
					if(resource != null) {
						launchBndProject(resource.getProject(), mode);
						return;
					}
				}
			}
		}
	}
	public void launch(IEditorPart editor, String mode) {
		IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
		if(resource != null) {
			launchBndProject(resource.getProject(), mode);
		}
	}
	void launchBndProject(IProject project, String mode) {
		List<ILaunchConfiguration> configs = getLaunchConfigurations(project);
		if(configs != null && !configs.isEmpty()) {
			DebugUITools.launch(configs.get(0), mode);
		} else {
			try {
				ILaunchConfiguration newConfig = createLaunchConfiguration(project);
				DebugUITools.openLaunchConfigurationDialog(getShell(), newConfig, LAUNCH_GROUP_ID, null);
			} catch (CoreException e) {
				ErrorDialog.openError(getShell(), "Launch Bnd OSGi Project", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error creating launch configuration.", e));
			}
		}
	}
	List<ILaunchConfiguration> getLaunchConfigurations(IProject project) {
		ILaunchManager mgr = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType configType = mgr.getLaunchConfigurationType(LaunchConfigurationDelegate.LAUNCH_CONFIG_TYPE);
		
		try {
			ILaunchConfiguration[] configs = mgr.getLaunchConfigurations(configType);
			List<ILaunchConfiguration> result = new ArrayList<ILaunchConfiguration>(configs.length);
			
			for (ILaunchConfiguration config : configs) {
				String projectName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
				if(projectName != null && projectName.equals(project.getName())) {
					result.add(config);
				}
			}
			return result;
		} catch (CoreException e) {
			ErrorDialog.openError(getShell(), "Launch Bnd OSGi Project", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error reading launch configurations.", e));
			return Collections.emptyList();
		}
	}
	ILaunchConfiguration createLaunchConfiguration(IProject project) throws CoreException {
		ILaunchManager mgr = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType configType = mgr.getLaunchConfigurationType(LaunchConfigurationDelegate.LAUNCH_CONFIG_TYPE);
		
		ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, mgr.generateLaunchConfigurationName(project.getName()));
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
		
		return wc.doSave();
	}
	public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
		Set<ILaunchConfiguration> result = new HashSet<ILaunchConfiguration>();
		if(selection instanceof IStructuredSelection) {
			Iterator<?> iter = ((IStructuredSelection) selection).iterator();
			while(iter.hasNext()) {
				Object item = iter.next();
				if(item instanceof IResource) {
					IResource resource = (IResource) item;
					result.addAll(getLaunchConfigurations(resource.getProject()));
				} else if(item instanceof IAdaptable) {
					IResource resource = (IResource) ((IAdaptable) item).getAdapter(IResource.class);
					if(resource != null) {
						result.addAll(getLaunchConfigurations(resource.getProject()));
					}
				}
			}
		}
		return (ILaunchConfiguration[]) result.toArray(new ILaunchConfiguration[result.size()]);
	}
	public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editor) {
		List<ILaunchConfiguration> result;
		IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
		if(resource != null) {
			result = getLaunchConfigurations(resource.getProject());
		} else {
			result = Collections.emptyList();
		}
		return (ILaunchConfiguration[]) result.toArray(new ILaunchConfiguration[result.size()]);
	}
	public IResource getLaunchableResource(ISelection selection) {
		return null;
	}
	public IResource getLaunchableResource(IEditorPart editorpart) {
		return null;
	}
	static Shell getShell() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		return window != null ? window.getShell() : null;
	}
}
