package bndtools.wizards.workspace;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.PreferencesUtil;

import bndtools.Plugin;
import bndtools.preferences.OBRPreferences;
import bndtools.preferences.ui.OBRPreferencePage;
import bndtools.shared.OBRLink;
import bndtools.shared.URLLabelProvider;

public class OBRSelectionPage extends WizardPage {

    private final RepositoryAdmin repoAdmin;
    private final IRepositoriesChangedCallback callback;

    private final Collection<OBRLink> availableLinks = new LinkedHashSet<OBRLink>();
    private final Collection<OBRLink> selectedLinks = new ArrayList<OBRLink>();

    private Table table;
    private CheckboxTableViewer viewer;

    /**
     * Create the wizard.
     */
    public OBRSelectionPage(RepositoryAdmin repoAdmin, IRepositoriesChangedCallback callback) {
        super("wizardPage");
        this.repoAdmin = repoAdmin;
        this.callback = callback;

        setTitle("Repository Selection");
        setDescription("Select the set of repositories to search. This set will also be used to resolve dependencies of the selected bundles.");
    }

    /**
     * Create contents of the wizard.
     * @param parent
     */
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);
        container.setLayout(new GridLayout(2, false));

        Label lblRepositoryUrls = new Label(container, SWT.NONE);
        lblRepositoryUrls.setText("Known Repositories:");
        new Label(container, SWT.NONE);

        table = new Table(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
        viewer = new CheckboxTableViewer(table);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));

        Button btnCheckAll = new Button(container, SWT.NONE);
        btnCheckAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedLinks.clear();
                selectedLinks.addAll(availableLinks);
                viewer.setCheckedElements(selectedLinks.toArray());
                table.update();
                updateNextButton();
            }
        });
        btnCheckAll.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
        btnCheckAll.setText("Check All");

        Button btnUncheckAll = new Button(container, SWT.NONE);
        btnUncheckAll.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
        btnUncheckAll.setText("Uncheck All");
        btnUncheckAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedLinks.clear();
                viewer.setCheckedElements(selectedLinks.toArray());
                table.update();
                updateNextButton();
            }
        });

        Link linkConfigureRepos = new Link(container, SWT.NONE);
        linkConfigureRepos.setText("<a>Configure Repositories</a>");
        new Label(container, SWT.NONE);

        // LISTENERS
        linkConfigureRepos.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                PreferenceDialog prefsDlg = PreferencesUtil.createPreferenceDialogOn(getShell(), OBRPreferencePage.PAGE_ID, new String[] { OBRPreferencePage.PAGE_ID }, null);
                prefsDlg.open();
                loadData();
                viewer.refresh();
            }
        });

        viewer.setContentProvider(new ArrayContentProvider());
        viewer.setLabelProvider(new URLLabelProvider(parent.getDisplay()));
        viewer.setInput(availableLinks);

        loadData();
        updateNextButton();

        viewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                OBRLink link = (OBRLink) event.getElement();
                if (event.getChecked()) {
                    selectedLinks.add(link);
                } else {
                    selectedLinks.remove(link);
                }
                updateNextButton();
            }
        });

        // Validate before switching pages
        final WizardDialog dialog = (WizardDialog) getContainer();
        final OBRSelectionPage self = this;
        dialog.addPageChangingListener(new IPageChangingListener() {
            public void handlePageChanging(PageChangingEvent event) {
                if (event.getCurrentPage() == self && event.getTargetPage() != self) {
                    event.doit = doSetRepositories();
                }
            }
        });
    }

    boolean doSetRepositories() {
        final MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Error configuring repositories.", null);
        IRunnableWithProgress operation = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                // Remove all existing
                Repository[] repos = repoAdmin.listRepositories();
                for (Repository repo : repos) {
                    repoAdmin.removeRepository(repo.getURI());
                }

                // Add repos
                for (OBRLink link : selectedLinks) {
                    if(availableLinks.contains(link)) {
                        try {
                            repoAdmin.addRepository(link.getLink());
                        } catch (Exception e) {
                            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error adding repository URL: " + link.getLink(), e));
                        }
                    }
                }
            }
        };
        try {
            getContainer().run(true, false, operation);
        } catch (InvocationTargetException e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Unexpected error", e.getTargetException()));
        } catch (InterruptedException e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Interrupted", null));
        }

        if (callback != null)
            callback.changedRepositories(repoAdmin);

        if (!status.isOK()) {
            ErrorDialog.openError(getShell(), "Error", null, status);
            return false;
        }

        return true;
    }

    private void loadData() {
        IPreferenceStore prefs = Plugin.getDefault().getPreferenceStore();

        availableLinks.clear();
        OBRPreferences.loadConfiguredRepositories(availableLinks, prefs);
        if (!OBRPreferences.isHideBuiltIn(prefs)) {
            OBRPreferences.loadBuiltInRepositories(availableLinks);
        }

        viewer.refresh();
    }

    private void updateNextButton() {
        setPageComplete(!selectedLinks.isEmpty());
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            table.setFocus();
        }
    }

}
