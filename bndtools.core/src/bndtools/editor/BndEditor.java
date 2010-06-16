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
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import aQute.bnd.build.Project;
import bndtools.Plugin;
import bndtools.editor.model.BndEditModel;
import bndtools.editor.pages.BundleBuildPage;
import bndtools.editor.pages.BundleContentPage;
import bndtools.editor.pages.ComponentsPage;
import bndtools.editor.pages.PoliciesPage;
import bndtools.editor.pages.ProjectBuildPage;
import bndtools.editor.pages.ProjectRunPage;
import bndtools.editor.pages.TestSuitesPage;
import bndtools.launch.LaunchConstants;

public class BndEditor extends FormEditor implements IResourceChangeListener {

    static final String CONTENT_PAGE = "__content_page";
	static final String BUILD_PAGE = "__build_page";
    static final String PROJECT_RUN_PAGE = "__project_run_page";
	static final String COMPONENTS_PAGE = "__components_page";
	static final String TEST_SUITES_PAGE = "__test_suites_page";
	static final String POLICIES_PAGE = "__policies_page";
	static final String SOURCE_PAGE = "__source_page";

	private final BndEditModel model = new BndEditModel();
	private final BndSourceEditorPage sourcePage = new BndSourceEditorPage(SOURCE_PAGE, this);

	@Override
	public void doSave(IProgressMonitor monitor) {
		if(sourcePage.isActive() && sourcePage.isDirty()) {
			sourcePage.commit(true);
		} else {
			commitPages(true);
			sourcePage.refresh();
		}
		sourcePage.doSave(monitor);
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
		    String inputName = getEditorInput().getName();
		    if(inputName.endsWith(LaunchConstants.EXT_BNDRUN)) {
		        addPagesBndRun();
		    } else {
		        addPagesBnd(inputName);
		    }
	        int sourcePageIndex = addPage(sourcePage, getEditorInput());
	        setPageText(sourcePageIndex, "Source");
		} catch (PartInitException e) {
		    Plugin.logError("Error adding page(s) to the editor.", e);
		}
	}

	private void addPagesBnd(String inputName) throws PartInitException {
        BundleContentPage contentPage = new BundleContentPage(this, model, CONTENT_PAGE, "Bundle Content");
        addPage(contentPage);

        if(Project.BNDFILE.equals(inputName)) {
            ProjectBuildPage buildPage = new ProjectBuildPage(this, model, BUILD_PAGE, "Build");
            addPage(buildPage);

            ProjectRunPage runPage = new ProjectRunPage(this, model, PROJECT_RUN_PAGE, "Run");
            addPage(runPage);
        } else {
            BundleBuildPage buildPage = new BundleBuildPage(this, model, BUILD_PAGE, "Build");
            addPage(buildPage);
        }

        ComponentsPage componentsPage = new ComponentsPage(this, model, COMPONENTS_PAGE, "Components");
        addPage(componentsPage);

        TestSuitesPage testSuitesPage = new TestSuitesPage(this, model, TEST_SUITES_PAGE, "Tests");
        addPage(testSuitesPage);

        PoliciesPage importsPage = new PoliciesPage(this, model, POLICIES_PAGE, "Policies");
        addPage(importsPage);
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
                    System.out.println("--> Content Replaced");
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
        IPath fullPath = myResource.getFullPath();
        delta = delta.findMember(fullPath);
        if (delta == null)
            return;

        if (delta.getKind() == IResourceDelta.REMOVED) {
            if ((delta.getFlags() & IResourceDelta.MOVED_TO) > 0) {
                IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(delta.getMovedToPath());
                FileEditorInput newInput = new FileEditorInput(file);

                setInput(newInput);
                setPartNameForInput(newInput);
                sourcePage.setInput(newInput);
            } else {
                close(false);
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