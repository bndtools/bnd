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
package bndtools.editor.contents;


import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import bndtools.editor.GeneralInfoPart;
import bndtools.editor.MDSashForm;
import bndtools.editor.PrivatePackagesPart;
import bndtools.editor.SaneDetailsPart;
import bndtools.editor.model.BndEditModel;
import bndtools.editor.model.ExportedPackage;
import bndtools.editor.pkgpatterns.PkgPatternsDetailsPage;
import bndtools.utils.MessageHyperlinkAdapter;

public class BundleContentPage extends FormPage {

    private final BndEditModel model;
    private PrivatePackagesPart privPkgsPart;
    private ExportPatternsListPart exportPkgsPart;
    private Composite parent;

    public BundleContentPage(FormEditor editor, BndEditModel model, String id, String title) {
        super(editor, id, title);
        this.model = model;
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        FormToolkit toolkit = managedForm.getToolkit();
        managedForm.setInput(model);

        ScrolledForm scrolledForm = managedForm.getForm();
        scrolledForm.setText("Bundle Content");

        Form form = scrolledForm.getForm();
        toolkit.decorateFormHeading(form);
        form.addMessageHyperlinkListener(new MessageHyperlinkAdapter(getEditor()));
        Composite body = form.getBody();

        // Create controls
        MDSashForm sashForm = new MDSashForm(body, SWT.HORIZONTAL, managedForm);
        sashForm.setSashWidth(6);
        toolkit.adapt(sashForm, false, false);

        Composite leftPanel = toolkit.createComposite(sashForm);
        createLeftPanel(managedForm, leftPanel);
        Composite rightPanel = toolkit.createComposite(sashForm);
        createRightPanel(managedForm, rightPanel);

        sashForm.hookResizeListener();

        // Layout
        body.setLayout(new FillLayout());
    }

    void createLeftPanel(IManagedForm mform, Composite parent) {
        FormToolkit toolkit = mform.getToolkit();

        GeneralInfoPart infoPart = new GeneralInfoPart(parent, toolkit, Section.TITLE_BAR | Section.TWISTIE);
        mform.addPart(infoPart);

        privPkgsPart = new PrivatePackagesPart(parent, toolkit, Section.TITLE_BAR | Section.EXPANDED);
        mform.addPart(privPkgsPart);

        exportPkgsPart = new ExportPatternsListPart(parent, toolkit, Section.TITLE_BAR | Section.EXPANDED);
        mform.addPart(exportPkgsPart);

        // LAYOUT
        GridData gd;
        GridLayout layout;

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        parent.setLayoutData(gd);

        layout = new GridLayout(1, false);
        parent.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        infoPart.getSection().setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        privPkgsPart.getSection().setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        exportPkgsPart.getSection().setLayoutData(gd);
    }

    void createRightPanel(IManagedForm mform, final Composite parent) {
        this.parent = parent;
        FormToolkit toolkit = mform.getToolkit();

        final Composite detailsPanel = toolkit.createComposite(parent);
        SaneDetailsPart detailsPart = new SaneDetailsPart();
        mform.addPart(detailsPart);

        BundleCalculatedImportsPart importsPart = new BundleCalculatedImportsPart(parent, toolkit, Section.TITLE_BAR | Section.TWISTIE);
        mform.addPart(importsPart);

        PkgPatternsDetailsPage page = new PkgPatternsDetailsPage(exportPkgsPart, "Export Pattern");
        detailsPart.registerPage(ExportedPackage.class, page);
        detailsPart.createContents(toolkit, detailsPanel);

        importsPart.getSection().addExpansionListener(new ExpansionAdapter() {
            @Override
            public void expansionStateChanged(ExpansionEvent e) {
                super.expansionStateChanged(e);
                parent.layout(true, true);
            }
            @Override
            public void expansionStateChanging(ExpansionEvent e) {
                super.expansionStateChanging(e);
            }
        });

        FormLayout layout;
        FormData fd;

        layout = new FormLayout();
        parent.setLayout(layout);

        // Attach importsPart to the bottom & fill width
        fd = new FormData(SWT.DEFAULT, SWT.DEFAULT);
        fd.left = new FormAttachment(0, 5);
        fd.right = new FormAttachment(100, -5);
        fd.bottom = new FormAttachment(100, 5);
        importsPart.getSection().setLayoutData(fd);

        // Attach detailsPanel to the top & fill width
        fd = new FormData(SWT.DEFAULT, SWT.DEFAULT);
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        fd.top = new FormAttachment(0);
        fd.bottom = new FormAttachment(importsPart.getSection());
        detailsPanel.setLayoutData(fd);
    }

    public void setSelectedExport(ExportedPackage export) {
        exportPkgsPart.getSelectionProvider().setSelection(new StructuredSelection(export));
    }

    public void setSelectedPrivatePkg(String pkg) {
        privPkgsPart.getSelectionProvider().setSelection(new StructuredSelection(pkg));
    }
}