package bndtools.editor;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.RunMode;
import org.bndtools.core.jobs.JobUtil;
import org.bndtools.core.resolve.ResolutionResult;
import org.bndtools.core.resolve.ResolveJob;
import org.bndtools.core.resolve.ui.ResolutionWizard;
import org.bndtools.core.ui.ExtendedFormEditor;
import org.bndtools.core.ui.IFormPageFactory;
import org.bndtools.utils.swt.SWTConcurrencyUtil;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.internal.runtime.Activator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.help.instructions.ResolutionInstructions.ResolveMode;
import aQute.bnd.properties.BadLocationException;
import aQute.bnd.exceptions.Exceptions;
import biz.aQute.resolve.Bndrun;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.editor.common.IPriority;
import bndtools.editor.model.IDocumentWrapper;
import bndtools.editor.pages.BundleContentPage;
import bndtools.editor.pages.BundleDescriptionPage;
import bndtools.editor.pages.ProjectBuildPage;
import bndtools.editor.pages.ProjectRunPage;
import bndtools.editor.pages.TestSuitesPage;
import bndtools.editor.pages.WorkspacePage;
import bndtools.launch.LaunchConstants;
import bndtools.launch.util.LaunchUtils;
import bndtools.preferences.BndPreferences;
import bndtools.types.Pair;

public class BndEditor extends ExtendedFormEditor implements IResourceChangeListener {

	private static final ILogger				logger				= Logger.getLogger(BndEditor.class);
	public static final String					SYNC_MESSAGE		= "Workspace is loading, please wait...";

	public static final String					WORKSPACE_EDITOR	= "bndtools.bndWorkspaceConfigEditor";

	public static final String					SOURCE_PAGE			= "__source_page";

	public static final String					CONTENT_PAGE		= "__content_page";
	public static final String					WORKSPACE_PAGE		= "__workspace_page";
	public static final String					WORKSPACE_EXT_PAGE	= "__workspace_ext_page";
	public static final String					DESCRIPTION_PAGE	= "__description_page";
	public static final String					BUILD_PAGE			= "__build_page";
	public static final String					PROJECT_RUN_PAGE	= "__project_run_page";
	public static final String					BNDRUN_PAGE			= "__bndrun_page";
	public static final String					TEST_SUITES_PAGE	= "__test_suites_page";

	private final BndEditModel					model				= new BndEditModel();

	private final Map<String, IFormPageFactory>	pageFactories		= new LinkedHashMap<>();

	private final Image							buildFileImg		= AbstractUIPlugin
		.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/bndtools-logo-16x16.png")
		.createImage();

	private BndSourceEditorPage					sourcePage;
	private Promise<Workspace>					modelReady;

	private IResource							inputResource;
	private File								inputFile;

	private void updateIncludedPages() {
		List<String> requiredPageIds = new LinkedList<>();

		// Need to know the file and project names.
		Pair<String, String> fileAndProject = getFileAndProject(getEditorInput());
		String path = fileAndProject.getFirst();
		String projectName = fileAndProject.getSecond();

		if (isMainWorkspaceConfig(path, projectName)) {
			requiredPageIds.add(WORKSPACE_PAGE);
		} else if (isExtWorkspaceConfig(path, projectName)) {
			requiredPageIds.add(WORKSPACE_EXT_PAGE);
			setTitleImage(buildFileImg);
		} else if (path.endsWith(LaunchConstants.EXT_BNDRUN)) {
			requiredPageIds.addAll(getPagesBndRun());
		} else {
			requiredPageIds.addAll(getPagesBnd(path));
		}
		requiredPageIds.add(SOURCE_PAGE);

		// Remove pages no longer required and remember the rest in a map
		int i = 0;
		Map<String, IFormPage> pageCache = new HashMap<>(requiredPageIds.size());
		while (i < getPageCount()) {
			IFormPage current = (IFormPage) pages.get(i);
			if (!requiredPageIds.contains(current.getId()))
				removePage(i);
			else {
				pageCache.put(current.getId(), current);
				i++;
			}
		}

		// Cache new pages
		for (String pageId : requiredPageIds) {
			if (!pageCache.containsKey(pageId)) {
				IFormPage page = SOURCE_PAGE.equals(pageId) ? sourcePage
					: pageFactories.get(pageId)
						.createPage(this, model, pageId);
				pageCache.put(pageId, page);
			}
		}

		// Add pages back in
		int requiredPointer = 0;
		int existingPointer = 0;

		while (requiredPointer < requiredPageIds.size()) {
			try {
				String requiredId = requiredPageIds.get(requiredPointer);
				if (existingPointer >= getPageCount()) {
					if (SOURCE_PAGE.equals(requiredId))
						addPage(sourcePage, getEditorInput());
					else
						addPage(pageCache.get(requiredId));
				} else {
					IFormPage existingPage = (IFormPage) pages.get(existingPointer);
					if (!requiredId.equals(existingPage.getId())) {
						if (SOURCE_PAGE.equals(requiredId))
							addPage(existingPointer, sourcePage, getEditorInput());
						else
							addPage(existingPointer, pageCache.get(requiredId));
					}
				}
				existingPointer++;
			} catch (PartInitException e) {
				logger.logError("Error adding page(s) to the editor.", e);
			}
			requiredPointer++;
		}

		// Set the source page title
		setPageText(sourcePage.getIndex(), "Source");

	}

