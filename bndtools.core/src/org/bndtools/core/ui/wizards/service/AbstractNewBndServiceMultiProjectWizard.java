package org.bndtools.core.ui.wizards.service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.builder.BuildErrorDetailsHandler;
import org.bndtools.templating.Resource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.Template;
import org.bndtools.utils.workspace.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.wizards.JavaProjectWizard;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import aQute.bnd.build.Project;
import aQute.bnd.build.model.BndEditModel;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.editor.model.BndProject;
import bndtools.wizards.project.ProjectTemplateParam;

public abstract class AbstractNewBndServiceMultiProjectWizard extends JavaProjectWizard {

	private static final ILogger			logger	= Logger.getLogger(AbstractNewBndServiceMultiProjectWizard.class);

	protected NewBndServiceWizardPageOne	pageOne;
	protected NewJavaProjectWizardPageTwo	pageTwo;

	protected AbstractNewBndServiceMultiProjectWizard(NewBndServiceWizardPageOne pageOne,
		NewJavaProjectWizardPageTwo pageTwo) {
		super(pageOne, pageTwo);
		setNeedsProgressMonitor(true);
		this.pageOne = pageOne;
		this.pageTwo = pageTwo;
	}

	@Override
	public void addPages() {
		addPage(pageOne);
		addPage(pageTwo);
	}

	protected BndEditModel generateBndModel(IProgressMonitor monitor) {
		try {
			return new BndEditModel(Central.getWorkspace());
		} catch (Exception e) {
			logger.logInfo("Unable to create BndEditModel with Workspace, defaulting to without Workspace", e);
			return new BndEditModel();
		}
	}

	/**
	 * Allows for an IProjectTemplate to modify the new Bnd project
	 *
	 * @param monitor
	 */
	protected BndProject generateBndProject(IProject project, IProgressMonitor monitor) {
		return new BndProject(project);
	}

	protected void generateProjectContent(IProject project, Template template, Map<String, String> params,
		IProgressMonitor monitor) throws IOException {
		// Set to current project name
		String projectName = project.getName();
		params.put(ProjectTemplateParam.PROJECT_NAME.getString(), projectName);
		params.put(ProjectTemplateParam.BASE_PACKAGE_DIR.getString(), projectName.replace('.', '/'));
		params.put(ProjectTemplateParam.BASE_PACKAGE_NAME.getString(), projectName);

		Map<String, List<Object>> templateParams = new HashMap<>();
		for (Entry<String, String> param : params.entrySet()) {
			templateParams.put(param.getKey(), Collections.<Object> singletonList(param.getValue()));
		}
		try {
			ResourceMap outputs;
			if (template != null) {
				outputs = template.generateOutputs(templateParams);
			} else {
				outputs = new ResourceMap(); // empty
			}

			SubMonitor progress = SubMonitor.convert(monitor, outputs.size() * 3);
			for (Entry<String, Resource> outputEntry : outputs.entries()) {
				String path = outputEntry.getKey();
				Resource resource = outputEntry.getValue();

				// Strip leading slashes from path
				while (path.startsWith("/"))
					path = path.substring(1);

				switch (resource.getType()) {
					case Folder :
						if (!path.isEmpty()) {
							IFolder folder = project.getFolder(path);
							FileUtils.mkdirs(folder, progress.split(1, SubMonitor.SUPPRESS_ALL_LABELS));
						}
						break;
					case File :
						IFile file = project.getFile(path);
						FileUtils.mkdirs(file.getParent(), progress.split(1, SubMonitor.SUPPRESS_ALL_LABELS));
						try (InputStream in = resource.getContent()) {
							if (file.exists())
								file.setContents(in, 0, progress.split(1, SubMonitor.SUPPRESS_NONE));
							else
								file.create(in, 0, progress.split(1, SubMonitor.SUPPRESS_NONE));
							file.setCharset(resource.getTextEncoding(), progress.split(1));
						}
						break;
					default :
						throw new IllegalArgumentException("Unknown resource type " + resource.getType());
				}
			}
		} catch (Exception e) {
			String message = MessageFormat.format("Error generating project contents from template \"{0}\": {1}",
				template != null ? template.getName() : "<null>", e.getMessage());
			throw new IOException(message);
		}
	}

	protected IJavaProject getServiceApiJavaProject() {
		return (IJavaProject) getCreatedElement();
	}

	protected abstract Map<String, String> getServiceTemplateParams();

	protected abstract Template getServiceApiTemplate();

	protected abstract Template getServiceImplTemplate();

	protected abstract IJavaProject getServiceImplJavaProject();

	protected abstract Template getServiceConsumerTemplate();

	protected abstract IJavaProject getServiceConsumerJavaProject();

