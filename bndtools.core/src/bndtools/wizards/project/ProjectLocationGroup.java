package bndtools.wizards.project;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.bndtools.utils.swt.SWTUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import aQute.bnd.build.Workspace;
import bndtools.Plugin;
import bndtools.central.Central;

public class ProjectLocationGroup {

	private static final String			PROP_LOCATION					= "location";
	private static final String			PROP_STATUS						= "status";
	private static final String			DIALOGSTORE_LAST_EXTERNAL_LOC	= JavaUI.ID_PLUGIN + ".last.external.project";	//$NON-NLS-1$

	private final PropertyChangeSupport	propSupport						= new PropertyChangeSupport(this);

	private final IPath					workspaceLocation;

	private boolean						useBndWorkspace					= true;
	private String						projectName;
	private IPath						location;
	private String						externalPath;
	private boolean						programmaticChange;
	private IStatus						status							= Status.OK_STATUS;

	private Composite					container;
	private Button						btnUseBndWorkspace;
	private Label						lblOtherLocation;
	private Text						txtLocation;
	private Button						btnBrowse;

	public ProjectLocationGroup(@SuppressWarnings("unused") String title) {
		this.workspaceLocation = findWorkspaceLocation();
		this.location = workspaceLocation;
	}

	private static IPath findWorkspaceLocation() {

		try {
			for (IProject iproj : ResourcesPlugin.getWorkspace()
				.getRoot()
				.getProjects()) {
				if (iproj.getName()
					.equals(Workspace.CNFDIR)) {
					IPath rawLocation = iproj.getRawLocation();
					if (rawLocation == null)
						break;

					return iproj.getRawLocation()
						.removeLastSegments(1);
				}
			}
		} catch (Exception e) {
			// nice try
		}

		IPath p = Platform.getLocation();

		Workspace ws;
		try {
			ws = Central.getWorkspace();
		} catch (Exception e) {
			ws = null;
			for (IProject iproj : ResourcesPlugin.getWorkspace()
				.getRoot()
				.getProjects()) {
				if (iproj.getName()
					.equals(Workspace.CNFDIR)) {
					return iproj.getRawLocation()
						.removeLastSegments(1);
				}
			}
		}

		if (ws != null && !ws.isDefaultWorkspace())
			p = Path.fromOSString(ws.getBase()
				.getAbsolutePath());

		return p;
	}

