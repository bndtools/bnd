package bndtools.wizards.obr;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.felix.bundlerepository.DataModelHelper;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.DataModelHelperImpl;
import org.bndtools.core.obr.ObrResolutionResult;
import org.bndtools.core.obr.ResolveOperation;
import org.bndtools.core.utils.filters.ObrConstants;
import org.bndtools.core.utils.filters.ObrFilterUtil;
import org.bndtools.core.utils.jface.StatusLabelProvider;
import org.bndtools.core.utils.jface.StatusTreeContentProvider;
import org.bndtools.core.utils.swt.SWTUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
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
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.osgi.framework.Version;

import aQute.libg.version.VersionRange;
import bndtools.Plugin;
import bndtools.api.IBndModel;
import bndtools.model.obr.UnresolvedReasonLabelProvider;
import bndtools.preferences.obr.ObrPreferences;
import bndtools.wizards.workspace.ResourceLabelProvider;

public class ObrResultsWizardPage extends WizardPage {

    public static final String PROP_RESULT = "result";

    private final IBndModel model;
    private final IFile file;
    private final DataModelHelper helper = new DataModelHelperImpl();
    private final PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);

    private final List<Resource> checkedOptional = new ArrayList<Resource>();

    private TabFolder tabFolder;

    private TabItem tbtmResults;
    private Table tblRequired;
    private Table tblOptional;
    private TableViewer requiredViewer;
    private CheckboxTableViewer optionalViewer;
    private Button btnAddResolveOptional;

    private TabItem tbtmErrors;
    private TableViewer processingErrorsViewer;
    private Table tblUnresolved;
    private TableViewer unresolvedViewer;

    private ObrResolutionResult result;

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
        GridLayout gl_container = new GridLayout(1, false);
        gl_container.marginWidth = 0;
        gl_container.marginHeight = 0;
        gl_container.horizontalSpacing = 0;
        container.setLayout(gl_container);

        tabFolder = new TabFolder(container, SWT.NONE);
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

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
                if (result != null)
                    checkedOptional.addAll(result.getOptional());
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
        GridLayout gl_composite = new GridLayout(1, false);
        gl_composite.marginRight = 7;
        composite.setLayout(gl_composite);

        Label lblProcessingErrors = new Label(composite, SWT.NONE);
        lblProcessingErrors.setText("Processing Errors:");

        Table tblProcessingErrors = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
        GridData gd_tblProcessingErrors = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_tblProcessingErrors.heightHint = 80;
        tblProcessingErrors.setLayoutData(gd_tblProcessingErrors);

        processingErrorsViewer = new TableViewer(tblProcessingErrors);

        ControlDecoration controlDecoration = new ControlDecoration(tblProcessingErrors, SWT.RIGHT | SWT.TOP);
        controlDecoration.setMarginWidth(2);
        controlDecoration.setDescriptionText("Double-click to view details");
        controlDecoration.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL).getImage());
        processingErrorsViewer.setContentProvider(new StatusTreeContentProvider());
        processingErrorsViewer.setLabelProvider(new StatusLabelProvider());

        processingErrorsViewer.addOpenListener(new IOpenListener() {
            public void open(OpenEvent event) {
                IStatus status = (IStatus) ((IStructuredSelection) event.getSelection()).getFirstElement();
                ErrorDialog.openError(getShell(), "Processing Errors", null, status);
            }
        });

        Label lblUnresolvedResources = new Label(composite, SWT.NONE);
        lblUnresolvedResources.setBounds(0, 0, 59, 14);
        lblUnresolvedResources.setText("Unresolved Requirements:");

        tblUnresolved = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
        GridData gd_tblUnresolved = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_tblUnresolved.heightHint = 80;
        tblUnresolved.setLayoutData(gd_tblUnresolved);

        unresolvedViewer = new TableViewer(tblUnresolved);
        unresolvedViewer.setContentProvider(ArrayContentProvider.getInstance());
        unresolvedViewer.setLabelProvider(new UnresolvedReasonLabelProvider());

        Button btnErrorsToClipboard = new Button(composite, SWT.NONE);
        btnErrorsToClipboard.setText("Copy to Clipboard");

        Link lnkConfigRepos = new Link(container, SWT.NONE);
        lnkConfigRepos.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    Set<String> before = ObrPreferences.loadAvailableReposAndExclusions().getSecond();
                    PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getShell(), "bndtools.prefPages.obr", new String[] { "bndtools.prefPages.obr" }, null);
                    dialog.open();
                    Set<String> after = ObrPreferences.loadAvailableReposAndExclusions().getSecond();
                    if (!before.equals(after)) {
                        reresolve();
                    }
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });
        lnkConfigRepos.setText("<a>Configure OBR repositories</a>");

        updateUi();
    }

    public ObrResolutionResult getResult() {
        return result;
    }

    public void setResult(ObrResolutionResult result) {
        ObrResolutionResult oldValue = this.result;
        this.result = result;
        propertySupport.firePropertyChange(PROP_RESULT, oldValue, result);
        if (getControl() != null && !getControl().isDisposed())
            updateUi();
    }

    private void reresolve() {
        checkedOptional.clear();
        try {
            ResolveOperation resolver = new ResolveOperation(file, model);
            getContainer().run(false, true, resolver);

            setResult(resolver.getResult());
        } catch (InvocationTargetException e) {
            ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Unexpected error", e));
            setResult(null);
        } catch (InterruptedException e) {
            setResult(null);
        } finally {
            updateUi();
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
        reresolve();
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
        requiredViewer.setInput(result != null ? result.getRequired() : null);
        optionalViewer.setInput(result != null ? result.getOptional() : null);
        unresolvedViewer.setInput(result != null ? result.getUnresolved() : null);
        processingErrorsViewer.setInput(result != null ? result.getStatus() : null);

        boolean resolved = result != null && result.isResolved() && (result.getStatus() == null || result.getStatus().getSeverity() < IStatus.ERROR);

        SWTUtil.recurseEnable(resolved, tbtmResults.getControl());
        SWTUtil.recurseEnable(!resolved, tbtmErrors.getControl());
        tabFolder.setSelection(resolved ? tbtmResults : tbtmErrors);

        getContainer().updateButtons();

        if (!checkedOptional.isEmpty()) {
            btnAddResolveOptional.setEnabled(true);
            setMessage("Click 'Add and Resolve' to add the checked optional bundles to requirements and re-resolve.", IMessageProvider.INFORMATION);
        } else {
            btnAddResolveOptional.setEnabled(false);
            setMessage(null, IMessageProvider.INFORMATION);
        }
        String error = result != null && result.isResolved() ? null : "Resolution failed!";
        setErrorMessage(error);
    }

    @Override
    public boolean isPageComplete() {
        return result != null && result.isResolved() &&  checkedOptional.isEmpty() && (result.getStatus() == null || result.getStatus().getSeverity() < IStatus.ERROR);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(propertyName, listener);
    }

}
