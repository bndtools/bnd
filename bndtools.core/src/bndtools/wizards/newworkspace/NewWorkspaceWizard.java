package bndtools.wizards.newworkspace;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.bndtools.core.ui.StickyToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
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
import bndtools.preferences.BndPreferences;
import bndtools.shared.ConfirmDialogWithTextarea;
import bndtools.util.ui.UI;

/**
 * Create a new Workspace Wizard.
 */
public class NewWorkspaceWizard extends Wizard implements IImportWizard, INewWizard {

	private static final String		SETTINGS_SECTION			= "NewWorkspaceWizard";
	private static final String		SETTING_CLEAN				= "clean";
	private static final String		SETTING_SWITCH_WORKSPACE	= "switchWorkspace";
	private static final String		SETTING_SHOW_ARCHIVED		= "showArchived";
	private static final String		SETTING_UPDATE_WORKSPACE	= "updateWorkspace";
	private static final Point		MINIMUM_DIALOG_SIZE			= new Point(880, 480);

	static final Logger				log					= LoggerFactory.getLogger(NewWorkspaceWizard.class);

	final Model						model;
	final UI<Model>					ui;
	final NewWorkspaceWizardPage	page				= new NewWorkspaceWizardPage();
	final FragmentTemplateEngine	templates;
	final Set<String>					loadingTemplateIndexes	= new HashSet<>();
	final Map<String, List<TemplateInfo>>	templateIndexes			= new HashMap<>();

	final static Image				verified			= Icons.image("icons/tick.png", false);
	final static Image				verifiedGreyedOut	= new Image(Display.getDefault(), verified, SWT.IMAGE_DISABLE);

