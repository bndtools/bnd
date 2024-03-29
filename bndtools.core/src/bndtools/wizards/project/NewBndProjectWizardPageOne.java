package bndtools.wizards.project;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import aQute.bnd.build.Workspace;
import bndtools.Plugin;
import bndtools.central.Central;

public class NewBndProjectWizardPageOne extends NewJavaProjectWizardPageOne {

	private final ProjectNameGroup		nameGroup		= new ProjectNameGroup();
	private final ProjectLocationGroup	locationGroup	= new ProjectLocationGroup("Location");
	private Template					template;

	protected void setBndPageComplete(boolean complete) {
		super.setPageComplete(complete);
	}

	NewBndProjectWizardPageOne() {
		setTitle("Create a Bnd OSGi Project");

		nameGroup.addPropertyChangeListener(event -> {
			IStatus status = nameGroup.getStatus();
			if (status.isOK()) {
				setBndPageComplete(true);
				setErrorMessage(null);
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
			} else {
				setErrorMessage(status.getMessage());
			}
		});
	}

	@Override
	public String getProjectName() {
		return nameGroup.getProjectName();
	}

	public String getPackageName() {
		return nameGroup.getPackageName();
	}

	@Override
	public URI getProjectLocationURI() {
		IPath location = locationGroup.getLocation();
		if (isDirectlyInWorkspace(location))
			return null;

		return URIUtil.toURI(location);
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
		super.setVisible(visible);
		if (visible) {
			nameControl.setFocus();
		}
	}

	@Override
	public IClasspathEntry[] getDefaultClasspathEntries() {
		IClasspathEntry[] entries = super.getDefaultClasspathEntries();
		List<IClasspathEntry> result = new ArrayList<>(entries.length + 2);
		result.addAll(Arrays.asList(entries));

		// Add the Bnd classpath container entry
		IPath bndContainerPath = BndtoolsConstants.BND_CLASSPATH_ID;
		IClasspathEntry bndContainerEntry = JavaCore.newContainerEntry(bndContainerPath, false);
		result.add(bndContainerEntry);

		return result.toArray(new IClasspathEntry[0]);
	}

	private static final IClasspathAttribute	TEST	= JavaCore.newClasspathAttribute("test",
		Boolean.TRUE.toString());
	private Control								nameControl;

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

	public void setTemplate(Template template) {
		this.template = template;
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
}
