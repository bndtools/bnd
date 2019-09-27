package bndtools.views.repository;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.core.ui.icons.Icons;
import org.bndtools.utils.swt.FilterPanelPart;
import org.bndtools.utils.swt.SWTUtil;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ViewPart;
import org.osgi.resource.Requirement;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.RemoteRepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.converter.Converter;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.central.RepositoriesViewRefresher;
import bndtools.central.RepositoryUtils;
import bndtools.model.repo.ContinueSearchElement;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleVersion;
import bndtools.model.repo.RepositoryEntry;
import bndtools.model.repo.RepositoryTreeLabelProvider;
import bndtools.model.repo.SearchableRepositoryTreeContentProvider;
import bndtools.preferences.BndPreferences;
import bndtools.preferences.JpmPreferences;
import bndtools.preferences.WorkspaceOfflineChangeAdapter;
import bndtools.utils.SelectionDragAdapter;
import bndtools.wizards.workspace.AddFilesToRepositoryWizard;
import bndtools.wizards.workspace.WorkspaceSetupWizard;

public class RepositoriesView extends ViewPart implements RepositoriesViewRefresher.RefreshModel {
	final static Pattern							LABEL_PATTERN				= Pattern
		.compile("(-)?(!)?([^{}]+)(?:\\{([^}]+)\\})?");
	private static final String						DROP_TARGET					= "dropTarget";

	private static final ILogger					logger						= Logger
		.getLogger(RepositoriesView.class);

	private final FilterPanelPart					filterPart					= new FilterPanelPart(
		Plugin.getDefault()
			.getScheduler());
	private SearchableRepositoryTreeContentProvider	contentProvider;
	private TreeViewer								viewer;
	private Control									filterPanel;

	private Action									collapseAllAction;
	private Action									refreshAction;
	private Action									addBundlesAction;
	private Action									advancedSearchAction;
	private Action									downloadAction;
	private String									advancedSearchState;
	private Action									offlineAction;

	private final BndPreferences					prefs						= new BndPreferences();

	private final WorkspaceOfflineChangeAdapter		workspaceOfflineListener	= new WorkspaceOfflineChangeAdapter() {
																					@Override
																					public void workspaceOfflineChanged(
																						boolean offline) {
																						configureOfflineAction();

																						if (!offline) {
																							// Fire
																							// a
																							// fake
																							// selection
																							// event
																							// so
																							// that
																							// repo
																							// plugins
																							// can
																							// do
																							// what
																							// they
																							// would
																							// do
																							// if
																							// they
																							// were
																							// already
																							// online
																							viewer.setSelection(
																								viewer.getSelection(),
																								false);
																						}
																					}
																				};

