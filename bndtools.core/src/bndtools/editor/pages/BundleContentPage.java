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
package bndtools.editor.pages;

import org.bndtools.core.ui.ExtendedFormEditor;
import org.bndtools.core.ui.IFormPageFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.build.model.clauses.ImportPattern;
import bndtools.editor.common.MDSashForm;
import bndtools.editor.contents.BundleCalculatedImportsPart;
import bndtools.editor.contents.GeneralInfoPart;
import bndtools.editor.contents.PrivatePackagesPart;
import bndtools.editor.exports.ExportPatternsListPart;
import bndtools.editor.imports.ImportPatternsListPart;
import bndtools.utils.MessageHyperlinkAdapter;

public class BundleContentPage extends FormPage {

    private final BndEditModel model;

    private PrivatePackagesPart privPkgsPart;
    private ImportPatternsListPart importPatternListPart;
    private ExportPatternsListPart exportPatternListPart;

    public static final IFormPageFactory FACTORY = new IFormPageFactory() {
        @Override
        public IFormPage createPage(ExtendedFormEditor editor, BndEditModel model, String id) throws IllegalArgumentException {
            return new BundleContentPage(editor, model, id, "Contents");
        }

        @Override
        public boolean supportsMode(Mode mode) {
            return mode == Mode.bundle;
        }
    };

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

        sashForm.setWeights(new int[] {
                1, 1
        });
        sashForm.hookResizeListener();

        // Layout
        body.setLayout(new FillLayout());
    }

    void createLeftPanel(IManagedForm mform, Composite parent) {
        FormToolkit toolkit = mform.getToolkit();

        GeneralInfoPart infoPart = new GeneralInfoPart(parent, toolkit, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
        mform.addPart(infoPart);

        privPkgsPart = new PrivatePackagesPart(parent, toolkit, Section.TITLE_BAR | Section.EXPANDED);
        mform.addPart(privPkgsPart);

        exportPatternListPart = new ExportPatternsListPart(parent, toolkit, Section.TITLE_BAR | Section.EXPANDED);
        mform.addPart(exportPatternListPart);

        // LAYOUT
        GridData gd;
        GridLayout layout;

        layout = new GridLayout(1, false);
        parent.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        infoPart.getSection().setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        privPkgsPart.getSection().setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        exportPatternListPart.getSection().setLayoutData(gd);
    }

    class NoSelectionPage extends AbstractFormPart implements IDetailsPage {
        @Override
        public void selectionChanged(IFormPart part, ISelection selection) {}

        @Override
        public void createContents(Composite parent) {
            FormToolkit toolkit = getManagedForm().getToolkit();
            // toolkit.createLabel(parent, "Nothing is selected");

            Section section = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
            section.setText("Selection Details");

            Composite composite = toolkit.createComposite(section);
            Label label = toolkit.createLabel(composite, "Select one or more items to view or edit their details.", SWT.WRAP);
            section.setClient(composite);

            GridLayout layout = new GridLayout();
            parent.setLayout(layout);

            GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
            section.setLayoutData(gd);

            layout = new GridLayout();
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            composite.setLayout(layout);

            gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
            gd.widthHint = 120;
            label.setLayoutData(gd);
        }
    }

    void createRightPanel(IManagedForm mform, final Composite parent) {
        FormToolkit toolkit = mform.getToolkit();

        BundleCalculatedImportsPart importsPart = new BundleCalculatedImportsPart(parent, toolkit, Section.TITLE_BAR | Section.EXPANDED);
        mform.addPart(importsPart);

        importPatternListPart = new ImportPatternsListPart(parent, toolkit, Section.TITLE_BAR | Section.TWISTIE);
        mform.addPart(importPatternListPart);

        GridLayout layout;
        GridData gd;

        layout = new GridLayout();
        parent.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 100;
        gd.heightHint = 200;
        importsPart.getSection().setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        importPatternListPart.getSection().setLayoutData(gd);
    }

    public void setSelectedExport(ExportedPackage export) {
        exportPatternListPart.getSelectionProvider().setSelection(new StructuredSelection(export));
    }

    public void setSelectedPrivatePkg(String pkg) {
        privPkgsPart.getSelectionProvider().setSelection(new StructuredSelection(pkg));
    }

    public void setSelectedImport(ImportPattern element) {
        importPatternListPart.getSelectionProvider().setSelection(new StructuredSelection(element));
    }

}