	/**
	 * @wbp.parser.entryPoint
	 */
	public Control createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));

		btnUseBndWorkspace = new Button(container, SWT.CHECK);
		btnUseBndWorkspace.setText("Use bnd workspace location");
		btnUseBndWorkspace.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));

		lblOtherLocation = new Label(container, SWT.NONE);
		lblOtherLocation.setText("Location:");

		txtLocation = new Text(container, SWT.BORDER);
		txtLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtLocation.setText("[WorkspaceLocation]");

		btnBrowse = new Button(container, SWT.NONE);
		btnBrowse.setText("Browse...");
		btnBrowse.setLayoutData(getButtonLayoutData(btnBrowse));

		updateUI();

		txtLocation.addModifyListener(e -> {
			IPath oldValue = getLocation();
			if (!useBndWorkspace) {
				externalPath = txtLocation.getText();
			}
			IPath newValue = getLocation();
			if (!programmaticChange) {
				propSupport.firePropertyChange(PROP_LOCATION, oldValue, newValue);
				validate();
			}
		});
		btnUseBndWorkspace.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IPath oldValue = getLocation();
				useBndWorkspace = btnUseBndWorkspace.getSelection();
				IPath newValue = getLocation();
				if (!programmaticChange) {
					propSupport.firePropertyChange(PROP_LOCATION, oldValue, newValue);
					updateUI();
					validate();
				}
			}
		});
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(container.getShell());
				dialog.setMessage("Choose a directory for the project contents:");
				String directoryName = txtLocation.getText()
					.trim();
				if (directoryName == null || directoryName.length() == 0) {
					String previous = JavaPlugin.getDefault()
						.getDialogSettings()
						.get(DIALOGSTORE_LAST_EXTERNAL_LOC);
					if (previous != null)
						directoryName = previous;
				}

				assert (directoryName != null);

				if ((directoryName != null) && (directoryName.length() > 0)) {
					File path = new File(directoryName);
					if (path.exists())
						dialog.setFilterPath(directoryName);
				}

				String selected = dialog.open();
				if (selected != null) {
					IPath path = new Path(selected);
					if (projectName != null && !projectName.equals(path.lastSegment()))
						selected = path.append(projectName)
							.toString();
					txtLocation.setText(selected);
				}
			}
		});

		return container;
	}

	private Object getButtonLayoutData(Button button) {
		GridData gd = new GridData();
		gd.widthHint = SWTUtil.getButtonWidthHint(button);
		return gd;
	}

	private IStatus checkStatus() {
		// Check for empty project name
		if (projectName == null || projectName.length() == 0)
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Project name must be specified", null);

		// If external location, ensure path is specified and ends with
		// projectname
		if (!useBndWorkspace) {
			IPath loc = getExternalLocation();
			if (loc == null || loc.isEmpty())
				return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Project location invalid or not specified.",
					null);

			if (loc.lastSegment() == null || !loc.lastSegment()
				.equals(projectName))
				return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
					"Project location must end with specified project name", null);

			if (workspaceLocation != null && !workspaceLocation.isPrefixOf(loc)) {
				return new Status(IStatus.INFO, Plugin.PLUGIN_ID, 0,
					"Project location will NOT work with the Bnd support because it is in a directory other than the cnf directory ("
						+ workspaceLocation + ")",
					null);
			}
		}

		// Check valid name
		IStatus nameStatus = JavaPlugin.getWorkspace()
			.validateName(projectName != null ? projectName : "", IResource.PROJECT);
		if (!nameStatus.isOK())
			return nameStatus;

		// Check if project name already exists
		IProject existingProject = JavaPlugin.getWorkspace()
			.getRoot()
			.getProject(projectName);
		if (existingProject.exists())
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
				MessageFormat.format("Project \"{0}\" already exists.", projectName), null);

		// Find existing project at that location and check if name matches
		IPath projectLocation = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getLocation()
			.append(projectName);
		if (projectLocation.toFile()
			.exists()) {
			try {
				String path = projectLocation.toFile()
					.getCanonicalPath();
				projectLocation = new Path(path);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String existingName = projectLocation.lastSegment();
			if (!existingName.equals(projectName))
				return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
					MessageFormat.format("The name of the new project must be ''{0}''", existingName), null);
		}

		return Status.OK_STATUS;
	}

	private void validate() {
		IStatus oldStatus = status;
		status = checkStatus();
		propSupport.firePropertyChange(PROP_STATUS, oldStatus, status);
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
		IPath old = location;
		location = buildProjectLocation(projectName);

		try {
			programmaticChange = true;
			updateUI();
		} finally {
			programmaticChange = false;
		}
		propSupport.firePropertyChange(PROP_LOCATION, old, location);
		validate();
	}

	public IPath getLocation() {
		IPath path = useBndWorkspace ? location : getExternalLocation();
		return path;
	}

	private IPath getExternalLocation() {
		IPath path;
		if (externalPath != null && externalPath.length() > 0 && Path.EMPTY.isValidPath(externalPath))
			path = Path.fromOSString(externalPath);
		else
			path = null;
		return path;
	}

	public IStatus getStatus() {
		return status;
	}

	private void updateUI() {
		if (btnUseBndWorkspace != null && !btnUseBndWorkspace.isDisposed()) {
			btnUseBndWorkspace.setSelection(useBndWorkspace);
			lblOtherLocation.setEnabled(!useBndWorkspace);
			txtLocation.setEnabled(!useBndWorkspace);
			txtLocation.setText(useBndWorkspace ? location.toOSString() : externalPath == null ? "" : externalPath);
			btnBrowse.setEnabled(!useBndWorkspace);
		}
	}

	private IPath buildProjectLocation(String projectName) {
		IPath result = workspaceLocation;
		if (projectName != null && projectName.length() > 0)
			result = result.append(projectName);
		return result;
	}

	public void addPropertyChangeListener(PropertyChangeListener l) {
		propSupport.addPropertyChangeListener(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		propSupport.removePropertyChangeListener(l);
	}

}