	protected boolean generateProjectContent(IJavaProject javaProject, Template template,
		Map<String, String> templateParams) {
		if (template == null) {
			return false;
		}
		boolean result = true;
		try {
			// Run using the progress bar from the wizard dialog
			getContainer().run(false, false, monitor -> {
				try {
					// Make changes to the project
					final IWorkspaceRunnable op = monitor1 -> {
						try {
							generateProjectContent(javaProject.getProject(), template, templateParams, monitor1);
						} catch (Exception e) {
							throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
								"Error generating project content from template", e));
						}
					};
					javaProject.getProject()
						.getWorkspace()
						.run(op, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			});
			result = true;
		} catch (InvocationTargetException e) {
			Throwable targetException = e.getTargetException();
			final IStatus status;
			if (targetException instanceof CoreException) {
				status = ((CoreException) targetException).getStatus();
			} else {
				status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error creating bnd project contents",
					targetException);
			}
			logger.logStatus(status);
			ErrorDialog.openError(getShell(), "Error", "Error creating bnd project", status);
			result = false;
		} catch (InterruptedException e) {
			// Shouldn't happen
		}
		return result;
	}

	protected IEditorPart openEditor(IJavaProject javaProject, String packageName, String className) {
		// Find and then open the type given by the packageName + the className
		try {
			if (packageName != null && className != null) {
				IType type = javaProject.findType(packageName, className);
				if (type != null) {
					IResource r = type.getResource();
					if (r.exists() && r instanceof IFile) {
						return IDE.openEditor(getWorkbench().getActiveWorkbenchWindow()
							.getActivePage(), (IFile) r);
					}
				}
			}
		} catch (JavaModelException | PartInitException e) {
			// Ignore
		}
		return null;
	}

	protected void checkForMissingWorkspace(IJavaProject bndProject) {
		// get bnd.bnd file
		IFile bndFile = bndProject.getProject()
			.getFile(Project.BNDFILE);
		try {
			if (!Central.hasWorkspaceDirectory()) {
				IResource markerTarget = bndFile;
				if (markerTarget == null || markerTarget.getType() != IResource.FILE || !markerTarget.exists())
					markerTarget = bndProject.getProject();
				IMarker marker = markerTarget.createMarker(BndtoolsConstants.MARKER_BND_MISSING_WORKSPACE);
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
				marker.setAttribute(IMarker.MESSAGE,
					"Missing Bnd Workspace. Create a new workspace with the 'New Bnd OSGi Workspace' wizard.");
				marker.setAttribute(BuildErrorDetailsHandler.PROP_HAS_RESOLUTIONS, true);
				marker.setAttribute("$bndType", BndtoolsConstants.MARKER_BND_MISSING_WORKSPACE);
			}
			// check to see if we need to add marker about missing workspace
		} catch (Exception e1) {
			// ignore exceptions, this is best effort to help new users
		}
	}

	protected IEditorPart configureServiceApiProject(IJavaProject serviceJavaProject,
		Map<String, String> templateParams) {
		checkForMissingWorkspace(serviceJavaProject);
		return openEditor(serviceJavaProject, templateParams.get(ProjectTemplateParam.BASE_PACKAGE_NAME.getString()),
			ServiceTemplateConstants
				.getApiClassName(templateParams.get(ServiceProjectTemplateParam.SERVICE_NAME.getString())));
	}

	protected IEditorPart configureImplProject(IJavaProject implJavaProject, Map<String, String> templateParams) {
		checkForMissingWorkspace(implJavaProject);
		return openEditor(implJavaProject, templateParams.get(ProjectTemplateParam.BASE_PACKAGE_NAME.getString()),
			ServiceTemplateConstants
				.getImplClassName(templateParams.get(ServiceProjectTemplateParam.SERVICE_NAME.getString())));
	}

	protected IEditorPart configureConsumerProject(IJavaProject consumerJavaProject,
		Map<String, String> templateParams) {
		checkForMissingWorkspace(consumerJavaProject);
		return openEditor(consumerJavaProject, templateParams.get(ProjectTemplateParam.BASE_PACKAGE_NAME.getString()),
			ServiceTemplateConstants
				.getConsumerClassName(templateParams.get(ServiceProjectTemplateParam.SERVICE_NAME.getString())));
	}

	@Override
	public boolean performFinish() {
		boolean result = super.performFinish();
		if (result) {
			final Map<String, String> templateParams = getServiceTemplateParams();
			final IJavaProject serviceJavaProject = getServiceApiJavaProject();
			// api project
			IEditorPart serviceApiEditor = null;
			result = generateProjectContent(serviceJavaProject, getServiceApiTemplate(), templateParams);
			if (result) {
				serviceApiEditor = configureServiceApiProject(serviceJavaProject, templateParams);
				// impl project
				IJavaProject implJavaProject = getServiceImplJavaProject();
				result = generateProjectContent(implJavaProject, getServiceImplTemplate(), templateParams);
				if (result) {
					configureImplProject(implJavaProject, templateParams);
					// consumer project
					IJavaProject consumerJavaProject = getServiceConsumerJavaProject();
					result = generateProjectContent(consumerJavaProject, getServiceConsumerTemplate(), templateParams);
					if (result) {
						configureConsumerProject(consumerJavaProject, templateParams);
					}
				}
			}
			if (serviceApiEditor != null) {
				getWorkbench().getActiveWorkbenchWindow()
					.getActivePage()
					.activate(serviceApiEditor);
			}
			// Finally refresh all projects
			try {
				Central.getWorkspace()
					.refreshProjects();
			} catch (Exception e) {}
		}
		return result;
	}

}