	@Override
	public void createPartControl(final Composite parent) {
		// CREATE CONTROLS

		final StackLayout stackLayout = new StackLayout();
		parent.setLayout(stackLayout);

		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		Composite defaultParent = toolkit.createComposite(parent, SWT.NONE);
		FillLayout fill = new FillLayout();
		fill.marginHeight = 5;
		fill.marginWidth = 5;
		defaultParent.setLayout(fill);

		if (!Central.hasWorkspaceDirectory()) {
			FormText form = toolkit.createFormText(defaultParent, true);
			form.setText("<form><p>No workspace configuration found. <a>Create a new Bnd workspace...</a></p></form>",
				true, false);
			form.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					IWorkbench workbench = PlatformUI.getWorkbench();
					IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

					WorkspaceSetupWizard wizard = new WorkspaceSetupWizard();
					wizard.init(workbench, StructuredSelection.EMPTY);
					WizardDialog dialog = new WizardDialog(window.getShell(), wizard);
					dialog.open();
				}
			});
		} else {
			toolkit.createLabel(defaultParent, "Repositories are loading, please wait...");
		}

		stackLayout.topControl = defaultParent;
		parent.layout();

		final Composite mainPanel = new Composite(parent, SWT.NONE);
		filterPanel = filterPart.createControl(mainPanel, 5, 5);
		Tree tree = new Tree(mainPanel, SWT.FULL_SELECTION | SWT.MULTI);
		filterPanel.setBackground(tree.getBackground());

		viewer = new TreeViewer(tree);

		contentProvider = new SearchableRepositoryTreeContentProvider() {
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				super.inputChanged(viewer, oldInput, newInput);

				if (newInput != null) {
					stackLayout.topControl = mainPanel;

					advancedSearchAction.setEnabled(true);
					refreshAction.setEnabled(true);
					collapseAllAction.setEnabled(true);

					configureOfflineAction();

					parent.layout();
				}
			}
		};
		viewer.setContentProvider(contentProvider);
		ColumnViewerToolTipSupport.enableFor(viewer);

		viewer.setLabelProvider(new RepositoryTreeLabelProvider(false));
		getViewSite().setSelectionProvider(viewer);
		Central.addRepositoriesViewer(viewer, RepositoriesView.this);

		JpmPreferences jpmPrefs = new JpmPreferences();
		final boolean showJpmOnClick = jpmPrefs.getBrowserSelection() != JpmPreferences.PREF_BROWSER_EXTERNAL;

		// LISTENERS
		filterPart.addPropertyChangeListener(event -> {
			String filter = (String) event.getNewValue();
			updatedFilter(filter);
		});
		ViewerDropAdapter dropAdapter = new ViewerDropAdapter(viewer) {
			@Override
			public boolean validateDrop(Object target, int operation, TransferData transferType) {
				if (target == null)
					return false;

				if (canDrop(target, transferType))
					return true;

				boolean valid = false;
				if (target instanceof RepositoryPlugin) {
					if (((RepositoryPlugin) target).canWrite()) {

						if (URLTransfer.getInstance()
							.isSupportedType(transferType))
							return true;

						if (LocalSelectionTransfer.getTransfer()
							.isSupportedType(transferType)) {
							ISelection selection = LocalSelectionTransfer.getTransfer()
								.getSelection();
							if (selection instanceof IStructuredSelection) {
								for (Iterator<?> iter = ((IStructuredSelection) selection).iterator(); iter
									.hasNext();) {
									Object element = iter.next();
									if (element instanceof RepositoryBundle
										|| element instanceof RepositoryBundleVersion) {
										valid = true;
										break;
									}
									if (element instanceof IFile) {
										valid = true;
										break;
									}
									if (element instanceof IAdaptable) {
										IFile file = ((IAdaptable) element).getAdapter(IFile.class);
										if (file != null) {
											valid = true;
											break;
										}
									}
								}
							}
						} else {
							valid = true;
						}
					}
				}
				return valid;
			}

			@Override
			public void dragEnter(DropTargetEvent event) {
				super.dragEnter(event);
				event.detail = DND.DROP_COPY;
			}

			@Override
			public boolean performDrop(Object data) {
				if (RepositoriesView.this.performDrop(getCurrentTarget(), getCurrentEvent().currentDataType, data)) {
					viewer.refresh(getCurrentTarget(), true);
					return true;
				}

				boolean copied = false;
				if (URLTransfer.getInstance()
					.isSupportedType(getCurrentEvent().currentDataType)) {
					try {
						URL url = new URL((String) URLTransfer.getInstance()
							.nativeToJava(getCurrentEvent().currentDataType));

						File tmp = File.createTempFile("dwnl", ".jar");
						try (HttpClient client = new HttpClient()) {
							IO.copy(client.connect(url), tmp);
						}

						if (isJarFile(tmp)) {
							copied = addFilesToRepository((RepositoryPlugin) getCurrentTarget(), new File[] {
								tmp
							});
						} else {
							tmp.delete();
							MessageDialog.openWarning(null, "Unrecognized Artifact",
								"The dropped URL is not recognized as a remote JAR file: " + url.toString());
						}
					} catch (Exception e) {
						return false;
					}
				} else if (data instanceof String[]) {
					String[] paths = (String[]) data;
					File[] files = new File[paths.length];
					for (int i = 0; i < paths.length; i++) {
						files[i] = new File(paths[i]);
					}
					copied = addFilesToRepository((RepositoryPlugin) getCurrentTarget(), files);
				} else if (data instanceof IResource[]) {
					IResource[] resources = (IResource[]) data;
					File[] files = new File[resources.length];
					for (int i = 0; i < resources.length; i++) {
						files[i] = resources[i].getLocation()
							.toFile();
					}
					copied = addFilesToRepository((RepositoryPlugin) getCurrentTarget(), files);
				} else if (data instanceof IStructuredSelection) {
					File[] files = convertSelectionToFiles((IStructuredSelection) data);
					if (files != null && files.length > 0)
						copied = addFilesToRepository((RepositoryPlugin) getCurrentTarget(), files);
				}
				return copied;
			}
		};
		dropAdapter.setFeedbackEnabled(false);
		dropAdapter.setExpandEnabled(false);

		viewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] {
			URLTransfer.getInstance(), FileTransfer.getInstance(), ResourceTransfer.getInstance(),
			LocalSelectionTransfer.getTransfer()
		}, dropAdapter);
		viewer.addDragSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] {
			LocalSelectionTransfer.getTransfer()
		}, new SelectionDragAdapter(viewer));

		viewer.addSelectionChangedListener(event -> {
			boolean writableRepoSelected = false;
			IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
			Object element = selection.getFirstElement();
			if (element instanceof RepositoryPlugin) {
				RepositoryPlugin repo = (RepositoryPlugin) element;
				writableRepoSelected = repo.canWrite();
			}
			addBundlesAction.setEnabled(writableRepoSelected);
		});
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent ev) {
				Object element = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
				if (element instanceof ContinueSearchElement) {
					try {
						getViewSite().getPage()
							.showView(Plugin.JPM_BROWSER_VIEW_ID, null,
								showJpmOnClick ? IWorkbenchPage.VIEW_ACTIVATE : IWorkbenchPage.VIEW_CREATE);
					} catch (PartInitException e) {
						Plugin.getDefault()
							.getLog()
							.log(e.getStatus());
					}
				}
			}
		});
		viewer.addDoubleClickListener(event -> {
			if (!event.getSelection()
				.isEmpty()) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				final Object element = selection.getFirstElement();
				if (element instanceof IAdaptable) {
					final URI uri = ((IAdaptable) element).getAdapter(URI.class);
					if (uri == null && element instanceof RepositoryEntry) {
						boolean download = MessageDialog.openQuestion(getSite().getShell(), "Repositories",
							"This repository entry is unable to be opened because it has not been downloaded. Download and open it now?");
						if (download) {
							final RepositoryEntry entry = (RepositoryEntry) element;
							Job downloadJob = new Job("Downloading repository entry " + entry.getBsn()) {
								@Override
								protected IStatus run(IProgressMonitor monitor) {
									final File repoFile = entry.getFile(true);
									if (repoFile != null && repoFile.exists()) {
										getSite().getShell()
											.getDisplay()
											.asyncExec(() -> openURI(repoFile.toURI()));
									}
									return Status.OK_STATUS;
								}
							};
							downloadJob.setUser(true);
							downloadJob.schedule();
						}
					} else if (uri != null) {
						openURI(uri);
					}
				} else if (element instanceof ContinueSearchElement) {
					ContinueSearchElement searchElement = (ContinueSearchElement) element;
					try {
						JpmPreferences jpmPrefs1 = new JpmPreferences();
						if (jpmPrefs1.getBrowserSelection() == JpmPreferences.PREF_BROWSER_EXTERNAL) {
							URI browseUrl = searchElement.browse();
							getViewSite().getWorkbenchWindow()
								.getWorkbench()
								.getBrowserSupport()
								.getExternalBrowser()
								.openURL(browseUrl.toURL());
						} else
							getViewSite().getPage()
								.showView(Plugin.JPM_BROWSER_VIEW_ID, null, IWorkbenchPage.VIEW_VISIBLE);
					} catch (PartInitException e1) {
						Plugin.getDefault()
							.getLog()
							.log(e1.getStatus());
					} catch (Exception e2) {
						Plugin.getDefault()
							.getLog()
							.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
								"Failed to load repository browser view", e2));
					}
				} else if (element instanceof RepositoryPlugin) {
					viewer.setExpandedState(element, !viewer.getExpandedState(element));
				}

			}
		});

		createContextMenu();

		// LAYOUT
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		mainPanel.setLayout(layout);

		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		filterPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		// Toolbar
		createActions();
		fillToolBar(getViewSite().getActionBars()
			.getToolBarManager());

		prefs.addPropertyChangeListener(workspaceOfflineListener);

		// synthenic call to "refresh" so that we can get the repositories to
		// show up in the UI
		new WorkspaceJob("Load repositories") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				try {
					Central.refreshPlugins();
				} catch (Exception e) {
					// ignore errors there may be no workspace yet
				}
				return Status.OK_STATUS;
			}
		}.schedule();

		IActionBars actionBars = getViewSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshAction);

	}

	private void configureOfflineAction() {
		Workspace workspace = Central.getWorkspaceIfPresent();
		if (workspace == null) {
			offlineAction.setChecked(false);
			offlineAction.setToolTipText("Go Offline");
			offlineAction.setImageDescriptor(Icons.desc("connected"));
			offlineAction.setDisabledImageDescriptor(Icons.desc("connected.disabled"));
			offlineAction.setEnabled(false);
			return;
		}

		if (prefs.isWorkspaceOffline()) {
			offlineAction.setChecked(true);
			offlineAction.setToolTipText("Go Online");
			offlineAction.setImageDescriptor(Icons.desc("disconnected"));
			offlineAction.setDisabledImageDescriptor(Icons.desc("disconnected.disabled"));
		} else {
			offlineAction.setChecked(false);
			offlineAction.setToolTipText("Go Offline");
			offlineAction.setImageDescriptor(Icons.desc("connected"));
			offlineAction.setDisabledImageDescriptor(Icons.desc("connected.disabled"));
		}
		offlineAction.setEnabled(true);
	}

	protected void openURI(URI uri) {
		IWorkbenchPage page = getSite().getPage();
		try {
			IFileStore fileStore = EFS.getLocalFileSystem()
				.getStore(uri);
			IDE.openEditorOnFileStore(page, fileStore);
		} catch (PartInitException e) {
			logger.logError("Error opening editor for " + uri, e);
		}
	}

	@Override
	public void setFocus() {
		filterPart.setFocus();
	}

	private static File[] convertSelectionToFiles(ISelection selection) {
		if (!(selection instanceof IStructuredSelection))
			return new File[0];

		IStructuredSelection structSel = (IStructuredSelection) selection;
		List<File> files = new ArrayList<>(structSel.size());

		for (Iterator<?> iter = structSel.iterator(); iter.hasNext();) {
			Object element = iter.next();
			if (element instanceof IFile)
				files.add(((IFile) element).getLocation()
					.toFile());
			else if (element instanceof IAdaptable) {
				IAdaptable adaptable = (IAdaptable) element;
				IFile ifile = adaptable.getAdapter(IFile.class);
				if (ifile != null) {
					files.add(ifile.getLocation()
						.toFile());
				} else {
					File file = adaptable.getAdapter(File.class);
					if (file != null) {
						files.add(file);
					}
				}
			}
		}

		return files.toArray(new File[0]);
	}

	@Override
	public void dispose() {
		Central.removeRepositoriesViewer(viewer);
		prefs.removePropertyChangeListener(workspaceOfflineListener);
		super.dispose();
	}

	boolean addFilesToRepository(RepositoryPlugin repo, File[] files) {
		AddFilesToRepositoryWizard wizard = new AddFilesToRepositoryWizard(repo, files);
		WizardDialog dialog = new WizardDialog(getViewSite().getShell(), wizard);
		dialog.open();
		viewer.refresh(repo);
		return true;
	}

	private void updatedFilter(String filterString) {
		contentProvider.setFilter(filterString);
		viewer.refresh();
		if (filterString != null)
			viewer.expandToLevel(2);
	}

	void createActions() {
		collapseAllAction = new Action() {
			@Override
			public void run() {
				viewer.collapseAll();
			}
		};
		collapseAllAction.setEnabled(false);
		collapseAllAction.setText("Collapse All");
		collapseAllAction.setToolTipText("Collapse All");
		collapseAllAction.setImageDescriptor(Icons.desc("collapse"));
		collapseAllAction.setDisabledImageDescriptor(Icons.desc("collapse.disabled"));

		refreshAction = new Action() {
			@Override
			public void run() {
				new WorkspaceJob("Refresh repositories") {

					@Override
					public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
						if (monitor == null)
							monitor = new NullProgressMonitor();

						monitor.subTask("Refresh all repositories");

						try {
							refreshAction.setEnabled(false);
							Central.refreshPlugins();
						} catch (Exception e) {
							Throwable t = Exceptions.unrollCause(e, InvocationTargetException.class);

							logger.logError("Unexpected error in refreshing plugns", t);

							return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, "Failed to refresh plugins", t);
						} finally {
							refreshAction.setEnabled(true);
						}
						return Status.OK_STATUS;
					}
				}.schedule();
			}
		};
		refreshAction.setEnabled(false);
		refreshAction.setText("Refresh");
		refreshAction.setToolTipText("Refresh Repositories Tree");
		refreshAction.setImageDescriptor(Icons.desc("refresh"));
		refreshAction.setDisabledImageDescriptor(Icons.desc("refresh.disabled"));

		addBundlesAction = new Action() {
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				Object element = selection.getFirstElement();
				if (element != null && element instanceof RepositoryPlugin) {
					RepositoryPlugin repo = (RepositoryPlugin) element;
					if (repo.canWrite()) {
						AddFilesToRepositoryWizard wizard = new AddFilesToRepositoryWizard(repo, new File[0]);
						WizardDialog dialog = new WizardDialog(getViewSite().getShell(), wizard);
						dialog.open();

						viewer.refresh(repo);
					}
				}
			}
		};
		addBundlesAction.setEnabled(false);
		addBundlesAction.setText("Add");
		addBundlesAction.setToolTipText("Add Bundles to Repository");
		addBundlesAction.setImageDescriptor(Icons.desc("add"));
		addBundlesAction.setDisabledImageDescriptor(Icons.desc("add.disabled"));

		advancedSearchAction = new Action("Advanced Search", IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				if (advancedSearchAction.isChecked()) {
					AdvancedSearchDialog dialog = new AdvancedSearchDialog(getSite().getShell());
					if (advancedSearchState != null) {
						try {
							XMLMemento memento = XMLMemento.createReadRoot(new StringReader(advancedSearchState));
							dialog.restoreState(memento);
						} catch (Exception e) {
							logger.logError("Failed to load dialog state", e);
						}
					}

					if (Window.OK == dialog.open()) {
						Requirement req = dialog.getRequirement();
						contentProvider.setRequirementFilter(req);
						SWTUtil.recurseEnable(false, filterPanel);
						viewer.refresh();
						viewer.expandToLevel(2);
					} else {
						advancedSearchAction.setChecked(false);
					}

					try {
						XMLMemento memento = XMLMemento.createWriteRoot("search");
						dialog.saveState(memento);

						StringWriter writer = new StringWriter();
						memento.save(writer);
						advancedSearchState = writer.toString();
					} catch (Exception e) {
						logger.logError("Failed to save dialog state", e);
					}
				} else {
					contentProvider.setRequirementFilter(null);
					SWTUtil.recurseEnable(true, filterPanel);
					viewer.refresh();
				}
			}
		};
		advancedSearchAction.setEnabled(false);
		advancedSearchAction.setText("Advanced Search");
		advancedSearchAction.setToolTipText("Toggle Advanced Search");
		advancedSearchAction.setImageDescriptor(Icons.desc("search"));
		advancedSearchAction.setDisabledImageDescriptor(Icons.desc("search.disabled"));

		downloadAction = new Action() {
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();

				// The set of Repos included in the selection; they will be
				// completely downloaded.
				Set<RemoteRepositoryPlugin> repos = Collections.newSetFromMap(new IdentityHashMap<>());
				repos.addAll(selectionByType(selection, RemoteRepositoryPlugin.class));

				// The set of Bundles included in the selection.
				Set<RepositoryBundle> bundles = Collections.newSetFromMap(new IdentityHashMap<>());
				for (RepositoryBundle bundle : selectionByType(selection, RepositoryBundle.class)) {
					// filter out bundles that come from already-selected repos.
					if (!repos.contains(bundle.getRepo()))
						bundles.add(bundle);
				}

				// The set of Bundle Versions included in the selection
				Set<RepositoryBundleVersion> bundleVersions = Collections.newSetFromMap(new IdentityHashMap<>());
				for (RepositoryBundleVersion bundleVersion : selectionByType(selection,
					RepositoryBundleVersion.class)) {
					// filter out bundles that come from already-selected repos.
					if (!repos.contains(bundleVersion.getRepo()))
						bundleVersions.add(bundleVersion);
				}

				RepoDownloadJob downloadJob = new RepoDownloadJob(repos, bundles, bundleVersions);
				downloadJob.schedule();
			}

			private <T> List<T> selectionByType(IStructuredSelection selection, Class<T> type) {
				List<T> result = new ArrayList<>(selection.size());
				@SuppressWarnings("unchecked")
				Iterator<Object> iterator = selection.iterator();
				while (iterator.hasNext()) {
					Object item = iterator.next();
					if (type.isInstance(item)) {
						@SuppressWarnings("unchecked")
						T cast = (T) item;
						result.add(cast);
					}
				}
				return result;
			}
		};
		downloadAction.setEnabled(false);
		downloadAction.setText("Download Repository Content");
		downloadAction.setImageDescriptor(Icons.desc("download"));
		downloadAction.setDisabledImageDescriptor(Icons.desc("download.disabled"));

		offlineAction = new Action("Online/Offline Mode", IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				Workspace workspace = Central.getWorkspaceIfPresent();
				if (workspace != null) {
					prefs.setWorkspaceOffline(offlineAction.isChecked());
				}
			}
		};
		offlineAction.setEnabled(false);
		offlineAction.setToolTipText("Go Offline");
		offlineAction.setImageDescriptor(Icons.desc("connected"));
		offlineAction.setDisabledImageDescriptor(Icons.desc("connected.disabled"));

		viewer.addSelectionChangedListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();

			boolean enable = false;
			@SuppressWarnings("unchecked")
			List<Object> list = selection.toList();
			for (Object item : list) {
				if (item instanceof RemoteRepositoryPlugin) {
					enable = true;
					break;
				} else if (item instanceof RepositoryEntry) {
					if (!((RepositoryEntry) item).isLocal()) {
						enable = true;
						break;
					}
				}
			}
			downloadAction.setEnabled(enable);
		});
	}

	void createContextMenu() {
		MenuManager mgr = new MenuManager();
		Menu menu = mgr.createContextMenu(viewer.getControl());
		viewer.getControl()
			.setMenu(menu);
		mgr.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		getSite().registerContextMenu(mgr, viewer);

		mgr.addMenuListener(manager -> {
			try {
				manager.removeAll();
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				if (!selection.isEmpty()) {
					final Object firstElement = selection.getFirstElement();
					if (firstElement instanceof Actionable) {

						final RepositoryPlugin rp = getRepositoryPlugin(firstElement);

						//
						// Use the Actionable interface to fill the menu
						// Should extend this to allow other menu entries
						// from the view, but currently there are none
						//
						final Actionable act = (Actionable) firstElement;
						Map<String, Runnable> actions = act.actions();
						if (actions != null) {
							for (final Entry<String, Runnable> e1 : actions.entrySet()) {
								String label = e1.getKey();
								boolean enabled = true;
								boolean checked = false;
								String description = null;
								Matcher m = LABEL_PATTERN.matcher(label);
								if (m.matches()) {
									if (m.group(1) != null)
										enabled = false;

									if (m.group(2) != null)
										checked = true;

									label = m.group(3);

									description = m.group(4);
								}
								Action a = new Action(label.replace("&", "&&")) {
									@Override
									public void run() {
										Job backgroundJob = new Job("Repository Action '" + getText() + "'") {

											@Override
											protected IStatus run(IProgressMonitor monitor) {
												try {
													e1.getValue()
														.run();
													if (rp != null && rp instanceof Refreshable)
														Central.refreshPlugin((Refreshable) rp);
												} catch (final Exception e) {
													IStatus status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID,
														"Error executing: " + getName(), e);
													Plugin.getDefault()
														.getLog()
														.log(status);
												}
												monitor.done();
												return Status.OK_STATUS;
											}
										};

										backgroundJob.addJobChangeListener(new JobChangeAdapter() {
											@Override
											public void done(IJobChangeEvent event) {
												if (event.getResult()
													.isOK()) {
													viewer.getTree()
														.getDisplay()
														.asyncExec(() -> viewer.refresh());
												}
											}
										});

										backgroundJob.setUser(true);
										backgroundJob.setPriority(Job.SHORT);
										backgroundJob.schedule();
									}
								};
								a.setEnabled(enabled);
								if (description != null)
									a.setDescription(description);
								a.setChecked(checked);
								manager.add(a);
							}
						}
					}
				}
			} catch (Exception e2) {
				throw new RuntimeException(e2);
			}
		});
	}

	private void fillToolBar(IToolBarManager toolBar) {
		toolBar.add(advancedSearchAction);
		toolBar.add(downloadAction);
		toolBar.add(new Separator());
		toolBar.add(refreshAction);
		toolBar.add(collapseAllAction);
		toolBar.add(addBundlesAction);
		toolBar.add(new Separator());
		toolBar.add(offlineAction);
		toolBar.add(new Separator());
	}

	/**
	 * Handle the drop on targets that understand drops.
	 *
	 * @param target The current target
	 * @param data The transfer data
	 * @return true if the data is acceptable, otherwise false
	 */
	boolean canDrop(Object target, TransferData data) {
		try {
			Class<?> type = toJavaType(data);
			if (type != null) {
				target.getClass()
					.getMethod(DROP_TARGET, type);
				return true;
			}
		} catch (Exception e) {
			// Ignore
		}
		return false;
	}

	/**
	 * Try a drop on the target. A drop is allowed if the target implements a
	 * {@code dropTarget} method that returns a boolean.
	 *
	 * @param target the target being dropped upon
	 * @param data the data
	 * @param data2
	 * @return true if dropped and processed, false if not
	 */
	boolean performDrop(Object target, TransferData data, Object dropped) {
		try {
			Object java = toJava(data);
			if (java == null) {
				java = toJava(dropped);
				if (java == null)
					return false;
			}

			try {
				Method m = target.getClass()
					.getMethod(DROP_TARGET, java.getClass());
				Boolean invoke = (Boolean) m.invoke(target, java);
				if (!invoke)
					return false;
			} catch (NoSuchMethodException e) {
				return false;
			}

			RepositoryPlugin repositoryPlugin = getRepositoryPlugin(target);
			if (repositoryPlugin != null && repositoryPlugin instanceof Refreshable)
				Central.refreshPlugin((Refreshable) repositoryPlugin);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private Object toJava(Object dropped) {
		if (dropped instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) dropped;
			if (!selection.isEmpty()) {
				Object firstElement = selection.getFirstElement();
				if (firstElement instanceof Resource) {
					Resource resource = (Resource) firstElement;
					IPath path = resource.getRawLocation();
					if (path != null) {
						File file = path.toFile();
						if (file != null)
							return file;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Return the class of the dropped object
	 *
	 * <pre>
	 *    URLTransfer             URI
	 *    FileTransfer            File[]
	 *    TextTransfer            String
	 *    ImageTransfer           ImageData
	 * </pre>
	 *
	 * @param data the dropped object
	 * @return the class of the dropped object, or null when it's unknown
	 * @throws Exception upon error
	 */
	Class<?> toJavaType(TransferData data) throws Exception {
		if (URLTransfer.getInstance()
			.isSupportedType(data))
			return URI.class;
		if (FileTransfer.getInstance()
			.isSupportedType(data))
			return File[].class;

		if (TextTransfer.getInstance()
			.isSupportedType(data))
			return String.class;

		if (ResourceTransfer.getInstance()
			.isSupportedType(data))
			return String.class;

		if (LocalSelectionTransfer.getTransfer()
			.isSupportedType(data)) {
			ISelection selection = LocalSelectionTransfer.getTransfer()
				.getSelection();
			if (selection instanceof IStructuredSelection) {
				Object firstElement = ((IStructuredSelection) selection).getFirstElement();
				if (firstElement instanceof IFile)
					return File.class;
			}
			return null;
		}

		// if (ImageTransfer.getInstance().isSupportedType(data))
		// return Image.class;
		return null;
	}

	/**
	 * Return a native data type that represents the dropped object
	 *
	 * <pre>
	 *    URLTransfer             URI
	 *    FileTransfer            File[]
	 *    TextTransfer            String
	 *    ImageTransfer           ImageData
	 * </pre>
	 *
	 * @param data the dropped object
	 * @return a native data type that represents the dropped object, or null
	 *         when the data type is unknown
	 * @throws Exception upon error
	 */
	Object toJava(TransferData data) throws Exception {
		LocalSelectionTransfer local = LocalSelectionTransfer.getTransfer();
		if (local.isSupportedType(data)) {
			ISelection selection = LocalSelectionTransfer.getTransfer()
				.getSelection();
			if (selection instanceof IStructuredSelection) {
				Object firstElement = ((IStructuredSelection) selection).getFirstElement();
				if (firstElement instanceof IFile) {
					IFile f = (IFile) firstElement;
					return f.getLocationURI();
				}
			}
		}
		if (URLTransfer.getInstance()
			.isSupportedType(data))
			return Converter.cnv(URI.class, URLTransfer.getInstance()
				.nativeToJava(data));
		else if (FileTransfer.getInstance()
			.isSupportedType(data)) {
			return Converter.cnv(File[].class, FileTransfer.getInstance()
				.nativeToJava(data));
		} else if (TextTransfer.getInstance()
			.isSupportedType(data)) {
			return TextTransfer.getInstance()
				.nativeToJava(data);
		}
		// Need to write the transfer code since the ImageTransfer turns it into
		// something very Eclipsy
		// else if (ImageTransfer.getInstance().isSupportedType(data))
		// return ImageTransfer.getInstance().nativeToJava(data);

		return null;
	}

	private RepositoryPlugin getRepositoryPlugin(Object element) {
		if (element instanceof RepositoryPlugin)
			return (RepositoryPlugin) element;
		else if (element instanceof RepositoryBundle)
			return ((RepositoryBundle) element).getRepo();
		else if (element instanceof RepositoryBundleVersion)
			return ((RepositoryBundleVersion) element).getParentBundle()
				.getRepo();

		return null;
	}

	@Override
	public List<RepositoryPlugin> getRepositories() {
		return RepositoryUtils.listRepositories(true);
	}

	private static boolean isJarFile(File candidate) {
		try (JarFile jar = new JarFile(candidate)) {
			return jar.getManifest() != null;
		} catch (IOException ex) {
			return false;
		}
	}
}
