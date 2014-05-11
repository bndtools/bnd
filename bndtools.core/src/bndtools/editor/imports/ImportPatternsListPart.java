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

import java.beans.PropertyChangeEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bndtools.utils.swt.SWTUtil;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.ImportPattern;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Constants;
import bndtools.editor.pkgpatterns.PkgPatternsListPart;
import bndtools.utils.ModificationLock;

public class ImportPatternsListPart extends PkgPatternsListPart<ImportPattern> {

    private class FixMissingStarsAction extends Action {
        public FixMissingStarsAction(String message) {
            super(message);
        }

        @Override
        public void run() {
            // Remove existing "*" patterns that are not in the last place
            List<ImportPattern> toRemove = new LinkedList<ImportPattern>();
            for (Iterator<ImportPattern> iter = getClauses().iterator(); iter.hasNext();) {
                ImportPattern pattern = iter.next();
                if (pattern.getName().equals("*") && iter.hasNext()) {
                    toRemove.add(pattern);
                }
            }
            if (!toRemove.isEmpty()) {
                doRemoveClauses(toRemove);
            }

            // Add a "*" at the end, if not already present
            List<ImportPattern> patterns = getClauses();
            if (patterns.size() != 0 && !patterns.get(patterns.size() - 1).getName().equals("*")) {
                ImportPattern starPattern = new ImportPattern("*", new Attrs());
                ImportPatternsListPart.super.doAddClauses(Arrays.asList(starPattern), -1, false);
            }
        }
    }

    private final ModificationLock modifyLock = new ModificationLock();
    private Text txtPattern;
    private Text txtVersion;
    private Button btnOptional;
    private Composite pnlDetails;

    public ImportPatternsListPart(Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style, Constants.IMPORT_PACKAGE, "Customise Imports", new ImportPatternLabelProvider());
        addPropertyChangeListener(PROP_SELECTION, this);
    }

    @Override
    protected void createSection(Section section, FormToolkit toolkit) {
        super.createSection(section, toolkit);

        Composite parentComposite = (Composite) getSection().getClient();
        pnlDetails = toolkit.createComposite(parentComposite);

        toolkit.createLabel(pnlDetails, "Pattern:");
        txtPattern = toolkit.createText(pnlDetails, "");

        toolkit.createLabel(pnlDetails, "Version:");
        txtVersion = toolkit.createText(pnlDetails, "", SWT.BORDER);
        btnOptional = toolkit.createButton(pnlDetails, "Optional", SWT.CHECK);

        pnlDetails.setLayout(new GridLayout(5, false));
        txtPattern.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        txtVersion.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        pnlDetails.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        SWTUtil.recurseEnable(false, pnlDetails);

        txtPattern.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (!modifyLock.isUnderModification()) {
                    List<ImportPattern> selectedClauses = getSelection();
                    if (selectedClauses.size() == 1) {
                        selectedClauses.get(0).setName(txtPattern.getText());
                        updateLabels(selectedClauses);
                        validate();
                        markDirty();
                    }
                }
            }
        });
        txtVersion.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (!modifyLock.isUnderModification()) {
                    String text = txtVersion.getText();
                    if (text.length() == 0)
                        text = null;

                    List<ImportPattern> selectedClauses = getSelection();
                    for (ImportPattern clause : selectedClauses) {
                        clause.getAttribs().put(Constants.VERSION_ATTRIBUTE, text);
                    }
                    updateLabels(selectedClauses);
                    validate();
                    markDirty();
                }
            }
        });
        btnOptional.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!modifyLock.isUnderModification()) {
                    boolean optional = btnOptional.getSelection();
                    List<ImportPattern> patterns = getSelection();
                    for (ImportPattern pattern : patterns) {
                        pattern.setOptional(optional);
                    }
                    updateLabels(patterns);
                    validate();
                    markDirty();
                }
            }
        });
    }

    @Override
    protected void doAddClauses(Collection< ? extends ImportPattern> clauses, int index, boolean select) {
        boolean appendStar = getClauses().isEmpty();

        super.doAddClauses(clauses, index, select);

        if (appendStar) {
            ImportPattern starPattern = new ImportPattern("*", new Attrs()); //$NON-NLS-1$
            super.doAddClauses(Arrays.asList(starPattern), -1, false);
        }
    }

    @Override
    public void validate() {
        IMessageManager msgs = getManagedForm().getMessageManager();
        msgs.setDecorationPosition(SWT.TOP | SWT.RIGHT);

        String noStarWarning = null;
        String actionMessage = null;
        List<ImportPattern> patterns = getClauses();
        if (!patterns.isEmpty()) {
            for (Iterator<ImportPattern> iter = patterns.iterator(); iter.hasNext();) {
                ImportPattern pattern = iter.next();
                if (pattern.getName().equals("*") && iter.hasNext()) {
                    noStarWarning = "The catch-all pattern \"*\" should be in the last position.";
                    actionMessage = "Move \"*\" pattern to the last position.";
                    break;
                }
            }

            if (noStarWarning == null) {
                ImportPattern last = patterns.get(patterns.size() - 1);
                if (!last.getName().equals("*")) {
                    noStarWarning = "The catch-all pattern \"*\" should be present and in the last position.";
                    actionMessage = "Add missing \"*\" pattern.";
                }
            }
        }
        if (noStarWarning != null) {
            msgs.addMessage("_warning_no_star", noStarWarning, new FixMissingStarsAction(actionMessage), IMessageProvider.WARNING);
        } else {
            msgs.removeMessage("_warning_no_star");
        }
    }

    @Override
    protected ImportPattern newHeaderClause(String text) {
        return new ImportPattern(text, new Attrs());
    }

    @Override
    protected List<ImportPattern> loadFromModel(BndEditModel model) {
        return model.getImportPatterns();
    }

    @Override
    protected void saveToModel(BndEditModel model, List< ? extends ImportPattern> clauses) {
        model.setImportPatterns(clauses);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (PROP_SELECTION.equals(evt.getPropertyName())) {
            final List<ImportPattern> selectedClauses = getSelection();
            modifyLock.modifyOperation(new Runnable() {
                @Override
                public void run() {
                    if (selectedClauses.isEmpty()) {
                        SWTUtil.recurseEnable(false, pnlDetails);
                        txtPattern.setText("");
                        txtVersion.setText("");
                        btnOptional.setSelection(false);
                    } else if (selectedClauses.size() == 1) {
                        SWTUtil.recurseEnable(true, pnlDetails);
                        ImportPattern pattern = selectedClauses.get(0);
                        txtPattern.setText(pattern.getName() != null ? pattern.getName() : "");
                        txtVersion.setText(pattern.getVersionRange() != null ? pattern.getVersionRange() : "");
                        btnOptional.setSelection(pattern.isOptional());
                    } else {
                        SWTUtil.recurseEnable(false, pnlDetails);
                        btnOptional.setEnabled(true);
                        pnlDetails.setEnabled(true);

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
        } else {
            super.propertyChange(evt);
        }
    }
}
