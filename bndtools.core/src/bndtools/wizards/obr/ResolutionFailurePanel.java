package bndtools.wizards.obr;

import java.util.Arrays;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Resource;
import org.bndtools.core.obr.ObrResolutionResult;
import org.bndtools.core.utils.jface.StatusLabelProvider;
import org.bndtools.core.utils.jface.StatusTreeContentProvider;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Version;

import bndtools.Plugin;
import bndtools.model.obr.PotentialMatch;
import bndtools.model.obr.ReasonSorter;
import bndtools.model.obr.ResolutionFailureFlatContentProvider;
import bndtools.model.obr.ResolutionFailureFlatLabelProvider;
import bndtools.model.obr.ResolutionFailureTreeContentProvider;
import bndtools.model.obr.ResolutionFailureTreeLabelProvider;
import bndtools.model.obr.ResolutionFailureTreeSorter;
import bndtools.model.obr.SorterComparatorAdapter;

public class ResolutionFailurePanel {

    private final Image clipboardImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/page_copy.png").createImage();
    private final Image treeViewImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/tree_mode.gif").createImage();
    private final Image flatViewImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/flat_mode.gif").createImage();
    
    private SashForm sashForm;
    private TableViewer processingErrorsViewer;
    private TreeViewer unresolvedViewer;

    private boolean failureTreeMode = true;

    public Control createControl(final Composite parent) {
        sashForm = new SashForm(parent, SWT.VERTICAL);
        sashForm.setSashWidth(5);

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
                ErrorDialog.openError(parent.getShell(), "Processing Errors", null, status);
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

        Tree treeUnresolved = new Tree(cmpUnresolved, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL);
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
        return sashForm;
    }
    
    public void setInput(ObrResolutionResult resolutionResult) {
        if (sashForm == null)
            throw new IllegalStateException("Control not created");
        else if (sashForm.isDisposed())
            throw new IllegalStateException("Control already disposed");

        unresolvedViewer.setInput(resolutionResult != null ? resolutionResult.getResolver() : null);
        processingErrorsViewer.setInput(resolutionResult != null ? resolutionResult.getStatus() : null);
        
        unresolvedViewer.expandToLevel(2);
    }

    public void dispose() {
        clipboardImg.dispose();
        treeViewImg.dispose();
        flatViewImg.dispose();
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
        
        Clipboard clipboard = new Clipboard(sashForm.getDisplay());
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

}
