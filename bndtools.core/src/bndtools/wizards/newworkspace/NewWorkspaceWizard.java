package bndtools.wizards.newworkspace;

import static aQute.bnd.wstemplates.FragmentTemplateEngine.DEFAULT_INDEX;
import static org.eclipse.jface.dialogs.MessageDialog.openConfirm;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.stream.Stream;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.forms.widgets.FormText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Workspace;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.result.Result;
import aQute.bnd.wstemplates.FragmentTemplateEngine;
import aQute.bnd.wstemplates.FragmentTemplateEngine.TemplateInfo;
import aQute.bnd.wstemplates.FragmentTemplateEngine.TemplateUpdater;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.util.ui.UI;

/**
 * Create a new Workspace Wizard.
 */
public class NewWorkspaceWizard extends Wizard implements IImportWizard, INewWizard {

	static final Logger				log					= LoggerFactory.getLogger(NewWorkspaceWizard.class);

	final Model						model;
	final UI<Model>					ui;
	final NewWorkspaceWizardPage	page				= new NewWorkspaceWizardPage();
	final FragmentTemplateEngine	templates;

	final static Image				verified			= Icons.image("icons/tick.png", false);
	final static Image				verifiedGreyedOut	= new Image(Display.getDefault(), verified, SWT.IMAGE_DISABLE);

