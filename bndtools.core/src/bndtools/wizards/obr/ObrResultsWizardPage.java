package bndtools.wizards.obr;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.felix.bundlerepository.DataModelHelper;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.DataModelHelperImpl;
import org.bndtools.core.utils.filters.ObrConstants;
import org.bndtools.core.utils.filters.ObrFilterUtil;
import org.bndtools.core.utils.swt.SWTUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.osgi.framework.Version;

import aQute.bnd.service.OBRIndexProvider;
import aQute.libg.version.VersionRange;
import bndtools.api.IBndModel;
import bndtools.model.obr.UnresolvedReasonLabelProvider;
import bndtools.wizards.workspace.ResourceLabelProvider;

public class ObrResultsWizardPage extends WizardPage {

    private final IBndModel model;
    private final IFile file;
    private final DataModelHelper helper = new DataModelHelperImpl();

    private boolean needsRefresh = true;
    private List<OBRIndexProvider> repositories;

    private boolean  resolved;
    private final List<Resource> checkedOptional = new ArrayList<Resource>();
    private List<Resource> resolvedRequired = Collections.emptyList();
    private List<Resource> resolvedOptional = Collections.emptyList();

    private TabFolder tabFolder;

    private TabItem tbtmResults;
    private Table tblRequired;
    private Table tblOptional;
    private TableViewer requiredViewer;
    private CheckboxTableViewer optionalViewer;
    private Button btnAddResolveOptional;

    private TabItem tbtmErrors;
    private Table tblUnresolved;
    private TableViewer unresolvedViewer;


    /**
     * Create the wizard.
     */
    public ObrResultsWizardPage(IBndModel model, IFile file) {
        super("resultsPage");
        this.model = model;
        this.file = file;
        setTitle("Resolution Results");
        setDescription("The required resources will be used to create the Run Bundles list. NOTE: The existing content of Run Bundles will be replaced!");
    }

