package bndtools.wizards.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import bndtools.Plugin;

public class ImportBndWorkspaceWizardPageOne extends WizardPage {

	private Text		txtFolder;
	private Button		deleteButton;
	private Button		inferExecutionEnvironmentButton;
	/**
	 * TableViewer is used to display an image for the project which is not
	 * possible with a ListViewer
	 */
	private TableViewer	tableViewer;

	protected ImportBndWorkspaceWizardPageOne(String pageName) {
		super(pageName);
		setTitle("Bnd Workspace Projects");
		setDescription("Select a root directory of Bnd Workspace");
	}

	/**
	 * Return the valid Bnd Workspace root, which was specified by the user Uses
	 * UI-Thread, do not call from other Threads
	 *
	 * @return valid folder
	 */
	File getSelectedFolder() {
		if (!isPageComplete()) {
			throw new IllegalStateException(
				"getSelectedFolder cannot be called before wizard-page is marked as complete!");
		}
		return new File(txtFolder.getText());
	}

	/**
	 * Return the selection, if the user wants to delete all existing settings.
	 * Uses UI-Thread, do not call from other Threads
	 *
	 * @return selection-state from checkbox
	 */
	boolean isDeleteSettings() {
		return deleteButton.getSelection();
	}

	/**
	 * Return the selection, if the user wants to infer the
	 * execution-environment from BND {@link Constants#JAVAC_TARGET}. Uses
	 * UI-Thread, do not call from other Threads
	 *
	 * @return selection-state from checkbox
	 */
	boolean isInferExecutionEnvironment() {
		return inferExecutionEnvironmentButton.getSelection();
	}

	@Override
	public void createControl(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NONE);

		Label lblFolder = new Label(container, SWT.NONE);
		lblFolder.setText("Root Directory:");

		txtFolder = new Text(container, SWT.BORDER | SWT.SINGLE);
		txtFolder.addModifyListener(event -> getWizard().getContainer()
			.updateButtons());

