/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools.editor;

import java.io.IOException;
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
import org.bndtools.api.ResolveMode;
import org.bndtools.core.resolve.ResolutionResult;
import org.bndtools.core.resolve.ResolveJob;
import org.bndtools.core.resolve.ui.ResolutionWizard;
import org.bndtools.core.ui.ExtendedFormEditor;
import org.bndtools.core.ui.IFormPageFactory;
import org.bndtools.utils.swt.SWTConcurrencyUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.properties.BadLocationException;
import bndtools.BndConstants;
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
import bndtools.preferences.BndPreferences;
import bndtools.types.Pair;

public class BndEditor extends ExtendedFormEditor implements IResourceChangeListener {
    private static final ILogger logger = Logger.getLogger(BndEditor.class);

    public static final String WORKSPACE_EDITOR = "bndtools.bndWorkspaceConfigEditor";

    public static final String SOURCE_PAGE = "__source_page";

    public static final String CONTENT_PAGE = "__content_page";
    public static final String WORKSPACE_PAGE = "__workspace_page";
    public static final String WORKSPACE_EXT_PAGE = "__workspace_ext_page";
    public static final String DESCRIPTION_PAGE = "__description_page";
    public static final String BUILD_PAGE = "__build_page";
    public static final String PROJECT_RUN_PAGE = "__project_run_page";
    public static final String BNDRUN_PAGE = "__bndrun_page";
    public static final String TEST_SUITES_PAGE = "__test_suites_page";

    private final Map<String,IFormPageFactory> pageFactories = new LinkedHashMap<String,IFormPageFactory>();

    private final BndEditModel model = new BndEditModel();
    private final BndSourceEditorPage sourcePage = new BndSourceEditorPage(SOURCE_PAGE, this);

    private final Image buildFileImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/bndtools-logo-16x16.png").createImage();

