package bndtools.wizards.workspace;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import aQute.bnd.build.Project;
import bndtools.utils.ModificationLock;

public class WorkspaceLocationPart {

	static final String					PROP_LOCATION	= "location";

	private final PropertyChangeSupport	propertySupport	= new PropertyChangeSupport(this);
	private final ModificationLock		modifyLock		= new ModificationLock();

	private LocationSelection			location		= LocationSelection.WORKSPACE;
	private Runnable					updateFields;
	private Runnable					updateEnablement;
	private Group						group;

	public Control createControl(final Composite parent) {
		// Create Controls
		group = new Group(parent, SWT.NONE);
		group.setText("Location");
		GridLayout layout = new GridLayout(3, false);
		group.setLayout(layout);

		File workspace = getUpdate();
		if (workspace == null) {

			final Button btnCreateInEclipseWorkspace = new Button(group, SWT.RADIO);
			btnCreateInEclipseWorkspace.setText("Create in current Eclipse Workspace");
			btnCreateInEclipseWorkspace.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));

			Label lblEclipseWorkspace = new Label(group, SWT.NONE);
			lblEclipseWorkspace.setText(ResourcesPlugin.getWorkspace()
				.getRoot()
				.getLocation()
				.toOSString());
			lblEclipseWorkspace.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));

			final Button btnCreateExternal = new Button(group, SWT.RADIO);
			btnCreateExternal.setText("Create in:");

			final Text txtExternalLocation = new Text(group, SWT.BORDER);
			txtExternalLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			final Button btnBrowseExternal = new Button(group, SWT.PUSH);
			btnBrowseExternal.setText("Browse");

			updateFields = () -> {
				btnCreateInEclipseWorkspace.setSelection(location.eclipseWorkspace);
				btnCreateExternal.setSelection(!location.eclipseWorkspace);
				txtExternalLocation.setText(location.externalPath != null ? location.externalPath : "");
			};
			updateEnablement = () -> {
				txtExternalLocation.setEnabled(!location.eclipseWorkspace);
				btnBrowseExternal.setEnabled(!location.eclipseWorkspace);
			};

			// Load initial state
			updateFields.run();
			updateEnablement.run();

			// Event listeners
			final Listener locationListener = ev -> modifyLock.modifyOperation(() -> setLocation(
				new LocationSelection(btnCreateInEclipseWorkspace.getSelection(), txtExternalLocation.getText())));
			btnCreateExternal.addListener(SWT.Selection, locationListener);
			btnCreateInEclipseWorkspace.addListener(SWT.Selection, locationListener);
			txtExternalLocation.addListener(SWT.Modify, locationListener);

			btnBrowseExternal.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					DirectoryDialog dialog = new DirectoryDialog(parent.getShell());
					String path = dialog.open();
					if (path != null)
						txtExternalLocation.setText(path);
				}
			});
		} else {
			final Label txtUpdateLocation = new Label(group, SWT.BORDER);
			txtUpdateLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			txtUpdateLocation.setText("Update in current Bnd Workspace");

			Label lblEclipseWorkspace = new Label(group, SWT.NONE);
			lblEclipseWorkspace.setText(workspace.getAbsolutePath());
			lblEclipseWorkspace.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
			setLocation(new LocationSelection(false, workspace.getAbsolutePath()));
		}

		return group;
	}

	private File getUpdate() {
		IProject cnfProject = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProject(Project.BNDCNF);
		if (cnfProject != null && cnfProject.exists()) {
			File cnf = cnfProject.getLocation()
				.toFile();
			if (cnf == null || !cnf.exists())
				return null;

			return cnf.getParentFile();
		}
		return null;
	}

	public LocationSelection getLocation() {
		return location;
	}

	public void setLocation(LocationSelection location) {
		LocationSelection oldLoc = this.location;
		this.location = location;
		if (group != null && !group.isDisposed() && updateFields != null) {
			modifyLock.ifNotModifying(updateFields);
			updateEnablement.run();
			propertySupport.firePropertyChange(PROP_LOCATION, oldLoc, location);
		}
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertySupport.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertySupport.removePropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertySupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertySupport.removePropertyChangeListener(propertyName, listener);
	}

}
