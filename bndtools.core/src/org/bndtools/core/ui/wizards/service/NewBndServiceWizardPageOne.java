package org.bndtools.core.ui.wizards.service;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ProjectPaths;
import org.bndtools.templating.Template;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.ClasspathEntry;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkingSet;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import aQute.bnd.build.Workspace;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.wizards.project.ProjectTemplateParam;

public class NewBndServiceWizardPageOne extends NewJavaProjectWizardPageOne {

	protected final ServiceApiProjectNameGroup		nameGroup		= new ServiceApiProjectNameGroup();
	protected final ServiceApiProjectLocationGroup	locationGroup	= new ServiceApiProjectLocationGroup("Location");

	protected final NewBndServiceWizardPageOne		parentPageOne;
	protected final String							projectNameSuffix;
	protected String								templateName;

	protected final ServiceTemplateLoaderJob		templateLoadingJob;

	/**
	 * Constructor used with built-in/explicitly-provided templates
	 */
	public NewBndServiceWizardPageOne() {
		this(null);
	}

	/**
	 * Construtor used with named template (type=service-api,service-impl, and
	 * service-consumer)
	 *
	 * @param templateName the name of the template to look for. May be null if
	 *            no template to be found/loaded.
	 */
	public NewBndServiceWizardPageOne(String templateName) {
		this(null, null, templateName);
	}

	protected ServiceTemplateLoaderJob createTemplateLoadingJob() {
		return new ServiceTemplateLoaderJob();
	}

	void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	public NewBndServiceWizardPageOne(NewBndServiceWizardPageOne parentPageOne, String projectNameSuffix,
		String templateName) {
		this.parentPageOne = parentPageOne;
		this.projectNameSuffix = projectNameSuffix;
		this.templateName = templateName;
		this.templateLoadingJob = createTemplateLoadingJob();
		setTitle("Create an OSGi Service");
		nameGroup.addPropertyChangeListener(event -> {
			IStatus status = nameGroup.getStatus();
			if (status.isOK()) {
				setBndPageComplete(true);
				setErrorMessage(null);
				setMessage("Enter a project name and service name.");
				locationGroup.setProjectName(nameGroup.getProjectName());
			} else {
				setBndPageComplete(false);
				setErrorMessage(status.getMessage());
			}
		});

		locationGroup.addPropertyChangeListener(event -> {
			IStatus status = locationGroup.getStatus();
			setBndPageComplete(status.isOK());
			if (status.isOK()) {
				setErrorMessage(null);
				setMessage("Enter a project name and service name.");
			} else {
				setErrorMessage(status.getMessage());
			}
		});
	}

	public String getServiceName() {
		return nameGroup.getServiceName();
	}

	@Override
	public String getProjectName() {
		if (parentPageOne == null) {
			return nameGroup.getProjectName();
		}
		return parentPageOne.nameGroup.getProjectName() + projectNameSuffix;
	}

	@Override
	public IWorkingSet[] getWorkingSets() {
		if (parentPageOne == null) {
			return super.getWorkingSets();
		}
		return parentPageOne.getWorkingSets();
	}

	public String getPackageName() {
		return nameGroup.getPackageName();
	}

	@Override
	public URI getProjectLocationURI() {
		ServiceApiProjectLocationGroup theLocationGroup = ((parentPageOne == null) ? this.locationGroup
			: parentPageOne.locationGroup);
		IPath location = theLocationGroup.getLocation();
		if (isDirectlyInWorkspace(location))
			return null;

		return URIUtil.toURI(location);
	}
	@Override
	public IClasspathEntry[] getDefaultClasspathEntries() {
		IClasspathEntry[] entries = null;
		if (parentPageOne != null) {
			return parentPageOne.getDefaultClasspathEntries();
		} else {
			entries = super.getDefaultClasspathEntries();
		}
		List<IClasspathEntry> result = new ArrayList<>(entries.length + 2);
		result.addAll(Arrays.asList(entries));

		// Add the Bnd classpath container entry
		IPath bndContainerPath = BndtoolsConstants.BND_CLASSPATH_ID;
		IClasspathEntry bndContainerEntry = JavaCore.newContainerEntry(bndContainerPath, false);
		result.add(bndContainerEntry);

		return result.toArray(new IClasspathEntry[0]);
	}
	@Override
	public String getCompilerCompliance() {
		if (parentPageOne == null) {
			return super.getCompilerCompliance();
		}
		return parentPageOne.getCompilerCompliance();
	}