	private static boolean isMainWorkspaceConfig(String path, String projectName) {
		if (Workspace.CNFDIR.equals(projectName)) {
			return Workspace.BUILDFILE.equals(path);
		}
		return false;
	}

	private static boolean isExtWorkspaceConfig(String path, String projectName) {
		if (Workspace.CNFDIR.equals(projectName)) {
			return path.startsWith("ext/") && path.endsWith(".bnd");
		}
		return false;
	}

	private final AtomicBoolean	saving	= new AtomicBoolean(false);
	private IHandlerActivation	resolveHandlerActivation;
	private JobChangeAdapter	resolveJobListener;

	@Override
	public void doSave(IProgressMonitor monitor) {

		commitDirtyPages();

		ResolveMode resolveMode = model.getResolveMode();

		// If auto resolve, then resolve and save in background thread.
		if (resolveMode == ResolveMode.auto && !PlatformUI.getWorkbench()
			.isClosing()) {
			resolveRunBundles(monitor, true);
		} else {
			// Not auto-resolving, just save
			reallySave(monitor);
		}
	}

	/**
	 * Commit the active page to the edit model or if no active page, commit all
	 * pages
	 */
	public void commitDirtyPages() {
		if (sourcePage.isActive() && sourcePage.isDirty()) {
			sourcePage.commit(true);
		} else {
			commitPages(true);
			sourcePage.refresh();
		}
	}