	public NewWorkspaceWizard() throws Exception {
		setWindowTitle("Create New bnd Workspace");
		Workspace workspace = Central.getWorkspace();
		this.model = new Model(workspace);
		this.ui = new UI<>(model);
		templates = new FragmentTemplateEngine(workspace);
		IDialogSettings workbenchSettings = Plugin.getDefault()
			.getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection(SETTINGS_SECTION);
		if (section == null) {
			section = workbenchSettings.addNewSection(SETTINGS_SECTION);
		}
		setDialogSettings(section);

		if (section.get(SETTING_CLEAN) != null) {
			model.clean = section.getBoolean(SETTING_CLEAN);
		}
		if (section.get(SETTING_SWITCH_WORKSPACE) != null) {
			model.switchWorkspace = section.getBoolean(SETTING_SWITCH_WORKSPACE);
		}
		if (section.get(SETTING_SHOW_ARCHIVED) != null) {
			model.showArchived = section.getBoolean(SETTING_SHOW_ARCHIVED);
		}
		if (section.get(SETTING_UPDATE_WORKSPACE) != null) {
			model.updateWorkspace(section.getBoolean(SETTING_UPDATE_WORKSPACE));
		} else {
			model.updateWorkspace(true);
		}

		try {
			Job job = Job.create("load index", mon -> {
				try {
					for (String uri : new BndPreferences().getWorkspaceTemplateIndexes()) {
						List<TemplateInfo> indexTemplates = templates.read(new URL(uri))
							.unwrap();
						indexTemplates.forEach(templates::add);
						templateIndexes.put(URI.create(uri).normalize().toString(), indexTemplates);
					}
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
							"failed to read template index", e));
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

		List<TemplateInfo> selectedAndRequired = templates.resolveRequirements(model.selectedTemplates);
		List<TemplateInfo> thirdParty = selectedAndRequired.stream()
			.filter(t -> !t.isOfficial())
			.toList();
		long num3rdParty = thirdParty.size();

		String title = "Install 3rd-Party templates";
		String message = String.format("Your selection would install %s"
			+ " template fragments from 3rd-party authors (including required dependency fragments): %n"
			+ "%nThese fragments may contain build instructions (e.g., .bnd files) that are executed with the next build.%n"
			+ "Only continue if you fully trust the authors and the content.%n%n" + "Do you want to proceed?",
			num3rdParty);
		StringBuilder details = new StringBuilder();
		try (Formatter f = new Formatter(details);) {
			thirdParty.forEach(ti -> f.format("%n%s - %s (Repo: %s) %n", ti.name(), ti.description(),
				ti.id()
					.repoUrl()));
		}

		boolean confirmed = num3rdParty == 0
			|| ConfirmDialogWithTextarea.open(getShell(), title, message, details.toString());

		if (!confirmed) {
			// not confirmed. cancel selected
			return false;
		}

		if (model.valid == null) {
			saveDialogSettings();
			ui.write(() -> {
				TemplateUpdater updater = templates.updater(model.location, model.selectedTemplates);
				model.execute(updater);
			});
			return true;
		} else
			return false;

	}

	@Override
	public void dispose() {
		saveDialogSettings();
		super.dispose();
	}

	@Override
	public Point getMinimumWizardSize() {
		return MINIMUM_DIALOG_SIZE;
	}

	private void saveDialogSettings() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			settings.put(SETTING_CLEAN, model.clean);
			settings.put(SETTING_SWITCH_WORKSPACE, model.switchWorkspace);
			settings.put(SETTING_SHOW_ARCHIVED, model.showArchived);
			settings.put(SETTING_UPDATE_WORKSPACE, model.updateWorkspace);
		}
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

			Button showArchived = new Button(container, SWT.CHECK);
			showArchived.setText("Show archived");
			showArchived.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 8, 1));

			Label filterLabel = new Label(container, SWT.NONE);
			filterLabel.setText("Filter");
			filterLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));

			Text filterText = new Text(container, SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);
			filterText.setMessage("glob pattern, e.g. *eclipse*");
			GridData filterTextLayoutData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 7, 1);
			GC gc = new GC(filterText);
			filterTextLayoutData.widthHint = gc.stringExtent("MMMMMMMMMMMMMMMMMMMMMMMMMMMMMM").x;
			gc.dispose();
			filterText.setLayoutData(filterTextLayoutData);

			CheckboxTableViewer selectedTemplates = CheckboxTableViewer.newCheckList(container,
				SWT.BORDER | SWT.FULL_SELECTION);
			StickyToolTipSupport.enableFor(selectedTemplates);
			selectedTemplates.setContentProvider(ArrayContentProvider.getInstance());
			selectedTemplates.addFilter(new ViewerFilter() {
				@Override
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					if (model.showArchived) {
						return true;
					}
					if (element instanceof TemplateInfo ti) {
						return !ti.archived();
					}
					return true;
				}
			});
			selectedTemplates.addFilter(new ViewerFilter() {
				@Override
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					String glob = filterText.getText()
						.trim();
					if (glob.isEmpty()) {
						return true;
					}
					if (!(element instanceof TemplateInfo ti)) {
						return true;
					}
					Pattern pattern = Pattern.compile(globToRegex(glob), Pattern.CASE_INSENSITIVE);
					String author = ti.isOfficial() ? "bndtools (Official)"
						: ti.id()
							.organisation() + " (3rd Party)";
					return pattern.matcher(ti.name())
						.matches()
						|| pattern.matcher(ti.description())
							.matches()
						|| pattern.matcher(author)
							.matches();
				}
			});
			filterText.addModifyListener(e -> selectedTemplates.refresh());
			Table table = selectedTemplates.getTable();
			GridData tableLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 6, 10);
			tableLayoutData.widthHint = 820;
			tableLayoutData.heightHint = 360;
			table.setLayoutData(tableLayoutData);
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

			TableViewerColumn checkboxColumn = new TableViewerColumn(selectedTemplates, SWT.NONE);
			checkboxColumn.setLabelProvider(new ColumnLabelProvider());

			TableViewerColumn indexColumn = new TableViewerColumn(selectedTemplates, SWT.NONE);
			indexColumn.getColumn()
				.setText("#");
			indexColumn.setLabelProvider(new ColumnLabelProvider() {

				@Override
				public String getText(Object element) {
					if (element instanceof TemplateInfo ti) {
						return String.valueOf(localIndex(ti));
					}
					return super.getText(element);
				}
			});

			TableViewerColumn nameColumn = new TableViewerColumn(selectedTemplates, SWT.NONE);
			nameColumn.getColumn()
				.setText("Name");
			nameColumn.setLabelProvider(new ColumnLabelProvider() {

				@Override
				public String getText(Object element) {
					if (element instanceof TemplateInfo ti) {
						return ti.name() + (ti.archived() ? " (archived)" : "");
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
						if (ti.archived()) {
							return verifiedGreyedOut;
						}
						boolean officialOrSHA = ti.isOfficial() || ti.isCommitSHA();
						return officialOrSHA ? verified : verifiedGreyedOut;
					}

					return super.getImage(element);
				}

			});

			TableViewerColumn requiresColumn = new TableViewerColumn(selectedTemplates, SWT.NONE);
			requiresColumn.getColumn()
				.setText("Requires");
			requiresColumn.setLabelProvider(new ColumnLabelProvider() {

				@Override
				public String getText(Object element) {
					if (element instanceof TemplateInfo ti) {
						return requiredIndices(ti);
					}
					return super.getText(element);
				}

				@Override
				public String getToolTipText(Object element) {
					if (element instanceof TemplateInfo ti) {

						List<TemplateInfo> reqs = NewWorkspaceWizard.this.templates
							.resolveRequirements(Collections.singletonList(ti));
						return reqs != null && reqs.size() > 1 ? "will be also installed: " + reqs.stream()
							.filter(rti -> !rti.equals(ti))
							.map(rti -> rti.name())
							.collect(Collectors.joining(", "))
							: "No dependencies";
					}
					return super.getToolTipText(element);
				}
			});
			tableLayout.addColumnData(new ColumnPixelData(20, false));
			tableLayout.addColumnData(new ColumnPixelData(30, false));
			tableLayout.addColumnData(new ColumnPixelData(120, false));
			tableLayout.addColumnData(new ColumnPixelData(460, false));
			tableLayout.addColumnData(new ColumnPixelData(120, false));
			tableLayout.addColumnData(new ColumnPixelData(60, false));

			Button addButton = new Button(container, SWT.PUSH);
			addButton.setText("+");
			addButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
			addButton.setToolTipText("Add entry via URI to a template repo index.bnd");

			FormText formText = new FormText(container, SWT.NO_FOCUS | SWT.FILL);
			formText.setText("Double click to open the fragment template Github-Repo in your browser.", false, false);
			formText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 6, 1));

			ui.u("location", model.location, UI.text(location)
				.map(File::getAbsolutePath, File::new));
			ui.u("clean", model.clean, UI.checkbox(clean));
			ui.u("switchWorkspace", model.switchWorkspace, UI.checkbox(switchWorkspace));
			ui.u("showArchived", model.showArchived, UI.checkbox(showArchived))
				.bind(v -> selectedTemplates.refresh());
			ui.u("updateWorkspace", model.updateWorkspace, UI.checkbox(useEclipseWorkspace))
				.bind(v -> location.setEnabled(!v))
				.bind(v -> browseButton.setEnabled(!v))
				.bind(v -> switchWorkspace.setEnabled(!v))
				.bind(v -> clean.setEnabled(!v))
				.bind(v -> setTitle(
					v ? "Update Workspace from template fragment" : "Create New Workspace from template fragment"))
				.bind(v -> setWindowTitle(v ? "Update Workspace from template fragment" : "Create New Workspace"));

			ui.u("valid", model.valid, this::setErrorMessage);
			ui.u("error", model.error, this::setErrorMessage);
			ui.u("valid", model.valid, v -> setPageComplete(v == null));
			ui.u("templates", model.templates, l -> selectedTemplates.setInput(l.toArray()));
			ui.u("selectedTemplates", model.selectedTemplates, UI.widget(selectedTemplates)
				.map(List::toArray, this::toTemplates));
			UI.checkbox(addButton)
				.subscribe(this::addTemplate);
			UI.checkbox(browseButton)
				.subscribe(this::browseForLocation);

			configureShellSize();
			ui.update();
		}

		private void configureShellSize() {
			Shell shell = getShell();
			shell.setMinimumSize(MINIMUM_DIALOG_SIZE);
			Shell parent = (Shell) shell.getParent();
			if (parent != null) {
				Rectangle parentBounds = parent.getClientArea();
				shell.setMaximumSize(parentBounds.width, parentBounds.height);
				Point preferredSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
				Point shellSize = new Point(Math.min(preferredSize.x, parentBounds.width),
					Math.min(preferredSize.y, parentBounds.height));
				shell.setSize(shellSize);
				Point parentOrigin = parent.toDisplay(parentBounds.x, parentBounds.y);
				shell.setLocation(parentOrigin.x + (parentBounds.width - shellSize.x) / 2,
					parentOrigin.y + (parentBounds.height - shellSize.y) / 2);
			}
		}

		List<TemplateInfo> toTemplates(Object[] selection) {
			return Stream.of(selection)
				.map(o -> (TemplateInfo) o)
				.toList();
		}

		int localIndex(TemplateInfo template) {
			return model.templates.indexOf(template) + 1;
		}

		String requiredIndices(TemplateInfo template) {
					List<TemplateInfo> requirements = NewWorkspaceWizard.this.templates
						.resolveRequirements(Collections.singletonList(template));
					if (requirements.size() <= 1) {
				return "-";
			}
					String indices = requirements.stream()
						.filter(required -> !required.equals(template))
						.map(this::localIndex)
						.map(String::valueOf)
				.collect(Collectors.joining(", "));
			return indices;
		}

		// converts a shell-style glob (* and ?) into a case-insensitive regex; plain text without glob
		// special characters is treated as a substring search
		static String globToRegex(String glob) {
			boolean hasWildcard = glob.indexOf('*') >= 0 || glob.indexOf('?') >= 0;
			StringBuilder sb = new StringBuilder();
			if (!hasWildcard) {
				sb.append(".*");
			}
			for (char c : glob.toCharArray()) {
				switch (c) {
					case '*' -> sb.append(".*");
					case '?' -> sb.append('.');
					case '.', '(', ')', '+', '|', '^', '$', '@', '%', '[', ']', '{', '}', '\\' -> sb.append('\\')
						.append(c);
					default -> sb.append(c);
				}
			}
			if (!hasWildcard) {
				sb.append(".*");
			}
			return sb.toString();
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
					URI uri;
					try {
						uri = toURI(selectedPath).normalize();
					} catch (Exception e) {
						model.error = "failed to add the index: " + e;
						return;
					}
					String uriStr = uri.toString();
					if (!loadingTemplateIndexes.add(uriStr)) {
						model.error = "index is already being added: " + uriStr;
						return;
					}
					Job job = Job.create("read " + selectedPath, mon -> {
						try {
							Result<List<TemplateInfo>> result = templates.read(uri.toURL());

							if (result.isErr()) {
								ui.write(() -> {
									loadingTemplateIndexes.remove(uriStr);
									model.error = result.toString();
								});
							} else {
								List<TemplateInfo> indexTemplates = result.unwrap();
								templates.removeAll(templateIndexes.getOrDefault(uriStr, Collections.emptyList()));
								indexTemplates.forEach(templates::add);
								templateIndexes.put(uriStr, indexTemplates);
								BndPreferences prefs = new BndPreferences();
								List<String> indexes = new ArrayList<>(prefs.getWorkspaceTemplateIndexes());
								if (!indexes.contains(uriStr)) {
									indexes.add(uriStr);
									prefs.setWorkspaceTemplateIndexes(indexes);
								}
								ui.write(() -> {
									loadingTemplateIndexes.remove(uriStr);
									model.templates = templates.getAvailableTemplates();
								});
							}
						} catch (Exception e) {
							ui.write(() -> {
								loadingTemplateIndexes.remove(uriStr);
								model.error = "failed to add the index: " + e;
							});
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
