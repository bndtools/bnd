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

import org.bndtools.core.obr.ObrResolutionJob;
import org.bndtools.core.obr.ObrResolutionResult;
import org.bndtools.core.ui.ExtendedFormEditor;
import org.bndtools.core.ui.IFormPageFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
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
import bndtools.Plugin;
import bndtools.api.ResolveMode;
import bndtools.editor.common.IPriority;
import bndtools.editor.model.BndEditModel;
import bndtools.editor.pages.BundleContentPage;
import bndtools.editor.pages.ComponentsPage;
import bndtools.editor.pages.ProjectBuildPage;
import bndtools.editor.pages.ProjectRunPage;
import bndtools.editor.pages.TestSuitesPage;
import bndtools.editor.pages.WorkspacePage;
import bndtools.launch.LaunchConstants;
import bndtools.types.Pair;
import bndtools.utils.SWTConcurrencyUtil;
import bndtools.wizards.obr.ObrResolutionWizard;

public class BndEditor extends ExtendedFormEditor implements IResourceChangeListener {

    public static final String WORKSPACE_EDITOR  = "bndtools.bndWorkspaceConfigEditor";

    static final String WORKSPACE_PAGE = "__workspace_page";
    static final String WORKSPACE_EXT_PAGE = "__workspace_ext_page";
    static final String CONTENT_PAGE = "__content_page";
    static final String BUILD_PAGE = "__build_page";
    static final String PROJECT_RUN_PAGE = "__project_run_page";
    static final String COMPONENTS_PAGE = "__components_page";
    static final String TEST_SUITES_PAGE = "__test_suites_page";
    static final String SOURCE_PAGE = "__source_page";

    private final Map<String, IFormPageFactory> pageFactories = new LinkedHashMap<String, IFormPageFactory>();

    private final BndEditModel model = new BndEditModel();
    private final BndSourceEditorPage sourcePage = new BndSourceEditorPage(SOURCE_PAGE, this);

    private final Image buildFileImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/bndtools-logo-16x16.png").createImage();