    public BndEditor() {
        pageFactories.put(WORKSPACE_PAGE, WorkspacePage.MAIN_FACTORY);
        pageFactories.put(WORKSPACE_EXT_PAGE, WorkspacePage.EXT_FACTORY);
        pageFactories.put(CONTENT_PAGE, BundleContentPage.FACTORY);
        pageFactories.put(DESCRIPTION_PAGE, BundleDescriptionPage.FACTORY);
        pageFactories.put(BUILD_PAGE, ProjectBuildPage.FACTORY);
        pageFactories.put(PROJECT_RUN_PAGE, ProjectRunPage.FACTORY_PROJECT);
        pageFactories.put(BNDRUN_PAGE, ProjectRunPage.FACTORY_BNDRUN);
        pageFactories.put(TEST_SUITES_PAGE, TestSuitesPage.FACTORY);

        IConfigurationElement[] configElems = Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, "editorPages");
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
    }

    static Pair<String,String> getFileAndProject(IEditorInput input) {
        String path;
        String projectName;
        if (input instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput) input).getFile();
            path = file.getProjectRelativePath().toString();
            projectName = file.getProject().getName();
        } else {
            path = input.getName();
            projectName = null;
        }
        return Pair.newInstance(path, projectName);
    }

    void updatePages() {
        List<String> requiredPageIds = new LinkedList<String>();

        // Need to know the file and project names.
        Pair<String,String> fileAndProject = getFileAndProject(getEditorInput());
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
        Map<String,IFormPage> pageCache = new HashMap<String,IFormPage>(requiredPageIds.size());
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
                IFormPage page = SOURCE_PAGE.equals(pageId) ? sourcePage : pageFactories.get(pageId).createPage(this, model, pageId);
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
        if (Workspace.CNFDIR.equals(projectName) || Workspace.BNDDIR.equals(projectName)) {
            return Workspace.BUILDFILE.equals(path);
        }
        return false;
    }

    private static boolean isExtWorkspaceConfig(String path, String projectName) {
        if (Workspace.CNFDIR.equals(projectName) || Workspace.BNDDIR.equals(projectName)) {
            return path.startsWith("ext/") && path.endsWith(".bnd");
        }
        return false;
    }

    private final AtomicBoolean saving = new AtomicBoolean(false);

    @Override
    public void doSave(IProgressMonitor monitor) {
        final Shell shell = getEditorSite().getShell();

        // Commit dirty pages
        if (sourcePage.isActive() && sourcePage.isDirty()) {
            sourcePage.commit(true);
        } else {
            commitPages(true);
            sourcePage.refresh();
        }

        ResolveMode resolveMode = getResolveMode();

        // If auto resolve, then resolve and save in background thread.
        if (resolveMode == ResolveMode.auto && !PlatformUI.getWorkbench().isClosing()) {
            final IFile file = ResourceUtil.getFile(getEditorInput());
            if (file == null) {
                MessageDialog.openError(shell, "Resolution Error", "Unable to run resolution because the file is not in the workspace. NB.: the file will still be saved.");
                reallySave(monitor);
                return;
            }

            // Create resolver job and pre-validate
            final ResolveJob job = new ResolveJob(model);
            IStatus validation = job.validateBeforeRun();
            if (!validation.isOK()) {
                String message = "Unable to run the resolver. NB.: the file will still be saved.";
                ErrorDialog.openError(shell, "Resolution Validation Problem", message, validation, IStatus.ERROR | IStatus.WARNING);
                reallySave(monitor);
                return;
            }

            // Add operation to perform at the end of resolution (i.e. display
            // results and actually save the file)
            final UIJob completionJob = new UIJob(shell.getDisplay(), "Display Resolution Results") {
                @Override
                public IStatus runInUIThread(IProgressMonitor monitor) {
                    ResolutionResult result = job.getResolutionResult();
                    ResolutionWizard wizard = new ResolutionWizard(model, file, result);
                    if (result.getOutcome() != ResolutionResult.Outcome.Resolved /*|| !result.getResolve().getOptionalResources().isEmpty() */) {
                        WizardDialog dialog = new WizardDialog(shell, wizard);
                        if (dialog.open() != Window.OK) {
                            if (!wizard.performFinish()) {
                                MessageDialog.openError(shell, "Error", "Unable to store resolution results into Run Bundles list.");
                            }
                        }
                    } else {
                        if (!wizard.performFinish()) {
                            MessageDialog.openError(shell, "Error", "Unable to store resolution results into Run Bundles list.");
                        }
                    }
                    reallySave(monitor);
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
        } else {
            // Not auto-resolving, just save
            reallySave(monitor);
        }
    }

    private ResolveMode getResolveMode() {
        ResolveMode resolveMode = ResolveMode.manual;
        try {
            String str = (String) model.genericGet(BndConstants.RESOLVE_MODE);
            if (str != null)
                resolveMode = Enum.valueOf(ResolveMode.class, str);
        } catch (Exception e) {
            logger.logError("Error parsing '-resolve' header.", e);
        }
        return resolveMode;
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
            updatePages();
        } finally {
            this.saving.set(false);
        }
    }

    protected void ensurePageExists(String pageId, IFormPage page, int index) {
        IFormPage existingPage = findPage(pageId);
        if (existingPage != null)
            return;

        try {
            addPage(index, page);
        } catch (PartInitException e) {
            ErrorDialog.openError(getSite().getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error adding page to editor.", e));
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
        updatePages();

        showHighestPriorityPage();
    }

    void showHighestPriorityPage() {
        int selectedPrio = Integer.MIN_VALUE;
        String selected = null;

        BndPreferences prefs = new BndPreferences();
        if (prefs.getEditorOpenSourceTab()) {
            selected = SOURCE_PAGE;
            selectedPrio = 0;
        }

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

        if (selected != null)
            setActivePage(selected);
    }

    private List<String> getPagesBnd(String fileName) {
        List<String> pages = new ArrayList<String>(5);

        boolean isProjectFile = Project.BNDFILE.equals(fileName);
        List<String> subBndFiles = model.getSubBndFiles();
        boolean isSubBundles = subBndFiles != null && !subBndFiles.isEmpty();

        for (Entry<String,IFormPageFactory> pageEntry : pageFactories.entrySet()) {
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
        List<String> pageIds = new ArrayList<String>(3);
        for (Entry<String,IFormPageFactory> pageEntry : pageFactories.entrySet()) {
            if (pageEntry.getValue().supportsMode(IFormPageFactory.Mode.bndrun))
                pageIds.add(pageEntry.getKey());
        }
        return pageIds;
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        super.init(site, input);
        sourcePage.init(site, input);

        setSourcePage(sourcePage);

        setPartNameForInput(input);

        IResource resource = ResourceUtil.getResource(input);
        String resourceName;
        if (resource != null) {
            resource.getWorkspace().addResourceChangeListener(this);
            resourceName = resource.getName();
        } else {
            IStorage storage = (IStorage) input.getAdapter(IStorage.class);
            if (storage != null) {
                resourceName = storage.getName();
            } else {
                resourceName = input.getName();
            }
        }

        final IDocumentProvider docProvider = sourcePage.getDocumentProvider();
        IDocument document = docProvider.getDocument(input);
        try {
            model.loadFrom(new IDocumentWrapper(document));
            model.setBndResourceName(resourceName);

            if (resource != null) {
                model.setBndResource(resource.getLocation().toFile());
            }
            // model.addPropertyChangeListener(modelListener);
        } catch (IOException e) {
            throw new PartInitException("Error reading editor input.", e);
        }

        // Ensure the field values are updated if the file content is replaced
        docProvider.addElementStateListener(new IElementStateListener() {

            String savedString = null;

            public void elementMoved(Object originalElement, Object movedElement) {}

            public void elementDirtyStateChanged(Object element, boolean isDirty) {}

            public void elementDeleted(Object element) {}

            public void elementContentReplaced(Object element) {
                try {
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
                } catch (IOException e) {
                    logger.logError("Error loading model from document.", e);
                } finally {
                    savedString = null;
                }
            }

            public void elementContentAboutToBeReplaced(Object element) {
                // [cs] This check is here to attempt to save content that would be thrown away by a (misbehaving?) version control plugin.
                // Scenario: File is checked out by Perforce plugin. 
                // This causes elementContentAboutToBeReplaced and elementContentReplaced callbacks to be fired.
                // However -- by the time that elementContentReplaced is called, the content inside of the IDocumentWrapper
                // is already replaced with the contents of the perforce file being checked out.
                // To avoid losing changes, we need to save the content here, then put that content BACK on to the document
                // in elementContentReplaced 
                if (saving.get()) {
                    logger.logInfo("Content about to be replaced... Save it.", null);
                    savedString = new IDocumentWrapper(docProvider.getDocument(element)).get();
                }
            }
        });
    }

    private static Workspace getWorkspace() {
        try {
            return Central.getWorkspace();
        } catch (Exception e) {
            return null;
        }
    }

    private void setPartNameForInput(IEditorInput input) {
        Pair<String,String> fileAndProject = getFileAndProject(input);
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
            resource.getWorkspace().removeResourceChangeListener(this);
        }

        buildFileImg.dispose();
    }

    public BndEditModel getEditModel() {
        return this.model;
    }

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
                IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(delta.getMovedToPath());
                final FileEditorInput newInput = new FileEditorInput(file);

                setInput(newInput);
                Display display = getEditorSite().getShell().getDisplay();
                if (display != null) {
                    SWTConcurrencyUtil.execForDisplay(display, true, new Runnable() {
                        public void run() {
                            setPartNameForInput(newInput);
                            sourcePage.setInput(newInput);
                        }
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
                final IDocument document = docProvider.getDocument(getEditorInput());
                SWTConcurrencyUtil.execForControl(getEditorSite().getShell(), true, new Runnable() {
                    public void run() {
                        try {
                            model.loadFrom(new IDocumentWrapper(document));
                            updatePages();
                        } catch (IOException e) {
                            logger.logError("Failed to reload document", e);
                        }
                    }
                });
            }
        }
    }

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
        if (IContentOutlinePage.class == adapter) {
            return new BndEditorContentOutlinePage(this, model);
        }
        return super.getAdapter(adapter);
    }
}