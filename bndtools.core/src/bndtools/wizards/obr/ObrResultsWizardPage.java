package bndtools.wizards.obr;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Resource;
import org.bndtools.core.obr.ObrResolutionResult;
import org.bndtools.core.obr.ResolveOperation;
import org.bndtools.core.utils.filters.ObrConstants;
import org.bndtools.core.utils.swt.SWTUtil;
import org.bndtools.core.utils.swt.SashFormPanelMaximiser;
import org.bndtools.core.utils.swt.SashHighlightForm;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.osgi.framework.Version;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

import aQute.bnd.build.model.BndEditModel;
import aQute.lib.osgi.resource.CapReqBuilder;
import aQute.lib.osgi.resource.Filters;
import aQute.libg.filters.AndFilter;
import aQute.libg.filters.Filter;
import aQute.libg.filters.SimpleFilter;
import aQute.libg.version.VersionRange;
import bndtools.Plugin;
import bndtools.model.obr.ReasonSorter;
import bndtools.model.obr.ResolutionFailureFlatLabelProvider;
import bndtools.wizards.workspace.ResourceLabelProvider;

public class ObrResultsWizardPage extends WizardPage {

    public static final String PROP_RESULT = "result";

    private final BndEditModel model;
    private final IFile file;
    private final PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);
    private final List<Resource> checkedOptional = new ArrayList<Resource>();

    private final ResolutionFailurePanel resolutionFailurePanel = new ResolutionFailurePanel();

    private TabFolder tabFolder;
    private TabItem tbtmResults;
    private TabItem tbtmErrors;

    private SashFormPanelMaximiser requiredMaximiser;
    private TableViewer requiredViewer;
    private SashFormPanelMaximiser optionalMaximiser;
    private CheckboxTableViewer optionalViewer;
    private TableViewer reasonsViewer;
    private Button btnAddResolveOptional;

    private ObrResolutionResult result;

    /**
     * Create the wizard.
     */
    public ObrResultsWizardPage(BndEditModel model, IFile file) {
        super("resultsPage");
        this.model = model;
        this.file = file;
        setTitle("Resolution Results");
        setDescription("The required resources will be used to create the Run Bundles list. NOTE: The existing content of Run Bundles will be replaced!");
    }

    /**
     * Create contents of the wizard.
     * 
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
        tbtmResults.setControl(createResultsTabControl(tabFolder));

        tbtmErrors = new TabItem(tabFolder, SWT.NONE);
        tbtmErrors.setText("Errors");
        tbtmErrors.setControl(resolutionFailurePanel.createControl(tabFolder));

        updateUi();
    }

    private Control createResultsTabControl(Composite parent) {
        SashHighlightForm sashForm = new SashHighlightForm(parent, SWT.VERTICAL);
        sashForm.setSashWidth(6);

        ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
        Color sashColor = colorRegistry.get("org.eclipse.ui.workbench.ACTIVE_TAB_HIGHLIGHT_START");
        sashForm.setSashBackground(sashColor);
        sashForm.setSashForeground(sashColor);

        Composite cmpRequired = new Composite(sashForm, SWT.NONE);
        cmpRequired.setLayout(new GridLayout(2, false));
        Label lblRequired = new Label(cmpRequired, SWT.NONE);
        lblRequired.setText("Required Resources");

        ToolBar requiredToolbar = new ToolBar(cmpRequired, SWT.FLAT | SWT.HORIZONTAL);
        requiredToolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
        requiredMaximiser = new SashFormPanelMaximiser(sashForm);
        requiredMaximiser.createToolItem(cmpRequired, requiredToolbar);

        Table tblRequired = new Table(cmpRequired, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL);
        GridData gd_tblRequired = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        gd_tblRequired.minimumHeight = 50;
        gd_tblRequired.heightHint = 100;
        tblRequired.setLayoutData(gd_tblRequired);

        requiredViewer = new TableViewer(tblRequired);
        requiredViewer.setContentProvider(ArrayContentProvider.getInstance());
        requiredViewer.setLabelProvider(new ResourceLabelProvider());
        requiredViewer.setSorter(new BundleSorter());

        ISelectionChangedListener reasonSelectionListener = new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection sel = (IStructuredSelection) event.getSelection();
                Resource resource = (Resource) sel.getFirstElement();
                Reason[] reasons = (resource != null) ? result.getReason(resource) : new Reason[0];
                reasonsViewer.setInput(reasons);
            }
        };
        requiredViewer.addSelectionChangedListener(reasonSelectionListener);

        Composite cmpOptional = new Composite(sashForm, SWT.NONE);
        cmpOptional.setLayout(new GridLayout(2, false));

        Label lblOptional = new Label(cmpOptional, SWT.NONE);
        lblOptional.setText("Optional Resources");
        lblOptional.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        ToolBar optionalToolbar = new ToolBar(cmpOptional, SWT.FLAT | SWT.HORIZONTAL);
        optionalToolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
        optionalMaximiser = new SashFormPanelMaximiser(sashForm);
        optionalMaximiser.createToolItem(cmpOptional, optionalToolbar);

        Table tblOptional = new Table(cmpOptional, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION | SWT.H_SCROLL);
        tblOptional.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        optionalViewer = new CheckboxTableViewer(tblOptional);
        optionalViewer.setContentProvider(ArrayContentProvider.getInstance());
        optionalViewer.setLabelProvider(new ResourceLabelProvider());
        optionalViewer.setSorter(new BundleSorter());

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
        optionalViewer.addSelectionChangedListener(reasonSelectionListener);

        Composite cmpOptionalButtons = new Composite(cmpOptional, SWT.NONE);
        cmpOptionalButtons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        GridLayout gl_cmpOptionalButtons = new GridLayout(3, false);
        gl_cmpOptionalButtons.marginHeight = 0;
        gl_cmpOptionalButtons.marginWidth = 0;
        cmpOptionalButtons.setLayout(gl_cmpOptionalButtons);

        btnAddResolveOptional = new Button(cmpOptionalButtons, SWT.NONE);
        btnAddResolveOptional.setEnabled(false);
        btnAddResolveOptional.setText("Add and Resolve");
        btnAddResolveOptional.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doAddResolve();
            }
        });
        btnAddResolveOptional.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));

        Button btnAllOptional = new Button(cmpOptionalButtons, SWT.NONE);
        btnAllOptional.setText("Select All");
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
        btnAllOptional.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        Button btnClearOptional = new Button(cmpOptionalButtons, SWT.NONE);
        btnClearOptional.setText("Clear All");
        btnClearOptional.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                checkedOptional.clear();
                optionalViewer.setCheckedElements(checkedOptional.toArray());
                updateUi();
            }
        });
        btnClearOptional.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        Composite cmpReason = new Composite(sashForm, SWT.NONE);
        Label lblReason = new Label(cmpReason, SWT.NONE);
        lblReason.setText("Reasons");

        cmpReason.setLayout(new GridLayout(1, false));

        Table tblReasons = new Table(cmpReason, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL);
        tblReasons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        reasonsViewer = new TableViewer(tblReasons);
        reasonsViewer.setContentProvider(ArrayContentProvider.getInstance());
        reasonsViewer.setSorter(new ReasonSorter());
        reasonsViewer.setLabelProvider(new ResolutionFailureFlatLabelProvider());
        reasonsViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                IStructuredSelection sel = (IStructuredSelection) event.getSelection();
                Reason reason = (Reason) sel.getFirstElement();

                TableItem[] items = requiredViewer.getTable().getItems();
                for (int idx = 0; idx < items.length; idx++) {
                    Resource resource = (Resource) items[idx].getData();
                    if (resource.equals(reason.getResource())) {
                        requiredViewer.getTable().select(idx);
                        requiredViewer.getTable().showSelection();
                        requiredViewer.getTable().notifyListeners(SWT.Selection, new Event());
                        return;
                    }
                }
            }
        });

        sashForm.setWeights(new int[] {
                3, 3, 1
        });
        return sashForm;
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
        List<Requirement> oldRequires = model.getRunRequires();
        if (oldRequires == null)
            oldRequires = Collections.emptyList();

        ArrayList<Requirement> newRequires = new ArrayList<Requirement>(oldRequires.size() + checkedOptional.size());
        newRequires.addAll(oldRequires);

        for (Resource resource : checkedOptional) {
            Requirement req = resourceToRequirement(resource);
            newRequires.add(req);
        }

        model.setRunRequires(newRequires);
        reresolve();
    }

    private static Requirement resourceToRequirement(Resource resource) {

        AndFilter filter = new AndFilter();
        filter.addChild(new SimpleFilter(ObrConstants.FILTER_BSN, resource.getSymbolicName()));

        Version version = resource.getVersion();
        VersionRange versionRange = new VersionRange(version.toString());
        filter.addChild(Filters.fromVersionRange(versionRange));

        Requirement req = new CapReqBuilder(ObrConstants.REQUIREMENT_BUNDLE).addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString()).buildSyntheticRequirement();
        return req;
    }

    private void updateUi() {
        requiredViewer.setInput(result != null ? result.getRequired() : null);
        optionalViewer.setInput(result != null ? result.getOptional() : null);
        resolutionFailurePanel.setInput(result);

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
        return result != null && result.isResolved() && checkedOptional.isEmpty() && (result.getStatus() == null || result.getStatus().getSeverity() < IStatus.ERROR);
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

    @Override
    public void dispose() {
        super.dispose();

        resolutionFailurePanel.dispose();

        requiredMaximiser.dispose();
        optionalMaximiser.dispose();

    }

    private static class BundleSorter extends ViewerSorter {

        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            Resource r1 = (Resource) e1;
            Resource r2 = (Resource) e2;
            String name1 = r1.getSymbolicName();
            if (name1 == null) {
                name1 = "";
            }
            String name2 = r2.getSymbolicName();
            if (name2 == null) {
                name2 = "";
            }

            int ret = name1.compareTo(name2);
            if (ret != 0) {
                return ret;
            }

            Version ver1 = r1.getVersion();
            if (ver1 == null) {
                ver1 = Version.emptyVersion;
            }
            Version ver2 = r2.getVersion();
            if (ver2 == null) {
                ver2 = Version.emptyVersion;
            }
            return ver1.compareTo(ver2);
        }
    }

}