	private void reallySave(IProgressMonitor monitor) {
		// Actually save, via the source editor
		try {
			boolean saveLocked = this.saving.compareAndSet(false, true);
			if (!saveLocked) {
				logger.logError("Tried to save while already saving", null);
				return;
			}
			sourcePage.doSave(monitor);
			loadEditModel();
			updateIncludedPages();
		} catch (Exception e) {
			ErrorDialog.openError(getEditorSite().getShell(), "Error", null,
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Failed to reload bnd edit model", e));
		} finally {
			this.saving.set(false);
		}
	}

	public Promise<IStatus> resolveRunBundles(IProgressMonitor monitor, boolean onSave) {
		final Shell shell = getEditorSite().getShell();
		final IFile file = ResourceUtil.getFile(inputResource);
		if (file == null) {
			MessageDialog.openError(shell, "Resolution Error",
				"Unable to run resolution because the file is not part of the current workspace.");
			if (onSave) {
				reallySave(monitor);
			}
			return Central.promiseFactory()
				.resolved(Status.CANCEL_STATUS);
		}

		Job loadWorkspaceJob = new Job("Loading workspace...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (monitor == null)
					monitor = new NullProgressMonitor();
				monitor.beginTask("Loading workspace", 2);
				try {
					Central.getWorkspace();
					monitor.worked(1);
					modelReady.getValue();
					return Status.OK_STATUS;
				} catch (Exception e) {
					return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Failed to initialize workspace.", e);
				} finally {
					monitor.done();
				}
			}
		};
		final UIJob runResolveInUIJob = new UIJob("Resolve") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				// Make sure all the parts of this editor page have committed
				// their
				// dirty state to the model
				for (Object pageObj : pages) {
					if (pageObj instanceof IFormPage) {
						IFormPage formPage = (IFormPage) pageObj;
						IManagedForm form = formPage.getManagedForm();
						if (form != null) {
							IFormPart[] formParts = form.getParts();
							for (IFormPart formPart : formParts) {
								if (formPart.isDirty())
									formPart.commit(false);
							}
						}
					}
				}
				if (sourcePage.isActive() && sourcePage.isDirty()) {
					sourcePage.commit(false);
				}

				// Create resolver job and pre-validate
				final ResolveJob job = new ResolveJob(model, inputResource);
				IStatus validation = job.validateBeforeRun();
				if (!validation.isOK()) {
					if (onSave)
						reallySave(monitor);
					return validation;
				}

				// Add operation to perform at the end of resolution (i.e.
				// display
				// results and actually save the file)
				final UIJob completionJob = new UIJob(shell.getDisplay(), "Display Resolution Results") {
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						ResolutionResult result = job.getResolutionResult();
						ResolutionWizard wizard = new ResolutionWizard(model, file, result);

						if (onSave) {
							// We are in auto-resolve-on-save, only show the
							// dialog if there is a problem
							wizard.setAllowFinishUnresolved(true);
							wizard.setPreserveRunBundlesUnresolved(true);
							if (result.getOutcome() != ResolutionResult.Outcome.Resolved) {
								WizardDialog dialog = new DuringSaveWizardDialog(shell, wizard);
								dialog.create();
								dialog.setErrorMessage("Resolve Failed! Saving now will not update the Run Bundles.");
								if (dialog.open() == Window.OK)
									reallySave(monitor);
							} else {
								wizard.performFinish();
								reallySave(monitor);
							}
						} else {
							// This is an interactive resolve, always show the
							// dialog
							boolean dirtyBeforeResolve = isDirty();
							WizardDialog dialog = new WizardDialog(shell, wizard) {

								@Override
								protected Button createButton(org.eclipse.swt.widgets.Composite parent, int id,
									String label, boolean defaultButton) {
									Button button = super.createButton(parent, id, label, defaultButton);
									if (id == IDialogConstants.FINISH_ID) {
										if (model.getResolveMode() == ResolveMode.beforelaunch) {
											button.setText("Set Cache");
											button.setToolTipText(
												"-resolve is set `beforelaunch`, resolving saves the run bundles in the cache");
										} else {
											button.setText("Update");
											button.setToolTipText("Update the -runbundles");
										}
									}
									return button;
								}
							};
							if (dialog.open() == Window.OK && !dirtyBeforeResolve) {
								// save changes immediately if there were no
								// unsaved changes before the resolve
								reallySave(monitor);
							}
						}
						return Status.OK_STATUS;
					}
				};
				job.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						completionJob.schedule();
					}
				});

				// Start job
				job.setUser(true);
				job.schedule();
				return Status.OK_STATUS;
			}
		};
		runResolveInUIJob.setUser(true);
		return JobUtil.chainJobs(loadWorkspaceJob, runResolveInUIJob);
	}

	protected void ensurePageExists(String pageId, IFormPage page, int index) {
		IFormPage existingPage = findPage(pageId);
		if (existingPage != null)
			return;

		try {
			addPage(index, page);
		} catch (PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), "Error", null,
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error adding page to editor.", e));
		}
	}

	protected void removePage(String pageId) {
		IFormPage page = findPage(pageId);
		if (page != null) {
			removePage(page.getIndex());
		}
	}

	@Override
	public void doSaveAs() {}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	protected void handlePropertyChange(int propertyId) {
		super.handlePropertyChange(propertyId);
	}

	@Override
	protected void addPages() {
		updateIncludedPages();

		showHighestPriorityPage();

		if (!Central.hasAnyWorkspace()) {
			IFormPage activePage = getActivePageInstance();

			if (activePage != null && activePage.getManagedForm() != null) {
				ScrolledForm form = activePage.getManagedForm()
					.getForm();
				if (form.getMessage() == null) {
					form.setMessage(SYNC_MESSAGE, IMessageProvider.WARNING);
				}
			}
		}
	}

	void showHighestPriorityPage() {
		int selectedPrio = Integer.MIN_VALUE;
		String selected = null;

		BndPreferences prefs = new BndPreferences();
		if (prefs.getEditorOpenSourceTab()) {
			selected = SOURCE_PAGE;
			selectedPrio = 0;
		} else {
			for (Object pageObj : pages) {
				IFormPage page = (IFormPage) pageObj;
				int priority = 0;
				if (page instanceof IPriority)
					priority = ((IPriority) page).getPriority();
				if (priority > selectedPrio) {
					selected = page.getId();
					selectedPrio = priority;
				}
			}
		}

		if (selected != null)
			setActivePage(selected);
	}

	private List<String> getPagesBnd(String fileName) {
		List<String> pages = new ArrayList<>(5);

		boolean isProjectFile = Project.BNDFILE.equals(fileName);
		List<String> subBndFiles = model.getSubBndFiles();
		boolean isSubBundles = subBndFiles != null && !subBndFiles.isEmpty();

		for (Entry<String, IFormPageFactory> pageEntry : pageFactories.entrySet()) {
			String pageId = pageEntry.getKey();
			IFormPageFactory page = pageEntry.getValue();

			if (!isSubBundles && page.supportsMode(IFormPageFactory.Mode.bundle))
				pages.add(pageId);
			else if (isProjectFile && page.supportsMode(IFormPageFactory.Mode.project))
				pages.add(pageId);
		}

		return pages;
	}

	private List<String> getPagesBndRun() {
		List<String> pageIds = new ArrayList<>(3);
		for (Entry<String, IFormPageFactory> pageEntry : pageFactories.entrySet()) {
			if (pageEntry.getValue()
				.supportsMode(IFormPageFactory.Mode.bndrun))
				pageIds.add(pageEntry.getKey());
		}
		return pageIds;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {

		super.init(site, input);
		try {

			// Work out our input file and subscribe to resource changes
			this.inputResource = ResourceUtil.getResource(input);
			if (this.inputResource != null) {
				inputResource.getWorkspace()
					.addResourceChangeListener(this);
				inputFile = inputResource.getLocation()
					.toFile();
				model.setBndResourceName(inputResource.getName());
			} else {
				if (input instanceof FileStoreEditorInput) {
					URI uri = ((FileStoreEditorInput) input).getURI();
					if (uri != null && uri.getScheme()
						.equalsIgnoreCase("file")) {
						this.inputFile = new File(uri);
						model.setBndResourceName(this.inputFile.getName());
					}
				}
			}

			if (this.inputFile == null) {
				throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"The bnd editor can only edit files inside the workspace"));
			}

			// Initialize pages and title
			initPages(site, input);
			setSourcePage(sourcePage);
			setPartNameForInput(input);

			IDocumentProvider docProvider = sourcePage.getDocumentProvider();
			// #1625: Ensure the IDocumentProvider is not null.
			if (docProvider != null) {
				docProvider.addElementStateListener(new ElementStateListener());
				if (!Central.hasWorkspaceDirectory()) { // default ws will be
														// created we can load
														// immediately
					modelReady = loadEditModel();
				} else { // a real ws will be resolved so we need to load async
					modelReady = Central.onAnyWorkspace(workspace -> loadEditModel());
				}
			} else {
				modelReady = Central.promiseFactory()
					.failed(new Exception("Model unavailable"));
			}

			setupActions();

		} catch (Exception e1) {
			throw Exceptions.duck(e1);
		}
	}

	private void setupActions() {
		String fileName = getFileAndProject(getEditorInput()).getFirst();
		if (fileName.endsWith(LaunchConstants.EXT_BNDRUN)) {
			final IHandlerService handlerSvc = getEditorSite().getService(IHandlerService.class);
			final AbstractHandler handler = new AbstractHandler() {
				@Override
				public Object execute(ExecutionEvent event) throws ExecutionException {
					resolveRunBundles(new NullProgressMonitor(), false);
					return null;
				}
			};
			final IHandlerActivation activation = handlerSvc.activateHandler("bndtools.runEditor.resolve", handler);

			this.resolveJobListener = new JobChangeAdapter() {
				@Override
				public void running(IJobChangeEvent event) {
					if (event.getJob() instanceof ResolveJob)
						Display.getDefault()
							.asyncExec(() -> handlerSvc.deactivateHandler(activation));
				}

				@Override
				public void done(IJobChangeEvent event) {
					if (event.getJob() instanceof ResolveJob)
						Display.getDefault()
							.asyncExec(() -> handlerSvc.activateHandler(activation));
				}
			};
			Job.getJobManager()
				.addJobChangeListener(resolveJobListener);
		}

	}

	private Promise<Workspace> loadEditModel() throws Exception {
		// Create the bnd edit model and workspace
		Project bndProject;
		if (inputResource != null) {
			bndProject = LaunchUtils.createRun(inputResource, RunMode.EDIT);
		} else {
			bndProject = Bndrun.createBndrun(null, this.inputFile);
		}
		model.setWorkspace(bndProject.getWorkspace());
		model.setProject(bndProject);

		// Load content into the edit model
		Deferred<Workspace> completed = Central.promiseFactory()
			.deferred();
		Display.getDefault()
			.asyncExec(() -> {
				final IDocumentProvider docProvider = sourcePage.getDocumentProvider();
				// #1625: Ensure the IDocumentProvider is not null.
				if (docProvider != null) {
					try {
						IDocument document = docProvider.getDocument(getEditorInput());
						model.loadFrom(new IDocumentWrapper(document));
						model.setBndResource(inputFile);
						model.setDirty(false);
						completed.resolve(model.getWorkspace());
					} catch (IOException e) {
						logger.logError("Unable to load edit model", e);
						completed.fail(e);
					}

					for (int i = 0; i < getPageCount(); i++) {
						Control control = getControl(i);

						if (control instanceof ScrolledForm) {
							ScrolledForm form = (ScrolledForm) control;

							if (SYNC_MESSAGE.equals(form.getMessage())) {
								form.setMessage(null, IMessageProvider.NONE);
							}
						}
					}
				} else {
					completed.fail(new Exception("Model unavailable"));
				}
			});
		return completed.getPromise();
	}

	private void initPages(IEditorSite site, IEditorInput input) throws PartInitException {
		// Initialise pages
		sourcePage = new BndSourceEditorPage(SOURCE_PAGE, this);
		pageFactories.put(WORKSPACE_PAGE, WorkspacePage.MAIN_FACTORY);
		pageFactories.put(WORKSPACE_EXT_PAGE, WorkspacePage.EXT_FACTORY);
		pageFactories.put(CONTENT_PAGE, BundleContentPage.FACTORY);
		pageFactories.put(DESCRIPTION_PAGE, BundleDescriptionPage.FACTORY);
		pageFactories.put(BUILD_PAGE, ProjectBuildPage.FACTORY);
		pageFactories.put(PROJECT_RUN_PAGE, ProjectRunPage.FACTORY_PROJECT);
		pageFactories.put(BNDRUN_PAGE, ProjectRunPage.FACTORY_BNDRUN);
		pageFactories.put(TEST_SUITES_PAGE, TestSuitesPage.FACTORY);

		IConfigurationElement[] configElems = Platform.getExtensionRegistry()
			.getConfigurationElementsFor(Plugin.PLUGIN_ID, "editorPages");
		if (configElems != null)
			for (IConfigurationElement configElem : configElems) {
				String id = configElem.getAttribute("id");
				if (id != null) {
					if (pageFactories.containsKey(id))
						logger.logError("Duplicate form page ID: " + id, null);
					else
						pageFactories.put(id, new DelayedPageFactory(configElem));
				}
			}
		sourcePage.init(site, input);
		sourcePage.initialize(this);
	}

	private void setPartNameForInput(IEditorInput input) {
		Pair<String, String> fileAndProject = getFileAndProject(getEditorInput());

		String path = fileAndProject.getFirst();
		String projectName = fileAndProject.getSecond();

		String name = input.getName();
		if (isMainWorkspaceConfig(path, projectName) || isExtWorkspaceConfig(path, projectName)) {
			name = path;
		} else if (Project.BNDFILE.equals(name)) {
			IResource resource = ResourceUtil.getResource(input);
			if (resource != null)
				name = projectName;
		} else if (name.endsWith(".bnd")) {
			IResource resource = ResourceUtil.getResource(input);
			if (resource != null)
				name = projectName + "." + name.substring(0, name.length() - ".bnd".length());
		} else if (name.endsWith(".bndrun")) {
			name = name.substring(0, name.length() - ".bndrun".length());
		}
		setPartName(name);
	}

	@Override
	public void dispose() {
		IResource resource = ResourceUtil.getResource(getEditorInput());

		super.dispose();

		if (resource != null) {
			resource.getWorkspace()
				.removeResourceChangeListener(this);
		}

		LaunchUtils.endRun((Run) model.getProject());

		buildFileImg.dispose();
		if (resolveHandlerActivation != null) {
			resolveHandlerActivation.getHandlerService()
				.deactivateHandler(resolveHandlerActivation);
		}
		if (resolveJobListener != null) {
			Job.getJobManager()
				.removeJobChangeListener(resolveJobListener);
		}
	}

	public BndEditModel getEditModel() {
		return this.model;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResource myResource = ResourceUtil.getResource(getEditorInput());

		IResourceDelta delta = event.getDelta();
		if (delta == null)
			return;
		IPath fullPath = myResource.getFullPath();
		delta = delta.findMember(fullPath);
		if (delta == null)
			return;

		// Delegate to any interested pages
		for (Object page : pages) {
			if (page instanceof IResourceChangeListener) {
				((IResourceChangeListener) page).resourceChanged(event);
			}
		}

		// Close editor if file removed or switch to new location if file moved
		if (delta.getKind() == IResourceDelta.REMOVED) {
			if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
				IFile file = ResourcesPlugin.getWorkspace()
					.getRoot()
					.getFile(delta.getMovedToPath());
				final FileEditorInput newInput = new FileEditorInput(file);

				setInput(newInput);
				Display display = getEditorSite().getShell()
					.getDisplay();
				if (display != null) {
					SWTConcurrencyUtil.execForDisplay(display, true, () -> {
						setPartNameForInput(newInput);
						sourcePage.setInput(newInput);
					});
				}
			} else {
				close(false);
			}

		}
		// File content updated externally => reload all pages
		else if ((delta.getKind() & IResourceDelta.CHANGED) > 0 && (delta.getFlags() & IResourceDelta.CONTENT) > 0) {
			if (!saving.get()) {
				final IDocumentProvider docProvider = sourcePage.getDocumentProvider();
				// #1625: Ensure the IDocumentProvider is not null.
				if (docProvider != null) {
					final IDocument document = docProvider.getDocument(getEditorInput());
					SWTConcurrencyUtil.execForControl(getEditorSite().getShell(), true, () -> {
						try {
							model.loadFrom(new IDocumentWrapper(document));
							updateIncludedPages();
						} catch (IOException e) {
							logger.logError("Failed to reload document", e);
						}
					});
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (IContentOutlinePage.class == adapter) {
			return (T) new BndEditorContentOutlinePage(this, model);
		} else if (Control.class == adapter) {
			return getSourcePage().getAdapter(adapter);
		}
		return super.getAdapter(adapter);
	}

	/**
	 * Ensures the field values are updated if the file content is replaced
	 */
	private class ElementStateListener implements IElementStateListener {

		String savedString = null;

		@Override
		public void elementMoved(Object originalElement, Object movedElement) {}

		@Override
		public void elementDirtyStateChanged(Object element, boolean isDirty) {}

		@Override
		public void elementDeleted(Object element) {}

		@Override
		public void elementContentReplaced(Object element) {
			try {
				IDocumentProvider docProvider = sourcePage.getDocumentProvider();
				// #1625: Ensure the IDocumentProvider is not null.
				if (docProvider != null) {
					IDocumentWrapper idoc = new IDocumentWrapper(docProvider.getDocument(element));
					if (!saving.get()) {
						model.loadFrom(idoc);
					} else {
						if (savedString != null) {
							logger.logInfo("Putting back content that we almost lost!", null);
							try {
								idoc.replace(0, idoc.getLength(), savedString);
							} catch (BadLocationException e) {
								e.printStackTrace();
							}
						}
					}
				}
			} catch (IOException e) {
				logger.logError("Error loading model from document.", e);
			} finally {
				savedString = null;
			}
		}

		@Override
		public void elementContentAboutToBeReplaced(Object element) {
			// [cs] This check is here to attempt to save content that would be
			// thrown away by a (misbehaving?) version
			// control plugin.
			// Scenario: File is checked out by Perforce plugin.
			// This causes elementContentAboutToBeReplaced and
			// elementContentReplaced callbacks to be fired.
			// However -- by the time that elementContentReplaced is called, the
			// content inside of the IDocumentWrapper
			// is already replaced with the contents of the perforce file being
			// checked out.
			// To avoid losing changes, we need to save the content here, then
			// put that content BACK on to the document
			// in elementContentReplaced
			if (saving.get()) {
				logger.logInfo("Content about to be replaced... Save it.", null);
				IDocumentProvider docProvider = sourcePage.getDocumentProvider();
				// #1625: Ensure the IDocumentProvider is not null.
				if (docProvider != null)
					savedString = new IDocumentWrapper(docProvider.getDocument(element)).get();
			}
		}
	}

	public BndEditModel getModel() {
		return model;
	}

	private Pair<String, String> getFileAndProject(IEditorInput input) {
		String path;
		String projectName;
		if (inputResource != null) {
			path = inputResource.getProjectRelativePath()
				.toString();
			projectName = inputResource.getProject()
				.getName();
		} else {
			path = inputFile.getAbsolutePath();
			projectName = "none";
		}
		return Pair.newInstance(path, projectName);
	}

}