	public NewWorkspaceWizard() throws Exception {
		setWindowTitle("Create New bnd Workspace");
		Workspace workspace = Central.getWorkspace();
		this.model = new Model(workspace);
		this.ui = new UI<>(model);
		templates = new FragmentTemplateEngine(workspace);
		try {
			Job job = Job.create("load index", mon -> {
				try {
					templates.read(new URL(DEFAULT_INDEX))
						.unwrap()
						.forEach(templates::add);
					Parameters p = workspace.getMergedParameters(Constants.WORKSPACE_TEMPLATES);
					templates.read(p)
						.forEach(templates::add);
					ui.write(() -> model.templates = templates.getAvailableTemplates());
				} catch (Exception e) {
					log.error("failed to read default index {}", e, e);

					Display.getDefault()
						.asyncExec(() -> {

							IStatus status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, "failed to read default index",
								e);
							ErrorDialog.openError(getShell(), "Error", "An error occurred", status);
						});

					Plugin.getDefault()
						.getLog()
						.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
							"failed to read default index " + DEFAULT_INDEX, e));
				}
			});
			job.schedule();
		} catch (Throwable e) {
			log.error("initialization {}", e, e);
			throw Exceptions.duck(e);
		}
	}

	@Override
	public void addPages() {
		addPage(page);
	}

	@Override
	public boolean performFinish() {

		// show a confirmation dialog
		// if there is at least one 3rd-party template selected
		long num3rdParty = model.selectedTemplates.stream()
			.filter(t -> !t.isOfficial())
			.count();

		boolean confirmed = num3rdParty == 0 || openConfirm(getShell(), "Install 3rd-Party templates",
			"You have selected " + num3rdParty + " templates from 3rd-party authors. "
				+ "Are you sure you trust the authors and want to continue fetching the content?");

		if (!confirmed) {
			// not confirmed. cancel selected
			return false;
		}

		if (model.valid == null) {
			ui.write(() -> {
				TemplateUpdater updater = templates.updater(model.location, model.selectedTemplates);
				model.execute(updater);
			});
			return true;
		} else
			return false;

	}

	class NewWorkspaceWizardPage extends WizardPage {
		NewWorkspaceWizardPage() {
			super("New Workspace");
			setTitle("Create New Workspace");
			setDescription("Specify the workspace details and select the template fragments to apply.");
		}

		@Override
		public void createControl(Composite parent) {

			Composite container = new Composite(parent, SWT.NONE);
			setControl(container);
			container.setLayout(new GridLayout(8, false));

			Button useEclipseWorkspace = new Button(container, SWT.CHECK);
			useEclipseWorkspace.setText("Update current Eclipse workspace");
			useEclipseWorkspace.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 8, 1));

			Label locationLabel = new Label(container, SWT.NONE);
			locationLabel.setText("Location");
			locationLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 8, 1));

			Text location = new Text(container, SWT.BORDER);
			location.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 6, 1));

			Button browseButton = new Button(container, SWT.PUSH);
			browseButton.setText("Browse...");
			browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

			Button clean = new Button(container, SWT.CHECK);
			clean.setText("Clean the directory");
			clean.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 8, 1));

			Button switchWorkspace = new Button(container, SWT.CHECK);
			switchWorkspace.setText("Show workspace select dialog to switch to new workspace after finish");
			switchWorkspace.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 8, 1));

			CheckboxTableViewer selectedTemplates = CheckboxTableViewer.newCheckList(container,
				SWT.BORDER | SWT.FULL_SELECTION);
			selectedTemplates.setContentProvider(ArrayContentProvider.getInstance());
			Table table = selectedTemplates.getTable();
			table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 6, 10));
			TableLayout tableLayout = new TableLayout();
			table.setLayout(tableLayout);
			table.setHeaderVisible(true);
			selectedTemplates.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent e) {
					// Handle double click event
					IStructuredSelection selection = (IStructuredSelection) e.getSelection();
					Object el = selection.getFirstElement();
					if (el instanceof TemplateInfo sti) {
						// Open URL in browser
						Program.launch(sti.id()
							.repoUrl());
					}
				}
			});

			TableViewerColumn nameColumn = new TableViewerColumn(selectedTemplates, SWT.NONE);
			nameColumn.getColumn()
				.setText("Name");
			nameColumn.setLabelProvider(new ColumnLabelProvider() {

				@Override
				public String getText(Object element) {
					if (element instanceof TemplateInfo ti) {
						return ti.name();
					}
					return super.getText(element);
				}
			});

			TableViewerColumn descriptionColumn = new TableViewerColumn(selectedTemplates, SWT.NONE);
			descriptionColumn.getColumn()
				.setText("Description");
			descriptionColumn.setLabelProvider(new ColumnLabelProvider() {

				@Override
				public String getText(Object element) {
					if (element instanceof TemplateInfo ti) {
						return ti.description();
					}
					return super.getText(element);
				}
			});

			TableViewerColumn authorColumn = new TableViewerColumn(selectedTemplates, SWT.NONE);
			authorColumn.getColumn()
				.setText("Author");
			authorColumn.setLabelProvider(new ColumnLabelProvider() {

				@Override
				public String getText(Object element) {
					if (element instanceof TemplateInfo ti) {
						if (ti.isOfficial()) {
							return "bndtools (Official)";

						} else {
							return ti.id()
								.organisation() + " (3rd Party)";
						}
					}
					return super.getText(element);
				}

				@Override
				public Image getImage(Object element) {
					if (element instanceof TemplateInfo ti) {
						boolean officialOrSHA = ti.isOfficial() || ti.isCommitSHA();
						return officialOrSHA ? verified : verifiedGreyedOut;
					}

					return super.getImage(element);
				}

			});

			tableLayout.addColumnData(new ColumnWeightData(1, 200, false));
			tableLayout.addColumnData(new ColumnWeightData(10, 460, true));
			tableLayout.addColumnData(new ColumnWeightData(20, 100, true));

			Button addButton = new Button(container, SWT.PUSH);
			addButton.setText("+");
			addButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
			addButton.setToolTipText("Add entry via URI to a template repo index.bnd");

			FormText formText = new FormText(container, SWT.NO_FOCUS | SWT.FILL);
			formText.setText("Double click to open the fragment template Github-Repo in your browser.", false, false);
			formText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 6, 1));

			model.updateWorkspace(true);
			ui.u("location", model.location, UI.text(location)
				.map(File::getAbsolutePath, File::new));
			ui.u("clean", model.clean, UI.checkbox(clean));
			ui.u("updateWorkspace", model.updateWorkspace, UI.checkbox(useEclipseWorkspace))
				.bind(v -> location.setEnabled(!v))
				.bind(v -> browseButton.setEnabled(!v))
				.bind(v -> switchWorkspace.setEnabled(!v))
				.bind(v -> clean.setEnabled(!v))
				.bind(v -> setTitle(
					v ? "Update Workspace with template fragment" : "Create New Workspace from template fragment"))
				.bind(v -> setWindowTitle(v ? "Update Workspace" : "Create New Workspace"));

			ui.u("valid", model.valid, this::setErrorMessage);
			ui.u("error", model.error, this::setErrorMessage);
			ui.u("valid", model.valid, v -> setPageComplete(v == null));
			ui.u("switchWorkspace", model.switchWorkspace, UI.checkbox(switchWorkspace));
			ui.u("templates", model.templates, l -> selectedTemplates.setInput(l.toArray()));
			ui.u("selectedTemplates", model.selectedTemplates, UI.widget(selectedTemplates)
				.map(List::toArray, this::toTemplates));
			UI.checkbox(addButton)
				.subscribe(this::addTemplate);
			UI.checkbox(browseButton)
				.subscribe(this::browseForLocation);

			centerShell();
			ui.update();
		}

		private void centerShell() {
			Shell shell = getShell();
			shell.setSize(840, 480);
			Shell parent = (Shell) shell.getParent();
			if (parent != null) {
				Rectangle parentBounds = parent.getBounds();
				Point shellSize = shell.getSize();
				int x = parentBounds.x + (parentBounds.width - shellSize.x) / 2;
				int y = parentBounds.y + (parentBounds.height - shellSize.y) / 2;
				shell.setLocation(x, y);
			} else {
				// If no parent, center on the display
				Rectangle displayBounds = shell.getDisplay()
					.getPrimaryMonitor()
					.getBounds();
				Point shellSize = shell.getSize();
				int x = (displayBounds.width - shellSize.x) / 2;
				int y = (displayBounds.height - shellSize.y) / 2;
				shell.setLocation(x, y);
			}
		}

		List<TemplateInfo> toTemplates(Object[] selection) {
			return Stream.of(selection)
				.map(o -> (TemplateInfo) o)
				.toList();
		}

		void browseForLocation() {
			DirectoryDialog dialog = new DirectoryDialog(getShell());
			dialog.setFilterPath(model.location.getAbsolutePath());
			String path = dialog.open();
			if (path != null) {
				ui.write(() -> model.location(new File(path)));
			}
		}

		void addTemplate() {
			TemplateDefinitionDialog dialog = new TemplateDefinitionDialog(getShell());
			if (dialog.open() == Window.OK) {
				String selectedPath = dialog.getSelectedPath();
				if (selectedPath != null && !selectedPath.isBlank()) {
					Job job = Job.create("read " + selectedPath, mon -> {
						try {
							URI uri = toURI(selectedPath);
							Result<List<TemplateInfo>> result = templates.read(uri.toURL());

							if (result.isErr()) {
								ui.write(() -> model.error = result.toString());
							} else {
								result.unwrap()
									.forEach(templates::add);
								ui.write(() -> model.templates = templates.getAvailableTemplates());
							}
						} catch (Exception e) {
							ui.write(() -> model.error = "failed to add the index: " + e);
						}
					});
					job.schedule();
				}
			}

		}

		URI toURI(String path) {
			URI uri;
			File f = new File(path);
			if (f.isFile()) {
				uri = f.toURI();
			} else {
				uri = URI.create(path);
			}
			return uri;
		}
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {}

}
