package bndtools.editor.exports;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import bndtools.editor.pkgpatterns.PkgPatternsDetailsPage;
import bndtools.editor.pkgpatterns.PkgPatternsListPart;
import bndtools.model.clauses.ExportedPackage;
import bndtools.utils.ModificationLock;

public class ExportPackagesDetailsPage extends PkgPatternsDetailsPage<ExportedPackage> {

    private final ModificationLock modifyLock = new ModificationLock();
    private Button btnProvide;

    public ExportPackagesDetailsPage() {
        super("Export Package Details");
    }

    @Override
    public void createContents(Composite parent) {
        super.createContents(parent);

        Composite mainComposite = getMainComposite();

        FormToolkit toolkit = getManagedForm().getToolkit();
        toolkit.createLabel(mainComposite, ""); // Spacer

        btnProvide = toolkit.createButton(mainComposite, "Provide", SWT.CHECK);
        btnProvide.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!modifyLock.isUnderModification()) {
                    boolean provide = btnProvide.getSelection();
                    for (ExportedPackage export : selectedClauses) {
                        export.setProvided(provide);
                    }
                    PkgPatternsListPart<ExportedPackage> listPart = getListPart();
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
                if (selectedClauses.isEmpty())
                    updateButton(false, false, false);
                else if (selectedClauses.size() == 1)
                    updateButton(true, false, selectedClauses.get(0).isProvided());
                else {
                    boolean differs = false;
                    boolean first = selectedClauses.get(0).isProvided();
                    for (ExportedPackage export : selectedClauses) {
                        differs = (first != export.isProvided());
                        if (differs)
                            break;
                    }
                    if (differs)
                        updateButton(true, true, false);
                    else
                        updateButton(true, false, first);
                }
            }
        });
    }

    private void updateButton(boolean enabled, boolean grayed, boolean value) {
        btnProvide.setEnabled(enabled);
        btnProvide.setGrayed(grayed);
        if (enabled && !grayed)
            btnProvide.setSelection(value);
    }
}
