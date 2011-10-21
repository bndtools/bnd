package bndtools.preferences.ui.obr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import aQute.bnd.service.OBRIndexProvider;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.Plugin;
import bndtools.preferences.obr.ObrPreferences;
import bndtools.types.Pair;

public class ObrPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private final List<OBRIndexProvider> availableRepos = new ArrayList<OBRIndexProvider>();
    private final List<OBRIndexProvider> selectedRepos = new ArrayList<OBRIndexProvider>();

    private Table table;
    private CheckboxTableViewer viewer;

    /**
     * Create the preference page.
     */
    public ObrPreferencePage() {
        setTitle("Excluded OBR Repositories");
        setDescription("The checked repositories will NOT BE USED during OBR resolution");
    }

    /**
     * Create contents of the preference page.
     * @param parent
     */
    @Override
    public Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);
        container.setLayout(new GridLayout(2, false));

        table = new Table(container, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));
        table.setBounds(0, 0, 18, 34);

        viewer = new CheckboxTableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new ObrIndexProviderLabelProvider());

        Button btnAll = new Button(container, SWT.NONE);
        btnAll.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            }
        });
        btnAll.setText("All");

        Button btnClear = new Button(container, SWT.NONE);
        btnClear.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
        btnClear.setText("Clear");

        viewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                List<OBRIndexProvider> old = new ArrayList<OBRIndexProvider>(selectedRepos);
                if (event.getChecked()) {
                    selectedRepos.add((OBRIndexProvider) event.getElement());
                } else {
                    selectedRepos.remove(event.getElement());
                }
                updateUi();
            }
        });
        btnAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                List<OBRIndexProvider> old = new ArrayList<OBRIndexProvider>(selectedRepos);
                selectedRepos.clear();
                selectedRepos.addAll(availableRepos);
                viewer.setCheckedElements(selectedRepos.toArray());
                updateUi();
            }
        });
        btnClear.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                List<OBRIndexProvider> old = new ArrayList<OBRIndexProvider>(selectedRepos);
                selectedRepos.clear();
                viewer.setCheckedElements(selectedRepos.toArray());
                updateUi();
            }
        });

        load();

        return container;
    }

    void updateUi() {
        updateApplyButton();
    }

    void load() {
        try {
            Pair<List<OBRIndexProvider>, Set<String>> prefs = ObrPreferences.loadAvailableReposAndExclusions();

            availableRepos.clear();
            selectedRepos.clear();
            for (OBRIndexProvider repo : prefs.getFirst()) {
                availableRepos.add(repo);
                String name = getName(repo);
                if (prefs.getSecond().contains(name))
                    selectedRepos.add(repo);
            }
            viewer.setInput(availableRepos);
            viewer.setCheckedElements(selectedRepos.toArray());
            updateUi();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public boolean performOk() {
        Set<String> set = new HashSet<String>();

        for (OBRIndexProvider repo : selectedRepos) {
            set.add(getName(repo));
        }

        try {
            ObrPreferences.saveExclusions(set);
            return true;
        } catch (IOException e) {
            ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Failed to save.", e));
            return false;
        }
    }

    String getName(OBRIndexProvider repo) {
        String name = (repo instanceof RepositoryPlugin) ? ((RepositoryPlugin) repo).getName() : repo.toString();
        return name;
    }

    /**
     * Initialize the preference page.
     */
    public void init(IWorkbench workbench) {
    }
}
