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
package bndtools.jareditor.internal;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.ide.ResourceUtil;

import bndtools.utils.SWTConcurrencyUtil;

public class JAREditor extends FormEditor implements IResourceChangeListener {

    JARContentPage contentPage = new JARContentPage(this, "contentPage", "Content");
    JARPrintPage printPage = new JARPrintPage(this, "printPage", "Print");

    @Override
    protected void addPages() {
        try {
            addPage(contentPage);
            addPage(printPage);
        } catch (PartInitException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    public void doSaveAs() {
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        super.init(site, input);

        IResource resource = ResourceUtil.getResource(input);
        if (resource != null) {
            resource.getWorkspace().addResourceChangeListener(this);
        }
    }

    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
        if (input instanceof IFileEditorInput) {
            String name = ((IFileEditorInput) input).getFile().getName();
            setPartName(name);
        }
    }

    protected void updateContent(final IEditorInput input) {
        Runnable update = new Runnable() {
            public void run() {
                if (contentPage != null && !contentPage.getPartControl().isDisposed()) {
                    contentPage.getManagedForm().refresh();
                }

                if (printPage != null && !printPage.getPartControl().isDisposed()) {
                    printPage.refresh();
                }
            }
        };
        try {
            SWTConcurrencyUtil.execForDisplay(contentPage.getPartControl().getDisplay(), update);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dispose() {
        IResource resource = ResourceUtil.getResource(getEditorInput());

        super.dispose();

        if(resource != null) {
            resource.getWorkspace().removeResourceChangeListener(this);
        }
    }

    public void resourceChanged(IResourceChangeEvent event) {
        IResource myResource = ResourceUtil.getResource(getEditorInput());

        IResourceDelta delta = event.getDelta();
        IPath fullPath = myResource.getFullPath();
        delta = delta.findMember(fullPath);
        if(delta == null)
            return;

        if(delta.getKind() == IResourceDelta.REMOVED) {
            close(false);
        } else if(delta.getKind() == IResourceDelta.CHANGED) {
            updateContent(getEditorInput());
        }
    }
}
