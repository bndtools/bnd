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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import bndtools.Plugin;
import bndtools.editor.model.BndEditModel;
import bndtools.editor.pages.BundleContentPage;
import bndtools.editor.pages.ComponentsPage;
import bndtools.editor.pages.ProjectBuildPage;
import bndtools.editor.pages.ProjectRunPage;
import bndtools.editor.pages.TestSuitesPage;
import bndtools.editor.pages.WorkspacePage;
import bndtools.launch.LaunchConstants;
import bndtools.utils.SWTConcurrencyUtil;

public class BndEditor extends FormEditor implements IResourceChangeListener {

    public static final String WORKSPACE_EDITOR  = "bndtools.bndWorkspaceConfigEditor";

    static final String WORKSPACE_PAGE = "__workspace_page";
    static final String WORKSPACE_EXT_PAGE = "__workspace_ext_page";
    static final String CONTENT_PAGE = "__content_page";
	static final String BUILD_PAGE = "__build_page";
    static final String PROJECT_RUN_PAGE = "__project_run_page";
	static final String COMPONENTS_PAGE = "__components_page";
	static final String TEST_SUITES_PAGE = "__test_suites_page";
	static final String SOURCE_PAGE = "__source_page";

	private final Map<String, IPageFactory> pageFactories = new HashMap<String, IPageFactory>();

	private final BndEditModel model = new BndEditModel();
	private final BndSourceEditorPage sourcePage = new BndSourceEditorPage(SOURCE_PAGE, this);

    public BndEditor() {
        pageFactories.put(WORKSPACE_PAGE, WorkspacePage.MAIN_FACTORY);
        pageFactories.put(WORKSPACE_EXT_PAGE, WorkspacePage.EXT_FACTORY);
        pageFactories.put(CONTENT_PAGE, BundleContentPage.FACTORY);
        pageFactories.put(BUILD_PAGE, ProjectBuildPage.FACTORY);
        pageFactories.put(PROJECT_RUN_PAGE, ProjectRunPage.FACTORY);
        pageFactories.put(COMPONENTS_PAGE, ComponentsPage.FACTORY);
        pageFactories.put(TEST_SUITES_PAGE, TestSuitesPage.FACTORY);
    }

    void updatePages() {
        List<String> requiredPageIds = new LinkedList<String>();

        // Need to know the file and project names.
        String path;
        String projectName;

        IEditorInput input = getEditorInput();
        if (input instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput) input).getFile();
            path = file.getProjectRelativePath().toString();
            projectName = file.getProject().getName();
        } else {
            path = input.getName();
            projectName = null;
        }

        if (isMainWorkspaceConfig(path, projectName)) {
            requiredPageIds.add(WORKSPACE_PAGE);
        } else if (isExtWorkspaceConfig(path, projectName)) {
            requiredPageIds.add(WORKSPACE_EXT_PAGE);
        } else if (path.endsWith(LaunchConstants.EXT_BNDRUN)) {
            requiredPageIds.addAll(getPagesBndRun());
        } else {
            requiredPageIds.addAll(getPagesBnd(path));
        }

        IFormPage activePage = getActivePageInstance();
        String currentPageId = activePage != null ? activePage.getId() : null;

        // Remove all pages except source
        while (getPageCount() > 1) removePage(0);

        // Add required pages;
        for (ListIterator<String> iter = requiredPageIds.listIterator(requiredPageIds.size()); iter.hasPrevious(); ) {
            String pageId = iter.previous();
            IPageFactory pf = pageFactories.get(pageId);
            if (pf == null)
                Plugin.log(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, "No page factory available for page ID: " + pageId, null));
            else {
                try {
                    IFormPage page = pf.createPage(this, model, pageId);
                    addPage(0, page);
                } catch (IllegalArgumentException e) {
                    Plugin.logError("Error creating page for ID: " + pageId, e);
                } catch (PartInitException e) {
                    Plugin.logError("Error adding page(s) to the editor.", e);
                }
            }
        }
        // Always add the source page if it doesn't exist
        try {
            if (findPage(SOURCE_PAGE) == null) {
                int sourcePageIndex = addPage(sourcePage, getEditorInput());
                setPageText(sourcePageIndex, "Source");
            }
        } catch (PartInitException e) {
            Plugin.logError("Error adding page(s) to the editor.", e);
        }

        // Restore the current selected page
        if (currentPageId != null) {
            setActivePage(currentPageId);
        }
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
        if (sourcePage.isActive() && sourcePage.isDirty()) {
            sourcePage.commit(true);
        } else {
            commitPages(true);
            sourcePage.refresh();
        }
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
    }

    private List<String> getPagesBnd(String fileName) {
        List<String> pages = new ArrayList<String>(5);

        boolean isProjectFile = Project.BNDFILE.equals(fileName);
        List<String> subBndFiles = model.getSubBndFiles();
        boolean isSubBundles = subBndFiles != null && !subBndFiles.isEmpty();

        if (!isSubBundles)
            pages.add(CONTENT_PAGE);

        if (isProjectFile) {
            pages.add(BUILD_PAGE);
            pages.add(PROJECT_RUN_PAGE);
        }

        if (!isSubBundles) {
            pages.add(COMPONENTS_PAGE);
            pages.add(TEST_SUITES_PAGE);
        }

        return pages;
    }

    private List<String> getPagesBndRun() {
        return Collections.singletonList(PROJECT_RUN_PAGE);
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
        String name = input.getName();
        if (Project.BNDFILE.equals(name)) {
            IResource resource = ResourceUtil.getResource(input);
            if (resource != null)
                name = resource.getProject().getName();
        } else if(name.endsWith(".bnd")) {
            name = name.substring(0, name.length() - ".bnd".length());
        } else if(name.endsWith(".bndrun")) {
            name = name.substring(0, name.length() - ".bndrun".length());
        }
        setPartName(name);
    }

	@Override
	public void dispose() {
//	    if (model != null)
//	        model.removePropertyChangeListener(modelListener);

		IResource resource = ResourceUtil.getResource(getEditorInput());

		super.dispose();

		if(resource != null) {
			resource.getWorkspace().removeResourceChangeListener(this);
		}
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
        } else if ( (delta.getKind() & IResourceDelta.CHANGED) > 0 && (delta.getFlags() & IResourceDelta.CONTENT) > 0) {
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