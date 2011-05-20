package bndtools.wizards.workspace;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.CapabilityImpl;
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

import bndtools.Plugin;
import bndtools.api.EE;
import bndtools.bindex.GlobalCapabilityGenerator;
import bndtools.bindex.IRepositoryIndexProvider;
import bndtools.utils.Requestor;

public class DependentResourcesWizardPage extends WizardPage {

    private final RepositoryAdmin repoAdmin;
    private final List<IRepositoryIndexProvider> indexProviders = new ArrayList<IRepositoryIndexProvider>();

    private Requestor<Collection<? extends Resource>> selectedRequestor;

    private final Set<Resource> selected = new LinkedHashSet<Resource>();
    private final List<Resource> required = new ArrayList<Resource>();
    private final List<Resource> availableOptional = new ArrayList<Resource>();
    private final List<Resource> checkedOptional = new ArrayList<Resource>();

    private TableViewer requiredViewer;
    private TableViewer selectedViewer;
    private CheckboxTableViewer optionalViewer;
    private Button btnAddAndResolve;

    private boolean modifiedSelection = false;
    private Resolver resolver = null;

    private File systemBundle;

    /**
     * Create the wizard.
     */
    public DependentResourcesWizardPage(RepositoryAdmin repoAdmin, Collection<? extends IRepositoryIndexProvider> indexes) {
        super("wizardPage");
        this.repoAdmin = repoAdmin;

        setTitle("Requirements");
        setDescription("Review requirements of the selected bundles. All bundles in the \"Required\" list will be installed.");
    }

    public void addRepositoryIndexProvider(IRepositoryIndexProvider provider) {
        indexProviders.add(provider);
    }

    public void setSelectedResourcesRequestor(final Requestor<Collection<? extends Resource>> resourceRequestor) {
        selected.clear();
        selectedRequestor = resourceRequestor;
        modifiedSelection = true;
    }

    public void setSelectedResources(final Collection<? extends Resource> resources) {
        setSelectedResourcesRequestor(new Requestor<Collection<? extends Resource>>() {
            public Collection<? extends Resource> request(IProgressMonitor monitor) throws InvocationTargetException {
                return resources;
            }
        });
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

        layout = new GridLayout(2, false);
        layout.horizontalSpacing = 10;
        composite.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3);
        selectedTable.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 150;
        requiredTable.setLayoutData(gd);

        setControl(composite);
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
                    // Add indexes
                    for (IRepositoryIndexProvider provider : indexProviders) {
                        provider.initialise(progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                        repoAdmin.addRepository(provider.getUrl().toExternalForm());
                        --work;
                    }

                    // Create resolver and add selected resources
                    resolver = repoAdmin.resolver();
                    selected.addAll(selectedRequestor.request(progress.newChild(1, SubMonitor.SUPPRESS_NONE)));
                    for (Resource resource : selected) {
                        resolver.add(resource);
                    }

                    // Add global capabilities
                    if (systemBundle == null)
                        throw new IllegalStateException("System bundle not defined");
                    GlobalCapabilityGenerator capabilityGenerator = new GlobalCapabilityGenerator(systemBundle);
                    for (Capability capability : capabilityGenerator.listCapabilities(EE.J2SE_1_5)) {
                        resolver.addGlobalCapability(capability);
                    }

                    // Resolve and report missing stuff
                    boolean resolved = resolver.resolve(Resolver.NO_SYSTEM_BUNDLE | Resolver.NO_LOCAL_RESOURCES);
                    progress.worked(1);
                    Reason[] unsatisfiedRequirements = resolver.getUnsatisfiedRequirements();
                    for (Reason reason : unsatisfiedRequirements) {
                        if (reason.getRequirement().isOptional())
                            continue;
                        if (selected.contains(reason.getResource()) || required.contains(reason.getResource())) {
                            System.err.println("Unsatisfied: " + reason.getRequirement() + " required by " + reason.getResource());
                            Reason[] reasons = resolver.getReason(reason.getResource());
                            if (reasons != null) {
                                System.out.println(reason.getResource() + " was required because: ");
                                for (Reason reason2 : reasons) {
                                    System.out.println("   " + reason2.getRequirement() + " required by " + reason2.getResource());
                                }
                            }
                        } else {
                            System.out.println(reason.getRequirement() + " required by " + reason.getResource());
                        }
                    }
                    work--;


                    // Add to the required set
                    for (Resource resource : resolver.getRequiredResources()) {
                        if (resource.getURI() == null)
                            // This is the fake "resource" representing global capabilities
                            continue;

                        required.add(resource);
                        Reason[] reasons = resolver.getReason(resource);
                        if (reasons != null) {
                            System.out.println("Adding resource " + resource + " for the following reasons:");
                            for (Reason reason : reasons) {
                                System.out.println("   " + reason.getRequirement() + " REQUIRED BY " + reason.getResource());
                            }
                        }
                    }

                    // Add to the optional set
                    for (Resource resource : resolver.getOptionalResources()) {
                        boolean direct = false;
                        Reason[] reasons = resolver.getReason(resource);
                        for (Reason reason : reasons) {
                            if (selected.contains(reason.getResource()) || required.contains(reason.getResource())) {
                                direct = true;
                                break;
                            }
                        }
//                        if (direct)
                            availableOptional.add(resource);
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
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

    /*
    private boolean isInstalledResource(Resource resource) {
        String installedCategory = installedIndexProvider.getCategory();

        boolean installed = false;
        for (String category : resource.getCategories()) {
            if (category.equals(installedCategory)) {
                installed = true;
                break;
            }
        }

        return installed;
    }
    */

    void addEECapabilities(Resolver resolver) {
        String[] ees = new String[] {
                "J2SE-1.2", "J2SE-1.3", "J2SE-1.4", "J2SE-1.5", "JavaSE-1.6"
        };

        for (String ee : ees) {
            CapabilityImpl capability = new CapabilityImpl("ee");
            capability.addProperty("ee", ee);
            resolver.addGlobalCapability(capability);
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

    public void setSystemBundle(File systemBundle) {
        this.systemBundle = systemBundle;
    }

}
