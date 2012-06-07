package bndtools.wizards.obr;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Resource;
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
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Version;

import aQute.libg.version.VersionRange;
import bndtools.Plugin;
import bndtools.api.IBndModel;
import bndtools.api.Requirement;
import bndtools.model.obr.PotentialMatch;
import bndtools.model.obr.ReasonSorter;
import bndtools.model.obr.ResolutionFailureFlatContentProvider;
import bndtools.model.obr.ResolutionFailureFlatLabelProvider;
import bndtools.model.obr.ResolutionFailureTreeContentProvider;
import bndtools.model.obr.ResolutionFailureTreeLabelProvider;
import bndtools.model.obr.ResolutionFailureTreeSorter;
import bndtools.model.obr.SorterComparatorAdapter;
import bndtools.wizards.workspace.ReasonLabelProvider;
import bndtools.wizards.workspace.ResourceLabelProvider;

public class ObrResultsWizardPage extends WizardPage {

    public static final String PROP_RESULT = "result";

    private final Image clipboardImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/page_copy.png").createImage();
    private final Image treeViewImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/tree_mode.gif").createImage();
    private final Image flatViewImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/flat_mode.gif").createImage();
    
    private final IBndModel model;
    private final IFile file;
    private final PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);

    private final List<Resource> checkedOptional = new ArrayList<Resource>();

    private TabFolder tabFolder;

    private TabItem tbtmResults;
    private Table tblRequired;
    private Table tblOptional;
    private Table tblReasons;
    private TableViewer requiredViewer;
    private CheckboxTableViewer optionalViewer;
    private TableViewer reasonsViewer;
    private Button btnAddResolveOptional;

    private TabItem tbtmErrors;
    private TableViewer processingErrorsViewer;
    private Tree treeUnresolved;
    private TreeViewer unresolvedViewer;

    private ObrResolutionResult result;
    private boolean failureTreeMode = true;

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

        tblRequired = new Table(compResults, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL);
        GridData gd_tblRequired = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_tblRequired.heightHint = 100;
        tblRequired.setLayoutData(gd_tblRequired);

        requiredViewer = new TableViewer(tblRequired);
        requiredViewer.setContentProvider(ArrayContentProvider.getInstance());
        requiredViewer.setLabelProvider(new ResourceLabelProvider());
        requiredViewer.setSorter(new BundleSorter());

        requiredViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection sel = (IStructuredSelection) requiredViewer.getSelection();
                Resource resource = (Resource) sel.getFirstElement();
                if(resource != null){
                    List<Reason> reasons = new ArrayList<Reason>();
                    for (Reason reason : result.getReason(resource)) {
                        reasons.add(reason);
                    }
                    reasonsViewer.setInput(reasons.toArray(new Reason[reasons.size()]));
                }
                else{
                    reasonsViewer.setInput(new Reason[0]);
                }
            }
        });

        Label lblOptional = new Label(compResults, SWT.NONE);
        lblOptional.setText("Optional Resources");

        Composite compResultsOptional = new Composite(compResults, SWT.NONE);
        compResultsOptional.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        GridLayout gl_compResultsOptional = new GridLayout(2, false);
        gl_compResultsOptional.marginWidth = 0;
        gl_compResultsOptional.marginHeight = 0;
        compResultsOptional.setLayout(gl_compResultsOptional);

        tblOptional = new Table(compResultsOptional, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION | SWT.H_SCROLL);
        tblOptional.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));

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

        Label lblReason = new Label(compResults, SWT.NONE);
        lblReason.setText("Reasons");

        Composite compResultsReasons = new Composite(compResults, SWT.NONE);
        compResultsReasons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        GridLayout gl_compResultsReasons = new GridLayout(1, false);
        gl_compResultsReasons.marginWidth = 0;
        gl_compResultsReasons.marginHeight = 0;
        compResultsReasons.setLayout(gl_compResultsReasons);

        tblReasons = new Table(compResultsReasons, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL);
        tblReasons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        reasonsViewer = new TableViewer(tblReasons);
        reasonsViewer.setContentProvider(ArrayContentProvider.getInstance());
        reasonsViewer.setSorter(new ReasonSorter());
        reasonsViewer.setLabelProvider(new ReasonLabelProvider());
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

        tbtmErrors = new TabItem(tabFolder, SWT.NONE);
        tbtmErrors.setText("Errors");
        
        SashForm sashForm = new SashForm(tabFolder, SWT.VERTICAL);
        sashForm.setSashWidth(5);
        tbtmErrors.setControl(sashForm);

        Composite cmpProcessingErrors = new Composite(sashForm, SWT.NONE);
        GridLayout gl_processingErrors = new GridLayout(1, false);
        gl_processingErrors.marginRight = 7;
        cmpProcessingErrors.setLayout(gl_processingErrors);
        Label lblProcessingErrors = new Label(cmpProcessingErrors, SWT.NONE);
        lblProcessingErrors.setText("Processing Errors:");

        Table tblProcessingErrors = new Table(cmpProcessingErrors, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL);
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
        
        Composite cmpUnresolved = new Composite(sashForm, SWT.NONE);
        GridLayout gl_unresolved = new GridLayout(2, false);
        gl_unresolved.marginRight = 7;
        cmpUnresolved.setLayout(gl_unresolved);

        Label lblUnresolvedResources = new Label(cmpUnresolved, SWT.NONE);
        lblUnresolvedResources.setBounds(0, 0, 59, 14);
        lblUnresolvedResources.setText("Unresolved Requirements:");
        
        createUnresolvedViewToolBar(cmpUnresolved);

        treeUnresolved = new Tree(cmpUnresolved, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL);
        GridData gd_tblUnresolved = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        gd_tblUnresolved.heightHint = 80;
        treeUnresolved.setLayoutData(gd_tblUnresolved);

        unresolvedViewer = new TreeViewer(treeUnresolved);
        setFailureViewMode();

        /*
        unresolvedViewer.addTreeListener(new ITreeViewerListener() {
            public void treeExpanded(TreeExpansionEvent ev) {
                Object expanded = ev.getElement();
                if (expanded instanceof PotentialMatch) {
                    unresolvedViewer.expandToLevel(expanded, 2);
                }
            }
            public void treeCollapsed(TreeExpansionEvent ev) {
            }
        });
        */

        updateUi();
    }
    
    private void setFailureViewMode() {
        if (failureTreeMode) {
            unresolvedViewer.setContentProvider(new ResolutionFailureTreeContentProvider());
            unresolvedViewer.setLabelProvider(new ResolutionFailureTreeLabelProvider());
            unresolvedViewer.setSorter(new ResolutionFailureTreeSorter());
        } else {
            unresolvedViewer.setContentProvider(new ResolutionFailureFlatContentProvider());
            unresolvedViewer.setLabelProvider(new ResolutionFailureFlatLabelProvider());
            unresolvedViewer.setSorter(new ReasonSorter());
        }
    }

    private void switchFailureViewMode() {
        Object input = unresolvedViewer.getInput();
        unresolvedViewer.setInput(null);
        setFailureViewMode();
        unresolvedViewer.setInput(input);
        unresolvedViewer.expandToLevel(2);
    }

    private void createUnresolvedViewToolBar(Composite parent) {
        ToolBar unresolvedToolBar = new ToolBar(parent, SWT.FLAT | SWT.HORIZONTAL);
        GridData gd_unresolvedToolBar = new GridData(SWT.RIGHT, SWT.FILL, true, false);
        unresolvedToolBar.setLayoutData(gd_unresolvedToolBar);

        final ToolItem treeViewToggle = new ToolItem(unresolvedToolBar, SWT.RADIO);
        treeViewToggle.setImage(treeViewImg);
        treeViewToggle.setToolTipText("Tree View");
        treeViewToggle.setSelection(failureTreeMode);

        final ToolItem flatViewToggle = new ToolItem(unresolvedToolBar, SWT.RADIO);
        flatViewToggle.setImage(flatViewImg);
        flatViewToggle.setToolTipText("Flat View");
        flatViewToggle.setSelection(!failureTreeMode);

        new ToolItem(unresolvedToolBar, SWT.SEPARATOR);

        ToolItem toolErrorsToClipboard = new ToolItem(unresolvedToolBar, SWT.PUSH);
        toolErrorsToClipboard.setImage(clipboardImg);
        toolErrorsToClipboard.setToolTipText("Copy to Clipboard");

        SelectionListener modeListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean newTreeMode = treeViewToggle.getSelection();
                if (newTreeMode != failureTreeMode) {
                    failureTreeMode = newTreeMode;
                    switchFailureViewMode();
                }
            }
        };
        treeViewToggle.addSelectionListener(modeListener);
        flatViewToggle.addSelectionListener(modeListener);

        toolErrorsToClipboard.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                copyUnresolvedToClipboard();
            }
        });
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

    private static Requirement resourceToRequirement(Resource resource) {
        StringBuilder filterBuilder = new StringBuilder();

        filterBuilder.append("(&");

        ObrFilterUtil.appendBsnFilter(filterBuilder, resource.getSymbolicName());

        Version version = resource.getVersion();
        VersionRange versionRange = new VersionRange(version.toString());
        ObrFilterUtil.appendVersionFilter(filterBuilder, versionRange);

        filterBuilder.append(")");

        return new Requirement(ObrConstants.REQUIREMENT_BUNDLE, filterBuilder.toString());
    }

    private void updateUi() {
        requiredViewer.setInput(result != null ? result.getRequired() : null);
        optionalViewer.setInput(result != null ? result.getOptional() : null);
        unresolvedViewer.setInput(result != null ? result.getResolver() : null);
        processingErrorsViewer.setInput(result != null ? result.getStatus() : null);
        
        unresolvedViewer.expandToLevel(2);

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


    private void copyUnresolvedToClipboard() {
        StringBuilder builder = new StringBuilder();
        
        Object input = unresolvedViewer.getInput();
        if (input == null)
            return;
        
        ITreeContentProvider contentProvider = (ITreeContentProvider) unresolvedViewer.getContentProvider();
        Object[] roots = contentProvider.getElements(input);
        
        ViewerSorter sorter = unresolvedViewer.getSorter();
        if (sorter != null)
            Arrays.sort(roots, new SorterComparatorAdapter(unresolvedViewer, sorter));
        for (Object root : roots) {
            
            appendLabels(root, contentProvider, builder, 0);
        }
        
        /* TODO
        if (result != null) {
            StringBuilder buffer = new StringBuilder();
            List<Reason> unresolved = result.getUnresolved();

            if (unresolved != null) for (Iterator<Reason> iter = unresolved.iterator(); iter.hasNext(); ) {
                Reason reason = iter.next();
                buffer.append(unresolvedLabelProvider.getLabel(reason.getRequirement()).getString());
                buffer.append('\t');
                buffer.append(unresolvedLabelProvider.getLabel(reason.getResource()));

                if (iter.hasNext())
                    buffer.append('\n');
            }

        }
        */
        
        Clipboard clipboard = new Clipboard(getShell().getDisplay());
        TextTransfer transfer = TextTransfer.getInstance();
        clipboard.setContents(new Object[] { builder.toString() }, new Transfer[] { transfer });
        clipboard.dispose();
    }
    
    private void appendLabels(Object unresolvedTreeElem, ITreeContentProvider contentProvider, StringBuilder builder, int indent) {
        for (int i = 0; i < indent; i++)
            builder.append("..");
        
        builder.append(getClipboardContent(unresolvedTreeElem)).append('\n');

        Object[] children = contentProvider.getChildren(unresolvedTreeElem);
        if (children == null)
            return;
        ViewerSorter sorter = unresolvedViewer.getSorter();
        if (sorter != null)
            Arrays.sort(children, new SorterComparatorAdapter(unresolvedViewer, sorter));
        for (Object child : children) {
            appendLabels(child, contentProvider, builder, indent + 1);
        }
    }

    private static String getClipboardContent(Object element) {
        String label;
        if (element instanceof Reason) {
            Reason reason = (Reason) element;
            label = getClipboardContent(reason.getResource()) + "\trequires\t" + getClipboardContent(reason.getRequirement());
        } else if (element instanceof Resource) {
            Resource resource = (Resource) element;
            label = getClipboardContent(resource);
        } else if (element instanceof PotentialMatch) {
            PotentialMatch match = (PotentialMatch) element;
            String count;
            if (match.getResources().isEmpty())
                count = "UNMATCHED";
            else if (match.getResources().size() == 1)
                count = "1 potential match";
            else
                count = match.getResources().size() + " potential matches";
            label = getClipboardContent(match.getRequirement()) + "\t" + count;
        } else {
            label = "ERROR";
        }
        return label;
    }
    
    private static String getClipboardContent(Resource resource) { 
        String bsn;
        Version version;
        if (resource == null || resource.getId() == null) {
            bsn = "INITIAL";
            version = Version.emptyVersion;
        } else {
            bsn = resource.getSymbolicName();
            version = resource.getVersion();
            if (version == null) version = Version.emptyVersion;
        }
        return bsn + " " + version;
    }
    
    private static String getClipboardContent(org.apache.felix.bundlerepository.Requirement requirement) {
        return String.format("%s:%s\toptional=%s", requirement.getName(), requirement.getFilter(), requirement.isOptional());
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
        clipboardImg.dispose();
        treeViewImg.dispose();
        flatViewImg.dispose();
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
