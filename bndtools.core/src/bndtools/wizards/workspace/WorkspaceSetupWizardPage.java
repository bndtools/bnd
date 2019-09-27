package bndtools.wizards.workspace;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.build.Project;
import bndtools.Plugin;

public class WorkspaceSetupWizardPage extends WizardPage {

	public static final String			PROP_LOCATION		= "location";
	public static final String			PROP_CLEAN_BUILD	= "cleanBuild";

	private final PropertyChangeSupport	propSupport			= new PropertyChangeSupport(this);
	private final WorkspaceLocationPart	locationPart		= new WorkspaceLocationPart();

	private LocationSelection			location			= LocationSelection.WORKSPACE;
	private boolean						cleanBuild			= true;

	private Image						bannerImg;

	public WorkspaceSetupWizardPage() {
		super("Workspace Setup");
	}

	@Override
	public void createControl(Composite parent) {
		setTitle("Setup Bnd Workspace");
		setDescription("Create a workspace folder with initial configuration");
		setImageDescriptor(Plugin.imageDescriptorFromPlugin("icons/bndtools-wizban.png")); //$NON-NLS-1$

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.verticalSpacing = 20;
		composite.setLayout(layout);
		setControl(composite);

		bannerImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "banner.png")
			.createImage(parent.getDisplay());
		Label lblBanner = new Label(composite, SWT.NONE);
		lblBanner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		lblBanner.setImage(bannerImg);

		Control locationControl = locationPart.createControl(composite);
		locationControl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		locationPart.addPropertyChangeListener(WorkspaceLocationPart.PROP_LOCATION, evt -> {
			LocationSelection oldLocation = location;
			location = locationPart.getLocation();
			propSupport.firePropertyChange(PROP_LOCATION, oldLocation, location);
			updateUI();
		});

		Group buildGroup = new Group(composite, SWT.NONE);
		buildGroup.setText("Build");
		buildGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		buildGroup.setLayout(new GridLayout(1, false));

		final Button btnCleanBuild = new Button(buildGroup, SWT.CHECK);
		btnCleanBuild.setText("Clean workspace after import (Recommended)");
		btnCleanBuild.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		btnCleanBuild.setSelection(cleanBuild);

		btnCleanBuild.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cleanBuild = btnCleanBuild.getSelection();
				updateUI();
			}
		});

		updateUI();
	}

	private void updateUI() {
		String warning = cleanBuild ? null : "Existing Bnd projects may not build until the workspace is cleaned.";

		// Check for existing workspace/cnf
		IProject cnfProject = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProject(Project.BNDCNF);
		if (cnfProject != null && cnfProject.exists()) {
			File cnfDir = cnfProject.getLocation()
				.toFile();
			warning = String.format(
				"This Eclipse workspace is already configured as a Bnd workspace. You will not be able to create or import a Bnd workspace from elsewhere.",
				cnfDir);
		}
		setMessage(warning, WARNING);

		String locationError = location.validate();
		setErrorMessage(locationError);
		setPageComplete(locationError == null);
	}

	@Override
	public void dispose() {
		super.dispose();
		bannerImg.dispose();
	}

	public void setLocation(LocationSelection location) {
		this.location = location;
		locationPart.setLocation(location);
	}

	public LocationSelection getLocation() {
		return location;
	}

	public void setCleanBuild(boolean cleanBuild) {
		this.cleanBuild = cleanBuild;
	}

	public boolean isCleanBuild() {
		return cleanBuild;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propSupport.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propSupport.removePropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propSupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propSupport.removePropertyChangeListener(propertyName, listener);
	}

}
