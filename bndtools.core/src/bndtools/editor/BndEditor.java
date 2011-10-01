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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.List;
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
import aQute.lib.osgi.Constants;
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

    static final String CONTENT_PAGE = "__content_page";
	static final String BUILD_PAGE = "__build_page";
    static final String PROJECT_RUN_PAGE = "__project_run_page";
	static final String COMPONENTS_PAGE = "__components_page";
	static final String TEST_SUITES_PAGE = "__test_suites_page";
	static final String POLICIES_PAGE = "__policies_page";
	static final String SOURCE_PAGE = "__source_page";
	static final String WORKSPACE_PAGE = "__workspace_page";

	private final BndEditModel model = new BndEditModel();
	private final BndSourceEditorPage sourcePage = new BndSourceEditorPage(SOURCE_PAGE, this);

	private final PropertyChangeListener modelListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            if (Constants.SUB.equals(evt.getPropertyName())) {
                @SuppressWarnings("unchecked")
                List<String> newSubBundles = (List<String>) evt.getNewValue();
                if (newSubBundles != null && !newSubBundles.isEmpty()) {
                    removePage(CONTENT_PAGE);
                    removePage(COMPONENTS_PAGE);
                    removePage(TEST_SUITES_PAGE);
                } else {
                    BndEditor editor = BndEditor.this;

                    ensurePageExists(CONTENT_PAGE, new BundleContentPage(editor, model, CONTENT_PAGE, "Content"), 0);
                    ensurePageExists(COMPONENTS_PAGE, new ComponentsPage(editor, model, COMPONENTS_PAGE, "Components"), 3);
                    ensurePageExists(TEST_SUITES_PAGE, new TestSuitesPage(editor, model, TEST_SUITES_PAGE, "Tests"), 4);
                }
            }
        }
    };

    private final AtomicBoolean saving = new AtomicBoolean(false);

	@Override
	public void doSave(IProgressMonitor monitor) {
		if(sourcePage.isActive() && sourcePage.isDirty()) {
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
        try {
            String fileName;
            String projectName;

            IEditorInput input = getEditorInput();
            if (input instanceof IFileEditorInput) {
                IFile file = ((IFileEditorInput) input).getFile();
                fileName = file.getName();
                projectName = file.getProject().getName();
            } else {
                fileName = input.getName();
                projectName = null;
            }

            if (Workspace.BUILDFILE.equals(fileName) && (Workspace.CNFDIR.equals(projectName) || Workspace.BNDDIR.equals(projectName))) {
                addPagesBuildBnd();
            } else if (fileName.endsWith(LaunchConstants.EXT_BNDRUN)) {
                addPagesBndRun();
            } else {
                addPagesBnd(fileName);
            }
            int sourcePageIndex = addPage(sourcePage, getEditorInput());
            setPageText(sourcePageIndex, "Source");
        } catch (PartInitException e) {
            Plugin.logError("Error adding page(s) to the editor.", e);
        }
    }

    private void addPagesBuildBnd() throws PartInitException {
        addPage(new WorkspacePage(this, model, WORKSPACE_PAGE, "Workspace"));
    }

    private void addPagesBnd(String inputName) throws PartInitException {
        boolean isProjectFile = Project.BNDFILE.equals(inputName);

        List<String> subBndFiles = model.getSubBndFiles();
        boolean isSubBundles = subBndFiles != null && !subBndFiles.isEmpty();

        if (!isSubBundles) {
            BundleContentPage contentPage = new BundleContentPage(this, model, CONTENT_PAGE, "Bundle Content");
            addPage(contentPage);
        }

        if (isProjectFile) {
            ProjectBuildPage buildPage = new ProjectBuildPage(this, model, BUILD_PAGE, "Build");
            addPage(buildPage);

            ProjectRunPage runPage = new ProjectRunPage(this, model, PROJECT_RUN_PAGE, "Run");
            addPage(runPage);
        }

        if (!isSubBundles) {
            ComponentsPage componentsPage = new ComponentsPage(this, model, COMPONENTS_PAGE, "Components");
            addPage(componentsPage);

            TestSuitesPage testSuitesPage = new TestSuitesPage(this, model, TEST_SUITES_PAGE, "Tests");
            addPage(testSuitesPage);
        }
    }

    private void addPagesBndRun() throws PartInitException {
        ProjectRunPage runPage = new ProjectRunPage(this, model, PROJECT_RUN_PAGE, "Run");
        addPage(runPage);
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
			model.addPropertyChangeListener(modelListener);
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
	    if (model != null)
	        model.removePropertyChangeListener(modelListener);

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