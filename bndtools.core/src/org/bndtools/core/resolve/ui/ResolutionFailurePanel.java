package org.bndtools.core.resolve.ui;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.bndtools.core.resolve.ResolutionResult;
import org.bndtools.core.ui.resource.RequirementWithResourceLabelProvider;
import org.bndtools.core.utils.swt.SashFormPanelMaximiser;
import org.bndtools.core.utils.swt.SashHighlightForm;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.osgi.resource.Requirement;
import org.osgi.service.resolver.ResolutionException;

import bndtools.Plugin;
import bndtools.model.obr.SorterComparatorAdapter;

public class ResolutionFailurePanel {

    private final Image clipboardImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/page_copy.png").createImage();
    private final Image treeViewImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/tree_mode.gif").createImage();
    private final Image flatViewImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/flat_mode.gif").createImage();

    private SashHighlightForm sashForm;

    private Text processingErrorsText;
    private SashFormPanelMaximiser processingErrorsMaximiser;

    private TreeViewer unresolvedViewer;
    private SashFormPanelMaximiser unresolvedMaximiser;

    private static final boolean failureTreeMode = true;

    public Control createControl(final Composite parent) {
        sashForm = new SashHighlightForm(parent, SWT.VERTICAL);
        sashForm.setSashWidth(6);

        Color sashColor = JFaceResources.getColorRegistry().get(EditorsUI.PLUGIN_ID + "." + AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE_COLOR);
        sashForm.setSashBackground(sashColor);
        sashForm.setSashForeground(sashColor);

        Composite cmpProcessingErrors = new Composite(sashForm, SWT.NONE);
        GridLayout gl_processingErrors = new GridLayout(2, false);
        gl_processingErrors.marginRight = 7;
        cmpProcessingErrors.setLayout(gl_processingErrors);
        Label lblProcessingErrors = new Label(cmpProcessingErrors, SWT.NONE);
        lblProcessingErrors.setText("Processing Errors:");

        createProcessingErrorsToolBar(cmpProcessingErrors);

        processingErrorsText = new Text(cmpProcessingErrors, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.READ_ONLY);
        GridData gd_processingErrors = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        gd_processingErrors.heightHint = 80;
        processingErrorsText.setLayoutData(gd_processingErrors);

        ControlDecoration controlDecoration = new ControlDecoration(processingErrorsText, SWT.RIGHT | SWT.TOP);
        controlDecoration.setMarginWidth(2);
        controlDecoration.setDescriptionText("Double-click to view details");
        controlDecoration.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL).getImage());

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
        gd_tblUnresolved.widthHint = 400;
        treeUnresolved.setLayoutData(gd_tblUnresolved);

        unresolvedViewer = new TreeViewer(treeUnresolved);
        unresolvedViewer.setContentProvider(new UnresolvedRequirementsContentProvider());
        unresolvedViewer.setLabelProvider(new RequirementWithResourceLabelProvider());
        setFailureViewMode();

        return sashForm;
    }

    public void setInput(ResolutionResult resolutionResult) {
        if (sashForm == null)
            throw new IllegalStateException("Control not created");
        else if (sashForm.isDisposed())
            throw new IllegalStateException("Control already disposed");

        ResolutionException resolutionException = resolutionResult.getResolutionException();
        Collection<Requirement> unresolved = resolutionException != null ? resolutionException.getUnresolvedRequirements() : Collections.<Requirement> emptyList();

        unresolvedViewer.setInput(unresolved);
        processingErrorsText.setText(formatFailureStatus(resolutionResult.getStatus()));

        unresolvedViewer.expandToLevel(2);
    }

    private static String formatFailureStatus(IStatus status) {
        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);

        if (status.isMultiStatus()) {
            IStatus[] children = status.getChildren();
            for (IStatus child : children)
                pw.print(formatFailureStatus(child));
        } else {
            pw.println(status.getMessage());
            Throwable exception = status.getException();
            if (exception != null)
                exception.printStackTrace(pw);
        }
        pw.close();
        return writer.toString();
    }

    public void dispose() {
        clipboardImg.dispose();
        treeViewImg.dispose();
        flatViewImg.dispose();

        processingErrorsMaximiser.dispose();
        unresolvedMaximiser.dispose();
    }

    private void createProcessingErrorsToolBar(Composite parent) {
        ToolBar toolbar = new ToolBar(parent, SWT.FLAT | SWT.HORIZONTAL);
        toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));

        processingErrorsMaximiser = new SashFormPanelMaximiser(sashForm);
        processingErrorsMaximiser.createToolItem(parent, toolbar);
    }

    @SuppressWarnings("unused")
    private void createUnresolvedViewToolBar(final Composite parent) {
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

        unresolvedMaximiser = new SashFormPanelMaximiser(sashForm);
        unresolvedMaximiser.createToolItem(parent, unresolvedToolBar);

        /*
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
        */

        toolErrorsToClipboard.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                copyUnresolvedToClipboard();
            }
        });

    }

    private void setFailureViewMode() {
        /*
        if (failureTreeMode) {
            unresolvedViewer.setContentProvider(new ResolutionFailureTreeContentProvider());
            unresolvedViewer.setLabelProvider(new ResolutionFailureTreeLabelProvider());
            unresolvedViewer.setSorter(new ResolutionFailureTreeSorter());
        } else {
            unresolvedViewer.setContentProvider(new ResolutionFailureFlatContentProvider());
            unresolvedViewer.setLabelProvider(new ResolutionFailureFlatLabelProvider());
            unresolvedViewer.setSorter(new ReasonSorter());
        }
        */
    }

    //    private void switchFailureViewMode() {
    //        Object input = unresolvedViewer.getInput();
    //        unresolvedViewer.setInput(null);
    //        setFailureViewMode();
    //        unresolvedViewer.setInput(input);
    //        unresolvedViewer.expandToLevel(2);
    //    }

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

        /*
         * TODO if (result != null) { StringBuilder buffer = new StringBuilder(); List<Reason> unresolved =
         * result.getUnresolved(); if (unresolved != null) for (Iterator<Reason> iter = unresolved.iterator();
         * iter.hasNext(); ) { Reason reason = iter.next();
         * buffer.append(unresolvedLabelProvider.getLabel(reason.getRequirement ()).getString()); buffer.append('\t');
         * buffer.append(unresolvedLabelProvider .getLabel(reason.getResource())); if (iter.hasNext())
         * buffer.append('\n'); } }
         */

        Clipboard clipboard = new Clipboard(sashForm.getDisplay());
        TextTransfer transfer = TextTransfer.getInstance();
        clipboard.setContents(new Object[] {
            builder.toString()
        }, new Transfer[] {
            transfer
        });
        clipboard.dispose();
    }

    private void appendLabels(Object unresolvedTreeElem, ITreeContentProvider contentProvider, StringBuilder builder, int indent) {
        for (int i = 0; i < indent; i++)
            builder.append("..");

        //        builder.append(getClipboardContent(unresolvedTreeElem)).append('\n');

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

}