	private static boolean isDirectlyInWorkspace(IPath location) {
		File wslocation = Platform.getLocation()
			.toFile();
		return location.toFile()
			.getAbsoluteFile()
			.getParentFile()
			.equals(wslocation);
	}

	@Override
	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.
	 * widgets .Composite) This has been cut and pasted from the superclass
	 * because we wish to customize the contents of the page.
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		final Composite composite = new Composite(parent, SWT.NULL);
		composite.setFont(parent.getFont());
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		nameControl = nameGroup.createControl(composite);
		nameControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control locationControl = locationGroup.createControl(composite);
		locationControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control jreControl = createJRESelectionControl(composite);
		jreControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control workingSetControl = createWorkingSetControl(composite);
		workingSetControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// The following is a work-around for the problem
		// described in https://github.com/bndtools/bnd/issues/5239
		Control moduleControl = createModuleControl(composite);
		// Set the module control to invisible
		moduleControl.setVisible(false);
		GridData moduleGridData = new GridData(GridData.FILL_HORIZONTAL);
		// setup moduleGridData to *exclude* so that it doesn't take
		// up any space
		moduleGridData.exclude = true;
		moduleControl.setLayoutData(moduleGridData);

		Control infoControl = createInfoControl(composite);
		infoControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		setControl(composite);
	}

	@Override
	public void setVisible(boolean visible) {
		String failMessage = null;
		if (visible) {
			if (this.templateName != null) {
				try {
					// load templates here
					this.templateLoadingJob.loadTemplates(getContainer(), getShell().getDisplay());
				} catch (InvocationTargetException e) {
					failMessage = e.getMessage();
				}
				// then check to make sure that service api, impl and consumer
				// template types (with
				// name templateName) are all non-null/found.
				Template apiTemplate = getServiceApiTemplate();
				if (apiTemplate == null) {
					failMessage = "Error loading service-api template '" + this.templateName + "'";
				}
				Template implTemplate = getServiceImplTemplate();
				if (implTemplate == null) {
					failMessage = "Error loading service-impl template '" + this.templateName + "'";
				}
				Template consumerTemplate = getServiceConsumerTemplate();
				if (consumerTemplate == null) {
					failMessage = "Error loading service-consumer template '" + this.templateName + "'";
				}
			}
		}
		if (failMessage != null) {
			ErrorDialog.openError(getShell(), failMessage,
				"Could not load service template '" + "'\n" + "\n" + "This results from \n"
					+ "a) Attempting to create OSGi projects before creating a Bnd OSGi workspace\n"
					+ "   Fo fix, first create a Bnd OSGi workspace before creating new OSGi projects\n\n"
					+ "b) Attempting to create Remote Service projects without using the ECF \n"
					+ "   Remote Services Workspace template\n"
					+ "   When creating a Bnd Workspace, use the ECF Remote Services Workspace template\n"
					+ "   at https://github.com/ECF/bndtools.workspace.  This workspace template\n"
					+ "   provides all of the remote service Bnd OSGi Project templates",
				null, ERROR);
			// Now close dialog
			IWizardContainer wizardContainer = getWizard().getContainer();
			if (wizardContainer instanceof WizardDialog) {
				WizardDialog wizardDialog = (WizardDialog) wizardContainer;
				wizardDialog.close();
			}
		} else {
			// Everything ok, show
			super.setVisible(visible);
			setMessage("Enter a project name and then a service name.");
			if (visible) {
				nameControl.setFocus();
			}
		}
	}

	protected Optional<Template> getServiceTemplateWithName(String serviceTemplateType, String templateName) {
		Set<Template> ofType = templateLoadingJob.getTemplates()
			.get(serviceTemplateType);
		if (ofType == null) {
			return Optional.empty();
		}
		return ofType.stream().dropWhile(t -> !templateName.equals(t.getName())).findAny();
	}

	public Template getServiceApiTemplate() {
		Optional<Template> o = getServiceTemplateWithName(ServiceTemplateConstants.TEMPLATE_SERVICE_API_TYPE,
			this.templateName);
		return o.isEmpty()?null:o.get();
	}

	public Template getServiceImplTemplate() {
		Optional<Template> o = getServiceTemplateWithName(ServiceTemplateConstants.TEMPLATE_SERVICE_IMPL_TYPE,
			this.templateName);
		return o.isEmpty()?null:o.get();
	}

	public Template getServiceConsumerTemplate() {
		Optional<Template> o = getServiceTemplateWithName(ServiceTemplateConstants.TEMPLATE_SERVICE_CONSUMER_TYPE,
			this.templateName);
		return o.isEmpty()?null:o.get();
	}

	private static final IClasspathAttribute	TEST	= JavaCore.newClasspathAttribute("test",
		Boolean.TRUE.toString());

	protected Control							nameControl;

	public ProjectPaths getProjectsPaths() {
		try {
			Workspace workspace = Central.getWorkspace();
			return new ProjectPaths(workspace.toString(), workspace);
		} catch (Exception e) {
			return ProjectPaths.DEFAULT;
		}
	}

	@Override
	public IClasspathEntry[] getSourceClasspathEntries() {
		IPath projectPath = new Path(getProjectName()).makeAbsolute();

		ProjectPaths projectPaths = getProjectsPaths();

		List<IClasspathEntry> newEntries = new ArrayList<>(2);

		for (String src : projectPaths.getSrcs()) {
			IPath srcPath = projectPath.append(src);
			IPath binPath = projectPath.append(projectPaths.getBin());
			IClasspathEntry newSourceEntry = JavaCore.newSourceEntry(srcPath, //
				ClasspathEntry.INCLUDE_ALL, //
				ClasspathEntry.EXCLUDE_NONE, //
				binPath, //
				ClasspathEntry.NO_EXTRA_ATTRIBUTES);
			newEntries.add(newSourceEntry);
		}

		boolean enableTestSrcDir;
		try {
			Template template = getServiceApiTemplate();
			if (template == null)
				enableTestSrcDir = true;
			else {
				ObjectClassDefinition templateMeta = template.getMetadata();
				enableTestSrcDir = findAttribute(templateMeta, ProjectTemplateParam.TEST_SRC_DIR.getString()) != null;
			}
		} catch (Exception e) {
			Plugin.getDefault()
				.getLog()
				.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error accessing template parameters", e));
			enableTestSrcDir = true;
		}
		if (enableTestSrcDir) {
			for (String testSrc : projectPaths.getTestSrcs()) {
				IPath testSrcPath = projectPath.append(testSrc);
				IPath testBinPath = projectPath.append(projectPaths.getTestBin());
				IClasspathEntry newSourceEntry = JavaCore.newSourceEntry(testSrcPath, //
					ClasspathEntry.INCLUDE_ALL, //
					ClasspathEntry.EXCLUDE_NONE, //
					testBinPath, //
					new IClasspathAttribute[] {
						TEST
					});
				newEntries.add(newSourceEntry);
			}
		}
		return newEntries.toArray(new IClasspathEntry[0]);
	}

	private AttributeDefinition findAttribute(ObjectClassDefinition ocd, String name) {
		AttributeDefinition[] attDefs = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);

		if (attDefs == null)
			return null;

		for (AttributeDefinition attDef : attDefs) {
			if (name.equals(attDef.getName()))
				return attDef;
		}
		return null;
	}

	@Override
	public IPath getOutputLocation() {
		return new Path(getProjectName()).makeAbsolute()
			.append(ProjectPaths.DEFAULT.getBin());
	}

	// The following override is a work-around for the problem
	// described in https://github.com/bndtools/bnd/issues/5239
	@Override
	public void setPageComplete(boolean complete) {
		// This now does nothing, so that superclass
		// verification can be made irrelevant
		// See
		// https://github.com/bndtools/bnd/issues/5239#issuecomment-1399331939
	}

	// This override of the NewJavaProjectWizardPageOne always returns
	// false so that module info is never created for Bnd project
	@Override
	public boolean getCreateModuleInfoFile() {
		return false;
	}

	protected void setBndPageComplete(boolean complete) {
		super.setPageComplete(complete);
	}

}
