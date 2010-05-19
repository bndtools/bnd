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
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import bndtools.editor.GeneralInfoPart;
import bndtools.editor.PrivatePackagesPart;
import bndtools.editor.model.ExportedPackage;
import bndtools.editor.pkgpatterns.AnalyseToolbarAction;
import bndtools.editor.pkgpatterns.PkgPatternsDetailsPage;

public class BundleContentBlock extends MasterDetailsBlock {

	private ExportPatternsListPart exportsPart;
    private PrivatePackagesPart privatePkgPart;

    @Override
    public void createContent(IManagedForm managedForm, Composite parent) {
        final ScrolledForm form = managedForm.getForm();
        FormToolkit toolkit = managedForm.getToolkit();
        applyLayout(parent);
        sashForm = new SashForm(parent, SWT.HORIZONTAL);
        //toolkit.adapt(sashForm, false, false);
        sashForm.setMenu(parent.getMenu());
        applyLayoutData(sashForm);
        createMasterPart(managedForm, sashForm);

        //Composite rightPanel = toolkit.createComposite(sashForm);
        createDetailsPart(managedForm, sashForm);
//        createRightMasterPart(managedForm, rightPanel);

        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
//        rightPanel.setLayout(layout);

        createToolBarActions(managedForm);
        form.updateToolBar();
    }

    /*
     * Copied from MasterDetailsBlock
     */
    void createDetailsPart(final IManagedForm mform, Composite parent) {
        detailsPart = new DetailsPart(mform, parent, SWT.NULL);
        mform.addPart(detailsPart);
        registerPages(detailsPart);
    }

    void createRightMasterPart(final IManagedForm mform, Composite parent) {
        Text text = mform.getToolkit().createText(parent, "Foo", SWT.MULTI);

        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.heightHint = 200;
        text.setLayoutData(gd);
    }

	@Override
	protected void createMasterPart(IManagedForm managedForm, Composite parent) {
		FormToolkit toolkit = managedForm.getToolkit();

		Composite container = toolkit.createComposite(parent);

		GeneralInfoPart bundleInfoPart = new GeneralInfoPart(container, toolkit, Section.TITLE_BAR | Section.TWISTIE);
		managedForm.addPart(bundleInfoPart);

		privatePkgPart = new PrivatePackagesPart(container, toolkit, Section.TITLE_BAR | Section.EXPANDED);
		managedForm.addPart(privatePkgPart);

		exportsPart = new ExportPatternsListPart(container, toolkit, Section.TITLE_BAR | Section.EXPANDED);
		managedForm.addPart(exportsPart);

		// LAYOUT
		GridData gd;
		GridLayout layout;

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		container.setLayoutData(gd);

        layout = new GridLayout(1, false);
        container.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        bundleInfoPart.getSection().setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        exportsPart.getSection().setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        privatePkgPart.getSection().setLayoutData(gd);
	}

	@Override
	protected void createToolBarActions(final IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();

		AnalyseToolbarAction analyseAction = new AnalyseToolbarAction((IFormPage) managedForm.getContainer());
		analyseAction.setToolTipText("Analyse Imports/Exports");

		form.getToolBarManager().add(analyseAction);
	}

	@Override
	protected void registerPages(DetailsPart detailsPart) {
		final PkgPatternsDetailsPage exportDetailsPage = new PkgPatternsDetailsPage(exportsPart, "Export Pattern Details");
	    detailsPart.setPageProvider(new IDetailsPageProvider() {
            public Object getPageKey(Object object) {
                System.out.println("key = " + object);
                return object != null
                    ? object.getClass()
                    : "null";
            }
            public IDetailsPage getPage(Object key) {
                IDetailsPage result = null;
                if(key == ExportedPackage.class)
                    result = exportDetailsPage;
                return result;
            }
        });
//		detailsPart.registerPage(ExportedPackage.class, page);
	}

	void setSelectedExport(ExportedPackage export) {
		exportsPart.getSelectionProvider().setSelection(new StructuredSelection(export));
	}

    public void setSelectedPrivatePkg(String pkg) {
        privatePkgPart.getSelectionProvider().setSelection(new StructuredSelection(pkg));
    }
}
