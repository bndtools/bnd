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
package bndtools.editor.imports;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import bndtools.editor.pkgpatterns.PkgPatternsDetailsPage;
import bndtools.editor.pkgpatterns.PkgPatternsListPart;
import bndtools.model.clauses.ImportPattern;
import bndtools.utils.ModificationLock;

public class ImportPatternsDetailsPage extends PkgPatternsDetailsPage<ImportPattern> {

    private final ModificationLock modifyLock = new ModificationLock();

    private Button btnOptional;

    public ImportPatternsDetailsPage() {
        super("Import Pattern Details");
    }

    @Override
    public void createContents(Composite parent) {
        super.createContents(parent);

        Composite mainComposite = getMainComposite();

        FormToolkit toolkit = getManagedForm().getToolkit();
        toolkit.createLabel(mainComposite, ""); // Spacer
        btnOptional = toolkit.createButton(mainComposite, "Optional", SWT.CHECK);

        btnOptional.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!modifyLock.isUnderModification()) {
                    boolean optional = btnOptional.getSelection();
                    for (ImportPattern pattern : selectedClauses) {
                        pattern.setOptional(optional);
                    }
                    PkgPatternsListPart<ImportPattern> listPart = getListPart();
                    if (listPart != null) {
                        listPart.updateLabels(selectedClauses);
                        listPart.validate();
                    }
                    markDirty();
                }
            }
        });
    }

    @Override
    public void refresh() {
        super.refresh();
        modifyLock.modifyOperation(new Runnable() {
            public void run() {
                if (selectedClauses.isEmpty()) {
                    btnOptional.setEnabled(false);
                    btnOptional.setGrayed(false);
                } else if (selectedClauses.size() == 1) {
                    btnOptional.setEnabled(true);
                    btnOptional.setGrayed(false);
                    btnOptional.setSelection(selectedClauses.get(0).isOptional());
                } else {
                    btnOptional.setEnabled(true);

                    boolean differs = false;
                    boolean first = selectedClauses.get(0).isOptional();
                    for (ImportPattern pattern : selectedClauses) {
                        if (first != pattern.isOptional()) {
                            differs = true;
                            break;
                        }
                    }
                    if (differs) {
                        btnOptional.setGrayed(true);
                    } else {
                        btnOptional.setGrayed(false);
                        btnOptional.setSelection(first);
                    }
                }
            }
        });
    }
}
