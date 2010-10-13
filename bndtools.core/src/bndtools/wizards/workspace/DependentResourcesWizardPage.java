package bndtools.wizards.workspace;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

import bndtools.Plugin;
import bndtools.bindex.LocalRepositoryIndexer;

public class DependentResourcesWizardPage extends WizardPage {

    private TableViewer requiredViewer;
    private CheckboxTableViewer optionalViewer;

    private final RepositoryAdmin repoAdmin;

    private final List<Resource> selected = new ArrayList<Resource>();
    private final List<Resource> required = new ArrayList<Resource>();
    private final List<Resource> availableOptional = new ArrayList<Resource>();
    private final List<Resource> checkedOptional = new ArrayList<Resource>();

    private boolean modifiedSelection = false;

    private URL localIndexURL = null;
    private Button btnAddAndResolve;

    /**
     * Create the wizard.
     */
    public DependentResourcesWizardPage(RepositoryAdmin repoAdmin) {
        super("wizardPage");
        this.repoAdmin = repoAdmin;

        setTitle("Wizard Page title");
        setDescription("Wizard Page description");
    }


    void setSelectedResources(Collection<? extends Resource> resource) {
        selected.clear();
        selected.addAll(resource);
        modifiedSelection = true;
    }

    void addResource(Resource resource) {
        selected.add(resource);
        modifiedSelection = true;
    }

    void addResources(Collection<Resource> resource) {
        selected.addAll(resource);
        modifiedSelection = true;
    }

    /**
     * Create contents of the wizard.
     * @param parent
     */
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);

        new Label(composite, SWT.NONE).setText("Required Resources (will be installed automatically):");
        Table requiredTable = new Table(composite, SWT.HIDE_SELECTION | SWT.BORDER);
        GridData gd_requiredTable = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_requiredTable.heightHint = 100;
        requiredTable.setLayoutData(gd_requiredTable);
        requiredViewer = new TableViewer(requiredTable);
        requiredViewer.setContentProvider(new ArrayContentProvider());
        requiredViewer.setLabelProvider(new ResourceLabelProvider());

        Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);

        new Label(composite, SWT.NONE).setText("Optional Resources:");
        optionalViewer = createOptionalPanel(composite);
        optionalViewer.setAllChecked(false);

        // LISTENERS
        ICheckStateListener checkListener = new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                // TODO
            }
        };
        optionalViewer.addCheckStateListener(checkListener);

        // LAYOUT
        GridData gd;
        GridLayout layout;

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        composite.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        separator.setLayoutData(gd);

        layout = new GridLayout();
        layout.verticalSpacing = 10;
        composite.setLayout(layout);

        setControl(composite);
    }

    private void refreshBundles() {
        required.clear();
        availableOptional.clear();
        checkedOptional.clear();

        IRunnableWithProgress operation = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                int work = 4;
                SubMonitor progress = SubMonitor.convert(monitor, "", work);

                URL localUrl = getLocalRepositoryIndex("__local__", progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                progress.setWorkRemaining(--work);

                try {
                    repoAdmin.addRepository(localUrl.toExternalForm());

                    Resolver resolver = repoAdmin.resolver();
                    for (Resource resource : selected) {
                        resolver.add(resource);
                    }

                    boolean resolved = resolver.resolve(Resolver.NO_SYSTEM_BUNDLE | Resolver.NO_LOCAL_RESOURCES);
                    progress.worked(1); work--;

                    Resource[] tmp;
                    tmp = resolver.getRequiredResources();
                    for (Resource resource : tmp) {
                        // Check if the resource is tagged local
                        boolean localResource = false;
                        for (String category : resource.getCategories()) {
                            if("__local__".equals(category)) {
                                localResource = true; break;
                            }
                        }

                        // Add to the required set
                        if (!localResource) required.add(resource);
                    }

                    tmp = resolver.getOptionalResources();
                    for (Resource resource : tmp) {
                        boolean direct = false;
                        Reason[] reasons = resolver.getReason(resource);
                        for (Reason reason : reasons) {
                            if (selected.contains(reason.getResource()) || required.contains(reason.getResource())) {
                                direct = true;
                                break;
                            }
                        }
                        if (direct)
                            availableOptional.add(resource);
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
                    repoAdmin.removeRepository(localUrl.toExternalForm());
                }
            }
        };

        if (modifiedSelection) {
            try {
                getContainer().run(true, true, operation);
                requiredViewer.setInput(required);
                optionalViewer.setInput(availableOptional);
                modifiedSelection = false;
            } catch (InvocationTargetException e) {
                ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error in resolver", e.getTargetException()));
            } catch (InterruptedException e) {
                // do nothing, just don't set the modified flag to true
            }
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        refreshBundles();
    }

    @Override
    public boolean isPageComplete() {
        return !modifiedSelection && checkedOptional.isEmpty();
    }

    private synchronized URL getLocalRepositoryIndex(String localCategory, IProgressMonitor monitor) {
        if (localIndexURL == null) {
            LocalRepositoryIndexer indexer = new LocalRepositoryIndexer();
            indexer.setLocalCategory(localCategory);
            indexer.run(monitor);

            localIndexURL = indexer.getUrl();
        }

        return localIndexURL;
    }

    private CheckboxTableViewer createOptionalPanel(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);

        Table table = new Table(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
        final CheckboxTableViewer viewer = new CheckboxTableViewer(table);
        viewer.setContentProvider(new ArrayContentProvider());
        viewer.setLabelProvider(new ResourceLabelProvider());

        Button checkAll = new Button(container, SWT.PUSH);
        checkAll.setText("Check All");

        Button uncheckAll = new Button(container, SWT.PUSH);
        uncheckAll.setText("Uncheck All");

        Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

        btnAddAndResolve = new Button(container, SWT.NONE);
        btnAddAndResolve.setText("Add and Resolve");
        btnAddAndResolve.setEnabled(false);


        // LISTENERS
        checkAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                checkedOptional.clear();
                checkedOptional.addAll(availableOptional);
                optionalViewer.setCheckedElements(checkedOptional.toArray());
                updateButtons();
            }
        });
        uncheckAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                checkedOptional.clear();
                optionalViewer.setCheckedElements(checkedOptional.toArray());
                updateButtons();
            }
        });
        viewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                Resource resource = (Resource) event.getElement();
                if (event.getChecked()) {
                    checkedOptional.add(resource);
                } else {
                    checkedOptional.remove(resource);
                }
                updateButtons();
            }
        });
        btnAddAndResolve.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addResources(checkedOptional);
                refreshBundles();
                updateButtons();
            }
        });

        // LAYOUT
        GridLayout layout;
        GridData gd;

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        container.setLayoutData(gd);

        layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        container.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 4);
        gd.heightHint = 100;
        gd.widthHint = 400;
        table.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        checkAll.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        uncheckAll.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.BOTTOM, false, false, 1, 1);
        btnAddAndResolve.setLayoutData(gd);

        return viewer;
    }

    private void updateButtons() {
        getContainer().updateButtons();
        btnAddAndResolve.setEnabled(!checkedOptional.isEmpty());
    }

    public List<Resource> getSelected() {
        return selected;
    }


    public List<Resource> getRequired() {
        return required;
    }

}
