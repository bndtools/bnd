package bndtools.wizards.workspace;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
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

import aQute.bnd.build.Project;
import bndtools.Plugin;
import bndtools.bindex.AbstractIndexer;
import bndtools.bindex.IRepositoryIndexProvider;
import bndtools.model.clauses.VersionedClause;
import bndtools.utils.Requestor;

public class DependentResourcesWizardPage extends WizardPage {

    private final RepositoryAdmin repoAdmin;
    private final AbstractIndexer installedIndexer;
    private final List<IRepositoryIndexProvider> indexProviders = new ArrayList<IRepositoryIndexProvider>();

    private Requestor<Collection<? extends Resource>> selectedRequestor;

    private final Set<Resource> selected = new HashSet<Resource>();
    private final List<Resource> required = new ArrayList<Resource>();
    private final List<Resource> availableOptional = new ArrayList<Resource>();
    private final List<Resource> checkedOptional = new ArrayList<Resource>();

    private TableViewer requiredViewer;
    private TableViewer selectedViewer;
    private CheckboxTableViewer optionalViewer;
    private Button btnAddAndResolve;

    private boolean modifiedSelection = false;
    private Resolver resolver = null;

    /**
     * Create the wizard.
     */
    public DependentResourcesWizardPage(RepositoryAdmin repoAdmin, AbstractIndexer installedIndexer) {
        super("wizardPage");
        this.repoAdmin = repoAdmin;
        this.installedIndexer = installedIndexer;

        setTitle("Requirements");
        setDescription("Review requirements of the selected bundles. All bundles in the \"Required\" list will be installed.");
    }

    public void addRepositoryIndexProvider(IRepositoryIndexProvider provider) {
        indexProviders.add(provider);
    }

    public void setSelectedBundles(final Project project, final Collection<? extends VersionedClause> bundles) {
        selectedRequestor = new BundleResourceRequestor(repoAdmin, installedIndexer, bundles, project);
        modifiedSelection = true;
    }

    void setSelectedResources(final Collection<? extends Resource> resources) {
        selectedRequestor = new Requestor<Collection<? extends Resource>>() {
            public Collection<? extends Resource> request(IProgressMonitor monitor) throws InvocationTargetException {
                return resources;
            }
        };
        modifiedSelection = true;
    }

    /**
     * Create contents of the wizard.
     *
     * @param parent
     */
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);

        new Label(composite, SWT.NONE).setText("Selected Resources:");
        new Label(composite, SWT.NONE).setText("Required:");
        Table selectedTable = new Table(composite, SWT.BORDER);
        Table requiredTable = new Table(composite, SWT.BORDER);

        selectedViewer = new TableViewer(selectedTable);
        selectedViewer.setContentProvider(new ArrayContentProvider());
        selectedViewer.setLabelProvider(new ResourceLabelProvider());

        requiredViewer = new TableViewer(requiredTable);
        requiredViewer.setContentProvider(new ArrayContentProvider());
        requiredViewer.setLabelProvider(new ResourceLabelProvider());

        new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        new Label(composite, SWT.NONE).setText("Optional:");
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

        layout = new GridLayout(2, true);
        layout.horizontalSpacing = 10;
        composite.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 4);
        selectedTable.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 100;
        requiredTable.setLayoutData(gd);

        setControl(composite);
        new Label(composite, SWT.NONE);
        new Label(composite, SWT.NONE);
    }

    void addResources(Collection<? extends Resource> adding) {
        selected.addAll(adding);
        selectedViewer.add(adding.toArray());
        modifiedSelection = true;
    }

    void refreshBundles() {
        required.clear();
        availableOptional.clear();
        checkedOptional.clear();

        IRunnableWithProgress operation = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                int work = 4 + indexProviders.size();
                SubMonitor progress = SubMonitor.convert(monitor, "", work);

                try {
                    installedIndexer.initialise(progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                    --work;

                    repoAdmin.addRepository(installedIndexer.getUrl().toExternalForm());

                    for (IRepositoryIndexProvider provider : indexProviders) {
                        provider.initialise(progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                        repoAdmin.addRepository(provider.getUrl().toExternalForm());
                        --work;
                    }

                    resolver = repoAdmin.resolver();
                    Collection<? extends Resource> newSelected = selectedRequestor.request(progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                    selected.addAll(newSelected);
                    for (Resource resource : selected) {
                        resolver.add(resource);
                    }

                    boolean resolved = resolver.resolve(Resolver.NO_SYSTEM_BUNDLE | Resolver.NO_LOCAL_RESOURCES);
                    progress.worked(1);
                    work--;

                    Resource[] tmp;
                    tmp = resolver.getRequiredResources();

                    // Add to the required set
                    for (Resource resource : tmp) {
                        if (!isInstalledResource(resource))
                            required.add(resource);
                    }

                    // Add to the optional set
                    tmp = resolver.getOptionalResources();
                    for (Resource resource : tmp) {
                        if (isInstalledResource(resource))
                            continue;

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
                    repoAdmin.removeRepository(installedIndexer.getUrl().toExternalForm());
                    for (IRepositoryIndexProvider provider : indexProviders) {
                        repoAdmin.removeRepository(provider.getUrl().toExternalForm());
                    }
                }
            }

        };

        if (modifiedSelection) {
            try {
                getContainer().run(true, true, operation);
                selectedViewer.setInput(selected);
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

    private boolean isInstalledResource(Resource resource) {
        String installedCategory = installedIndexer.getCategory();

        boolean installed = false;
        for (String category : resource.getCategories()) {
            if (category.equals(installedCategory)) {
                installed = true;
                break;
            }
        }

        return installed;
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

    private CheckboxTableViewer createOptionalPanel(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);

        Table table = new Table(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
        final CheckboxTableViewer viewer = new CheckboxTableViewer(table);
        viewer.setContentProvider(new ArrayContentProvider());
        viewer.setLabelProvider(new ResourceLabelProvider());

        Button checkAll = new Button(container, SWT.PUSH);
        checkAll.setText("All");

        Button uncheckAll = new Button(container, SWT.PUSH);
        uncheckAll.setText("Clear");

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
                updateButtonAndMessage();
            }
        });
        uncheckAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                checkedOptional.clear();
                optionalViewer.setCheckedElements(checkedOptional.toArray());
                updateButtonAndMessage();
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
                updateButtonAndMessage();
            }
        });
        btnAddAndResolve.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addAndResolveSelectedOptionalResources();
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

        gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2);
        gd.heightHint = 100;
        table.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.BOTTOM, false, false);
        checkAll.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.TOP, false, false);
        uncheckAll.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        btnAddAndResolve.setLayoutData(gd);
        new Label(container, SWT.NONE);

        return viewer;
    }

    void addAndResolveSelectedOptionalResources() {
        addResources(checkedOptional);

        refreshBundles();

        updateButtonAndMessage();
    }

    private void updateButtonAndMessage() {
        getContainer().updateButtons();

        if (!checkedOptional.isEmpty()) {
            btnAddAndResolve.setEnabled(true);
            setMessage("Click 'Add and Resolve' to add the selected optional bundles and re-calculate dependencies.", IMessageProvider.INFORMATION);
        } else {
            btnAddAndResolve.setEnabled(false);
            setMessage(null, IMessageProvider.INFORMATION);
        }
    }

    public Collection<Resource> getSelected() {
        return selected;
    }

    public List<Resource> getRequired() {
        return required;
    }

    public Resolver getResolver() {
        return resolver;
    }

}
