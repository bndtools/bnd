package bndtools.wizards.bndfile;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import bndtools.Plugin;

public class ExecutableJarWizardPage extends WizardPage {

	private static final String			PREF_PREFIX						= "lastExecExport.";
	private static final String			PREF_LAST_EXPORT_IS_FOLDER		= PREF_PREFIX + "isFolder";
	private static final String			PREF_LAST_EXPORT_JAR_PATH		= PREF_PREFIX + "jarPath";
	private static final String			PREF_LAST_EXPORT_FOLDER_PATH	= PREF_PREFIX + "folderPath";

	private final PropertyChangeSupport	propSupport						= new PropertyChangeSupport(this);

	private boolean						jar								= true;
	private boolean						folder							= false;

	private String						jarPath;
	private String						folderPath;

	private Text						txtJarPath;
	private Text						txtFolderPath;
	private Button						btnBrowseJar;
	private Button						btnBrowseFolder;

	/**
	 * Create the wizard.
	 */
	public ExecutableJarWizardPage() {
		super("standaloneExportDestination");
		setTitle("Export Destination");
		setDescription("Configure the destination for export");
	}

	/**
	 * Create contents of the wizard.
	 *
	 * @param parent
	 */
	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);

		setControl(container);
		container.setLayout(new GridLayout(1, false));

		Group grpDestination = new Group(container, SWT.NONE);
		grpDestination.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		grpDestination.setText("Destination:");
		grpDestination.setLayout(new GridLayout(3, false));

		final Button btnJar = new Button(grpDestination, SWT.RADIO);
		btnJar.setText("Export to JAR:");
		btnJar.setSelection(jar);

		txtJarPath = new Text(grpDestination, SWT.BORDER);
		txtJarPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtJarPath.setText(jarPath != null ? jarPath : "");

		btnBrowseJar = new Button(grpDestination, SWT.NONE);
		btnBrowseJar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnBrowseJar.setText("Browse");

		final Button btnFolder = new Button(grpDestination, SWT.RADIO);
		btnFolder.setText("Export to folder:");
		btnFolder.setSelection(folder);

		txtFolderPath = new Text(grpDestination, SWT.BORDER);
		txtFolderPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtFolderPath.setText(folderPath != null ? folderPath : "");

		btnBrowseFolder = new Button(grpDestination, SWT.NONE);
		btnBrowseFolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnBrowseFolder.setText("Browse");

		btnBrowseFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				String path = dialog.open();
				if (path != null)
					txtFolderPath.setText(path);
			}
		});

		loadLastExport();
		updateEnablement();
		validate();

		Listener listener = event -> {
			jar = btnJar.getSelection();
			jarPath = txtJarPath.getText();

			folder = btnFolder.getSelection();
			folderPath = txtFolderPath.getText();

			updateEnablement();
			validate();
		};

		txtJarPath.addListener(SWT.Modify, listener);
		btnJar.addListener(SWT.Selection, listener);
		txtFolderPath.addListener(SWT.Modify, listener);
		btnFolder.addListener(SWT.Selection, listener);

		btnBrowseJar.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
				dialog.setFilterExtensions(new String[] {
					"*.jar"
				});
				dialog.setFilterNames(new String[] {
					"JAR Files"
				});
				String path = dialog.open();
				if (path != null)
					txtJarPath.setText(path);
			}
		});
	}

	private void loadLastExport() {
		IPreferenceStore prefs = Plugin.getDefault()
			.getPreferenceStore();
		folder = prefs.getBoolean(PREF_LAST_EXPORT_IS_FOLDER);

		jarPath = prefs.getString(PREF_LAST_EXPORT_JAR_PATH);
		if (jarPath != null)
			txtJarPath.setText(jarPath);

		folderPath = prefs.getString(PREF_LAST_EXPORT_FOLDER_PATH);
		if (folderPath != null)
			txtFolderPath.setText(folderPath);
	}

	void saveLastExport() {
		IPreferenceStore prefs = Plugin.getDefault()
			.getPreferenceStore();
		prefs.setValue(PREF_LAST_EXPORT_IS_FOLDER, folder);
		prefs.setValue(PREF_LAST_EXPORT_JAR_PATH, jarPath);
		prefs.setValue(PREF_LAST_EXPORT_FOLDER_PATH, folderPath);
	}

	private void updateEnablement() {
		txtFolderPath.setEnabled(folder);
		btnBrowseFolder.setEnabled(folder);

		txtJarPath.setEnabled(jar);
		btnBrowseJar.setEnabled(jar);
	}

	private void validate() {
		String path = folder ? folderPath : jarPath;

		boolean valid = true;
		String error = null;
		String warning = null;

		if (path == null || path.length() == 0) {
			valid = false;
		} else if (!Path.EMPTY.isValidPath(path)) {
			valid = false;
			error = "Invalid path: " + path;
		} else {
			File file = new File(path);
			if (file.exists()) {
				if (folder) {
					valid = false;
					error = "Path already exists, will not overwrite: " + path;
				} else if (!file.isFile()) {
					valid = false;
					error = "Path already exists and is not a plain file: " + path;
				} else {
					warning = "Path already exists, contents will be overwritten.";
				}
			}
		}

		setPageComplete(valid);
		setErrorMessage(error);
		setMessage(warning, WARNING);
	}

	public boolean isFolder() {
		return folder;
	}

	public void setFolder(boolean folder) {
		this.folder = folder;
	}

	public boolean isJar() {
		return jar;
	}

	public void setJar(boolean jar) {
		this.jar = jar;
	}

	public String getFolderPath() {
		return folderPath;
	}

	public void setFolderPath(String folderPath) {
		this.folderPath = folderPath;
	}

	public String getJarPath() {
		return jarPath;
	}

	public void setJarPath(String jarPath) {
		this.jarPath = jarPath;
	}

	public void addPropertyChangeListener(PropertyChangeListener var0) {
		propSupport.addPropertyChangeListener(var0);
	}

	public void addPropertyChangeListener(String var0, PropertyChangeListener var1) {
		propSupport.addPropertyChangeListener(var0, var1);
	}

	public void removePropertyChangeListener(PropertyChangeListener var0) {
		propSupport.removePropertyChangeListener(var0);
	}

	public void removePropertyChangeListener(String var0, PropertyChangeListener var1) {
		propSupport.removePropertyChangeListener(var0, var1);
	}

}