    public BndEditor() {
        pageFactories.put(WORKSPACE_PAGE, WorkspacePage.MAIN_FACTORY);
        pageFactories.put(WORKSPACE_EXT_PAGE, WorkspacePage.EXT_FACTORY);
        pageFactories.put(CONTENT_PAGE, BundleContentPage.FACTORY);
        pageFactories.put(BUILD_PAGE, ProjectBuildPage.FACTORY);
        pageFactories.put(PROJECT_RUN_PAGE, ProjectRunPage.FACTORY);
        pageFactories.put(COMPONENTS_PAGE, ComponentsPage.FACTORY);
        pageFactories.put(TEST_SUITES_PAGE, TestSuitesPage.FACTORY);
        
        IConfigurationElement[] configElems = Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, "editorPages");
        if (configElems != null) for (IConfigurationElement configElem : configElems) {
            String id = configElem.getAttribute("id");
            if (id != null) {
                if (pageFactories.containsKey(id))
                    Plugin.logError("Duplicate form page ID: " + id, null);
                else
                    pageFactories.put(id, new DelayedPageFactory(configElem));
            }
        }
    }

    Pair<String, String> getFileAndProject(IEditorInput input) {
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

        IFormPage activePage = getActivePageInstance();
        String currentPageId = activePage != null ? activePage.getId() : null;

        // Remove pages no longer required and remember the rest in a map
        int i = 0;
        Map<String, IFormPage> pageCache = new HashMap<String, IFormPage>(requiredPageIds.size());
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
                }
                else {
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
                Plugin.logError("Error adding page(s) to the editor.", e);
            }
            requiredPointer++;
        }

        // Set the source page title
        setPageText(sourcePage.getIndex(), "Source");

    }

    private boolean isMainWorkspaceConfig(String path, String projectName) {
        if (Workspace.CNFDIR.equals(projectName) || Workspace.BNDDIR.equals(projectName)) {
            return Workspace.BUILDFILE.equals(path);
        }
        return false;
    }

    private boolean isExtWorkspaceConfig(String path, String projectName) {
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
        ResolveMode resolveMode = model.getResolveMode();

        // If auto resolve, then resolve and save in background thread.
        if (resolveMode == ResolveMode.auto && !PlatformUI.getWorkbench().isClosing()) {
            final IFile file = ResourceUtil.getFile(getEditorInput());
            if (file == null) {
                MessageDialog.openError(shell, "Resolution Error", "Unable to run OBR resolution because the file is not in the workspace. NB.: the file will still be saved.");
                reallySave(monitor);
                return;
            }

            // Create resolver job and pre-validate
            final ObrResolutionJob job = new ObrResolutionJob(file, model);
            IStatus validation = job.validateBeforeRun();
            if (!validation.isOK()) {
                String message = "Unable to run the OBR resolver. NB.: the file will still be saved.";
                ErrorDialog.openError(shell, "Resolution Validation Problem", message, validation, IStatus.ERROR | IStatus.WARNING);
                reallySave(monitor);
                return;
            }

            // Add operation to perform at the end of resolution (i.e. display results and actually save the file)
            final UIJob completionJob = new UIJob(shell.getDisplay(), "Display Resolution Results") {
                @Override
                public IStatus runInUIThread(IProgressMonitor monitor) {
                    ObrResolutionResult result = job.getResolutionResult();
                    ObrResolutionWizard wizard = new ObrResolutionWizard(model, file, result);
                    if (!result.isResolved() || !result.getOptional().isEmpty()) {
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

    private void reallySave(IProgressMonitor monitor) {
        // Actually save, via the source editor
        try {
            boolean saveLocked = this.saving.compareAndSet(false, true);
            if (!saveLocked) {
                Plugin.logError("Tried to save while already saving", null);
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
    public void doSaveAs() {
    }

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
        
        for (Entry<String, IFormPageFactory> pageEntry : pageFactories.entrySet()) {
            String pageId = pageEntry.getKey();
            IFormPageFactory page = pageEntry.getValue();
            
            if (!isSubBundles && page.supportsMode(IFormPageFactory.Mode.bundle))
                pages.add(pageId);
            else if (isProjectFile && page.supportsMode(IFormPageFactory.Mode.build))
                pages.add(pageId);
            else if (isProjectFile && page.supportsMode(IFormPageFactory.Mode.run))
                pages.add(pageId);
        }

        return pages;
    }

    private List<String> getPagesBndRun() {
        List<String> pageIds = new ArrayList<String>(3);
        for (Entry<String, IFormPageFactory> pageEntry : pageFactories.entrySet()) {
            if (pageEntry.getValue().supportsMode(IFormPageFactory.Mode.run))
                pageIds.add(pageEntry.getKey());
        }
        return pageIds;
    }

    @Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		sourcePage.init(site, input);

		setPartNameForInput(input);

		IResource resource = ResourceUtil.getResource(input);
		if(resource != null) {
		    resource.getWorkspace().addResourceChangeListener(this);
		}

		final IDocumentProvider docProvider = sourcePage.getDocumentProvider();
		IDocument document = docProvider.getDocument(input);
		try {
			model.loadFrom(document);
			model.setProjectFile(Project.BNDFILE.equals(input.getName()));
			model.setBndResource(resource);
//			model.addPropertyChangeListener(modelListener);
		} catch (IOException e) {
			throw new PartInitException("Error reading editor input.", e);
		}

		// Ensure the field values are updated if the file content is replaced
        docProvider.addElementStateListener(new IElementStateListener() {
            public void elementMoved(Object originalElement, Object movedElement) {
            }
            public void elementDirtyStateChanged(Object element, boolean isDirty) {
            }
            public void elementDeleted(Object element) {
            }
            public void elementContentReplaced(Object element) {
                try {
                    model.loadFrom(docProvider.getDocument(element));
                } catch (IOException e) {
                    Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading model from document.", e));
                }
            }
            public void elementContentAboutToBeReplaced(Object element) {
            }
        });
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
        } else if(name.endsWith(".bnd")) {
            IResource resource = ResourceUtil.getResource(input);
            if (resource != null)
                name = projectName + "." + name.substring(0, name.length() - ".bnd".length());
        } else if(name.endsWith(".bndrun")) {
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

	public BndEditModel getBndModel() {
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
            if ((delta.getFlags() & IResourceDelta.MOVED_TO) > 0) {
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
                            model.loadFrom(document);
                            updatePages();
                        } catch (IOException e) {
                            Plugin.logError("Failed to reload document", e);
                        }
                    }
                });
            }
        }
    }

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if(IContentOutlinePage.class == adapter) {
			return new BndEditorContentOutlinePage(this, model);
		}
		return super.getAdapter(adapter);
	}
}