		Button btnOpenDialog = new Button(container, SWT.PUSH);
		btnOpenDialog.setText("Browse...");
		btnOpenDialog.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				DirectoryDialog dirDialog = new DirectoryDialog(container.getShell());
				dirDialog.setFilterPath(ResourcesPlugin.getWorkspace()
					.getRoot()
					.getLocation()
					.toOSString());
				dirDialog.setText("Select the folder containing the project.");
				txtFolder.setText(dirDialog.open());
				getWizard().getContainer()
					.updateButtons();
			}
		});

		Label lblProjects = new Label(container, SWT.NONE);
		lblProjects.setText("Projects:");

		tableViewer = new TableViewer(container, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				// configuration project always first
				if (e1 instanceof File && e2 instanceof Project) {
					return -1;
				} else if (e1 instanceof Project && e2 instanceof File) {
					return 1;
				}
				Project p1 = (Project) e1;
				Project p2 = (Project) e2;
				return super.compare(viewer, p1.getName(), p2.getName());
			}
		});
		tableViewer.addSelectionChangedListener(event -> {
			// Disable selection since the tableviewer should be readonly, but
			// not disabled
			if (!event.getSelection()
				.isEmpty()) {
				tableViewer.setSelection(StructuredSelection.EMPTY);
			}
		});
		TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.NONE);
		column.setLabelProvider(new ProjectsColumnLabelProvider());

		Button refreshButton = new Button(container, SWT.PUSH);
		refreshButton.setText("Refresh");
		refreshButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				getWizard().getContainer()
					.updateButtons();
			}
		});

		deleteButton = new Button(container, SWT.CHECK);
		deleteButton.setText("Delete existing settings");

		inferExecutionEnvironmentButton = new Button(container, SWT.CHECK);
		inferExecutionEnvironmentButton.setSelection(true);
		inferExecutionEnvironmentButton.setText("Infer execution-environment (J2SE and JavaSE).");
		inferExecutionEnvironmentButton.setToolTipText(
			"Uses the 'javac.target' from the Bnd Workspace to infer a Execution Environment to the JRE container. If nothing matches, the default JRE will be used.\nExisting containers will be removed.");

		FormLayout layout = new FormLayout();
		container.setLayout(layout);

		FormData fd_lblFolder = new FormData();
		fd_lblFolder.top = new FormAttachment(0, 10);
		fd_lblFolder.left = new FormAttachment(0, 10);
		lblFolder.setLayoutData(fd_lblFolder);

		FormData fd_txtFolder = new FormData();
		fd_txtFolder.top = new FormAttachment(lblFolder, 0, SWT.CENTER);
		fd_txtFolder.left = new FormAttachment(lblFolder, 10);
		fd_txtFolder.right = new FormAttachment(100, -100);
		txtFolder.setLayoutData(fd_txtFolder);

		FormData fd_btnDialog = new FormData();
		fd_btnDialog.top = new FormAttachment(lblFolder, 0, SWT.CENTER);
		fd_btnDialog.left = new FormAttachment(txtFolder, 10);
		fd_btnDialog.right = new FormAttachment(100, -10);
		btnOpenDialog.setLayoutData(fd_btnDialog);

		FormData fd_lblProjects = new FormData();
		fd_lblProjects.top = new FormAttachment(lblFolder, 20);
		fd_lblProjects.left = new FormAttachment(lblFolder, 0, SWT.LEFT);
		lblProjects.setLayoutData(fd_lblProjects);

		FormData fd_table = new FormData();
		fd_table.top = new FormAttachment(lblProjects, 5);
		fd_table.left = new FormAttachment(lblFolder, 0, SWT.LEFT);
		fd_table.right = new FormAttachment(100, -100);
		fd_table.bottom = new FormAttachment(100, -55);
		tableViewer.getTable()
			.setLayoutData(fd_table);

		FormData fd_btnRefresh = new FormData();
		fd_btnRefresh.top = new FormAttachment(tableViewer.getTable(), 0, SWT.TOP);
		fd_btnRefresh.left = new FormAttachment(btnOpenDialog, 0, SWT.LEFT);
		fd_btnRefresh.right = new FormAttachment(100, -10);
		refreshButton.setLayoutData(fd_btnRefresh);

		FormData fd_btnDelete = new FormData();
		fd_btnDelete.top = new FormAttachment(tableViewer.getTable(), 10);
		fd_btnDelete.left = new FormAttachment(lblFolder, 0, SWT.LEFT);
		deleteButton.setLayoutData(fd_btnDelete);

		FormData fd_btnInfer = new FormData();
		fd_btnInfer.top = new FormAttachment(deleteButton, 10);
		fd_btnInfer.left = new FormAttachment(lblFolder, 0, SWT.LEFT);
		inferExecutionEnvironmentButton.setLayoutData(fd_btnInfer);

		getShell().setMinimumSize(470, 450);

		setControl(container);
		setPageComplete(false);

		txtFolder.setText(ResourcesPlugin.getWorkspace()
			.getRoot()
			.getLocation()
			.toOSString());
	}

	@Override
	public boolean canFlipToNextPage() {
		// single page
		return false;
	}

	@Override
	public boolean isPageComplete() {
		return updateWorkspaceSelection();
	}

	/**
	 * Takes the selected/typed folder from the the {@link #txtFolder} and tries
	 * to obtain a valid Bnd Workspace. In any case, the {@link #tableViewer} is
	 * updated.
	 *
	 * @return true, when a Bnd Workspace was selected and properly initialized
	 */
	private boolean updateWorkspaceSelection() {
		final String selectedFolder = txtFolder.getText();
		boolean result = false;
		// check if folder containing a cnf-folder for Bnd was selected
		if (null != selectedFolder && selectedFolder.trim()
			.length() > 0) {
			File chosenDirectory = new File(txtFolder.getText());
			if (chosenDirectory.exists()) {
				final Workspace bndWorkspace;
				try {
					bndWorkspace = Workspace.getWorkspace(chosenDirectory);
					setErrorMessage(null);
					List<Object> tableEntries = new ArrayList<>(bndWorkspace.getAllProjects());
					tableEntries.add(bndWorkspace.getBuildDir());
					tableViewer.setInput(tableEntries);
					result = true;
				} catch (Exception e) {
					// not a valid Bnd Workspace folder
					setErrorMessage(e.getMessage());
				}
			} else {
				// handle non-existing folders
				setErrorMessage("No Workspace found from: " + selectedFolder);
			}
		}
		if (!result) {
			tableViewer.setInput(Collections.emptyList());
		}
		for (TableColumn col : tableViewer.getTable()
			.getColumns()) {
			// make sure TableViewerColumn has enough width to display new
			// selection
			col.pack();
		}
		tableViewer.refresh();
		return result;
	}

	private static final class ProjectsColumnLabelProvider extends ColumnLabelProvider {
		private static final String	KEY_GENERAL_PROJECT				= ProjectsColumnLabelProvider.class.getName();
		private static final String	KEY_GENERAL_PROJECT_GREYSCALE	= KEY_GENERAL_PROJECT + "_grey";
		private static final String	KEY_JAVA_PROJECT				= KEY_GENERAL_PROJECT + "_java";
		private static final String	KEY_JAVA_PROJECT_GREYSCALE		= KEY_JAVA_PROJECT + "_grey";

		private ProjectsColumnLabelProvider() {
			// prepare images (greyscale used for disabled)
			if (Plugin.getDefault()
				.getImageRegistry()
				.get(KEY_GENERAL_PROJECT) == null) {
				Image image = PlatformUI.getWorkbench()
					.getSharedImages()
					.getImage(SharedImages.IMG_OBJ_PROJECT);
				Plugin.getDefault()
					.getImageRegistry()
					.put(KEY_GENERAL_PROJECT, image);
				Plugin.getDefault()
					.getImageRegistry()
					.put(KEY_GENERAL_PROJECT_GREYSCALE, new Image(Display.getCurrent(), image, SWT.IMAGE_GRAY));
			}

			if (Plugin.getDefault()
				.getImageRegistry()
				.get(KEY_JAVA_PROJECT) == null) {
				// use Java-Project image from JDT (unfortunately not shared by
				// JDT-Plugin)
				Image image = AbstractUIPlugin
					.imageDescriptorFromPlugin(JavaUI.ID_PLUGIN, "icons/full/eview16/projects.gif")
					.createImage();
				Plugin.getDefault()
					.getImageRegistry()
					.put(KEY_JAVA_PROJECT, image);
				Plugin.getDefault()
					.getImageRegistry()
					.put(KEY_JAVA_PROJECT_GREYSCALE, new Image(Display.getCurrent(), image, SWT.IMAGE_GRAY));
			}
		}

		@Override
		public String getText(Object element) {
			String text;
			if (element instanceof File) {
				text = ((File) element).getName() + " - configuration project";
			} else {
				text = ((Project) element).getName();
			}

			if (projectExistsInEclipse(element)) {
				text = text + " (project already exists, metadata will updated)";
			}
			return text;
		}

		@Override
		public Color getForeground(Object element) {
			if (projectExistsInEclipse(element)) {
				// maybe check for some "disabled" foreground to work better
				// with theming
				return Display.getCurrent()
					.getSystemColor(SWT.COLOR_GRAY);
			}
			return super.getForeground(element);
		}

		@Override
		public Image getImage(Object element) {
			boolean projectExists = projectExistsInEclipse(element);
			final Image image;
			if (element instanceof File) {
				if (projectExists) {
					image = Plugin.getDefault()
						.getImageRegistry()
						.get(KEY_GENERAL_PROJECT_GREYSCALE);
				} else {
					image = Plugin.getDefault()
						.getImageRegistry()
						.get(KEY_GENERAL_PROJECT);
				}
			} else {
				if (projectExists) {
					image = Plugin.getDefault()
						.getImageRegistry()
						.get(KEY_JAVA_PROJECT_GREYSCALE);
				} else {
					image = Plugin.getDefault()
						.getImageRegistry()
						.get(KEY_JAVA_PROJECT);
				}
			}
			return image;
		}

		private boolean projectExistsInEclipse(Object element) {
			final IProject existingProject;
			if (element instanceof File) {
				File cnfFile = (File) element;
				existingProject = ResourcesPlugin.getWorkspace()
					.getRoot()
					.getProject(cnfFile.getName());
				return existingProject.exists() && existingProject.getLocation()
					.toString()
					.equals(cnfFile.toString());
			}
			Project project = (Project) element;
			existingProject = ResourcesPlugin.getWorkspace()
				.getRoot()
				.getProject(project.getName());
			return existingProject.exists() && existingProject.getLocation()
				.toString()
				.equals(project.getBase()
					.toString());
		}
	}

}