    /**
     * Create contents of the wizard.
     * @param parent
     */
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NULL);

        setControl(container);
        container.setLayout(new FillLayout(SWT.HORIZONTAL));

        tabFolder = new TabFolder(container, SWT.NONE);

        tbtmResults = new TabItem(tabFolder, SWT.NONE);
        tbtmResults.setText("Results");

        Composite compResults = new Composite(tabFolder, SWT.NONE);
        tbtmResults.setControl(compResults);
        compResults.setLayout(new GridLayout(1, false));

        Label lblRequired = new Label(compResults, SWT.NONE);
        lblRequired.setText("Required Resources");

        tblRequired = new Table(compResults, SWT.BORDER | SWT.FULL_SELECTION);
        GridData gd_tblRequired = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_tblRequired.heightHint = 100;
        tblRequired.setLayoutData(gd_tblRequired);

        requiredViewer = new TableViewer(tblRequired);
        requiredViewer.setContentProvider(ArrayContentProvider.getInstance());
        requiredViewer.setLabelProvider(new ResourceLabelProvider());

        Label lblOptional = new Label(compResults, SWT.NONE);
        lblOptional.setText("Optional Resources");

        Composite compResultsOptional = new Composite(compResults, SWT.NONE);
        compResultsOptional.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        GridLayout gl_compResultsOptional = new GridLayout(2, false);
        gl_compResultsOptional.marginWidth = 0;
        gl_compResultsOptional.marginHeight = 0;
        compResultsOptional.setLayout(gl_compResultsOptional);

        tblOptional = new Table(compResultsOptional, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION);
        tblOptional.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));

        optionalViewer = new CheckboxTableViewer(tblOptional);
        optionalViewer.setContentProvider(ArrayContentProvider.getInstance());
        optionalViewer.setLabelProvider(new ResourceLabelProvider());

        optionalViewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                Resource resource = (Resource) event.getElement();
                if (event.getChecked()) {
                    checkedOptional.add(resource);
                } else {
                    checkedOptional.remove(resource);
                }
                updateUi();
            }
        });

        Button btnAllOptional = new Button(compResultsOptional, SWT.NONE);
        btnAllOptional.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
        btnAllOptional.setText("All");
        btnAllOptional.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                checkedOptional.clear();
                checkedOptional.addAll(resolvedOptional);
                optionalViewer.setCheckedElements(checkedOptional.toArray());
                updateUi();
            }
        });

        Button btnClearOptional = new Button(compResultsOptional, SWT.NONE);
        btnClearOptional.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
        btnClearOptional.setText("Clear");
        btnClearOptional.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                checkedOptional.clear();
                optionalViewer.setCheckedElements(checkedOptional.toArray());
                updateUi();
            }
        });

        btnAddResolveOptional = new Button(compResultsOptional, SWT.NONE);
        btnAddResolveOptional.setEnabled(false);
        btnAddResolveOptional.setText("Add and Resolve");
        btnAddResolveOptional.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doAddResolve();
            }
        });

        new Label(compResultsOptional, SWT.NONE);

        tbtmErrors = new TabItem(tabFolder, SWT.NONE);
        tbtmErrors.setText("Errors");

        Composite composite = new Composite(tabFolder, SWT.NONE);
        tbtmErrors.setControl(composite);
        composite.setLayout(new GridLayout(1, false));

        Label lblUnresolvedResources = new Label(composite, SWT.NONE);
        lblUnresolvedResources.setBounds(0, 0, 59, 14);
        lblUnresolvedResources.setText("Unresolved Requirements:");

        tblUnresolved = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
        tblUnresolved.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        unresolvedViewer = new TableViewer(tblUnresolved);
        unresolvedViewer.setContentProvider(ArrayContentProvider.getInstance());
        unresolvedViewer.setLabelProvider(new UnresolvedReasonLabelProvider());

        Button btnErrorsToClipboard = new Button(composite, SWT.NONE);
        btnErrorsToClipboard.setText("Copy to Clipboard");
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible)
            refreshResults();
    }

    public void setRepositories(List<OBRIndexProvider> repositories) {
        this.repositories = repositories;
        needsRefresh = true;
        if (getContainer() != null && getContainer().getCurrentPage() == this)
            refreshResults();
    }

    private void refreshResults() {
        if (needsRefresh) {
            checkedOptional.clear();
            try {
                ResolveOperation resolver = new ResolveOperation(file, model, repositories);
                getContainer().run(true, true, resolver);

                resolvedRequired = resolver.getRequired();
                resolvedOptional = resolver.getOptional();

                requiredViewer.setInput(resolvedRequired);
                optionalViewer.setInput(resolvedOptional);
                unresolvedViewer.setInput(resolver.getUnresolved());

                resolved = resolver.isResolved();

                SWTUtil.recurseEnable(resolved, tbtmResults.getControl());
                SWTUtil.recurseEnable(!resolved, tbtmErrors.getControl());
                tabFolder.setSelection(resolved ? tbtmResults : tbtmErrors);

            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                needsRefresh = false;
                updateUi();
            }
        }
    }

    private void doAddResolve() {
        List<Requirement> oldRequires = model.getRunRequire();
        if (oldRequires == null) oldRequires = Collections.emptyList();

        ArrayList<Requirement> newRequires = new ArrayList<Requirement>(oldRequires.size() + checkedOptional.size());
        newRequires.addAll(oldRequires);

        for (Resource resource : checkedOptional) {
            Requirement req = resourceToRequirement(resource);
            newRequires.add(req);
        }

        model.setRunRequire(newRequires);
        needsRefresh = true;
        refreshResults();
    }

    private Requirement resourceToRequirement(Resource resource) {
        StringBuilder filterBuilder = new StringBuilder();

        filterBuilder.append("(&");

        ObrFilterUtil.appendBsnFilter(filterBuilder, resource.getSymbolicName());

        Version version = resource.getVersion();
        VersionRange versionRange = new VersionRange(version.toString());
        ObrFilterUtil.appendVersionFilter(filterBuilder, versionRange);

        filterBuilder.append(")");

        return helper.requirement(ObrConstants.REQUIREMENT_BUNDLE, filterBuilder.toString());
    }

    private void updateUi() {
        getContainer().updateButtons();

        if (!checkedOptional.isEmpty()) {
            btnAddResolveOptional.setEnabled(true);
            setMessage("Click 'Add and Resolve' to add the checked optional bundles to requirements and re-resolve.", IMessageProvider.INFORMATION);
        } else {
            btnAddResolveOptional.setEnabled(false);
            setMessage(null, IMessageProvider.INFORMATION);
        }
        String error = resolved ? null : "Resolution failed!";
        setErrorMessage(error);
    }

    @Override
    public boolean isPageComplete() {
        return resolved && !needsRefresh && checkedOptional.isEmpty();
    }

    public List<Resource> getSelectedResources() {
        return resolvedRequired;
    }
}
