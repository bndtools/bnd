package org.bndtools.core.resolve.ui;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.bndtools.core.resolve.ResolutionResult;
import org.bndtools.core.ui.resource.RequirementWithResourceLabelProvider;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.resource.Requirement;
import org.osgi.service.resolver.ResolutionException;

import bndtools.Plugin;
import bndtools.model.obr.SorterComparatorAdapter;

public class ResolutionFailurePanel {

    private final Image clipboardImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/page_copy.png").createImage();
    private final Image treeViewImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/tree_mode.gif").createImage();
    private final Image flatViewImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/flat_mode.gif").createImage();

    private Composite composite;

    private Text processingErrorsText;
    private TreeViewer unresolvedViewer;
    private Section sectProcessingErrors;
    private Section sectUnresolved;

    private static final boolean failureTreeMode = true;

    public void createControl(final Composite parent) {
        FormToolkit toolkit = new FormToolkit(parent.getDisplay());
        composite = toolkit.createComposite(parent);

        composite.setLayout(new GridLayout(1, false));
        GridData gd;

        sectProcessingErrors = toolkit.createSection(composite, Section.TITLE_BAR | Section.EXPANDED);
        sectProcessingErrors.setText("Processing Errors:");

        processingErrorsText = toolkit.createText(sectProcessingErrors, "", SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
        sectProcessingErrors.setClient(processingErrorsText);

        ControlDecoration controlDecoration = new ControlDecoration(processingErrorsText, SWT.RIGHT | SWT.TOP);
        controlDecoration.setMarginWidth(2);
        controlDecoration.setDescriptionText("Double-click to view details");
        controlDecoration.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL).getImage());

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 600;
        gd.heightHint = 300;
        sectProcessingErrors.setLayoutData(gd);

        sectUnresolved = toolkit.createSection(composite, Section.TITLE_BAR | Section.TWISTIE);
        sectUnresolved.setText("Unresolved Requirements:");

        createUnresolvedViewToolBar(sectUnresolved);

        Tree treeUnresolved = toolkit.createTree(sectUnresolved, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL);
        sectUnresolved.setClient(treeUnresolved);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.widthHint = 600;
        gd.heightHint = 300;
        sectUnresolved.setLayoutData(gd);

        unresolvedViewer = new TreeViewer(treeUnresolved);
        unresolvedViewer.setContentProvider(new UnresolvedRequirementsContentProvider());
        unresolvedViewer.setLabelProvider(new RequirementWithResourceLabelProvider());
        setFailureViewMode();
    }

    public Control getControl() {
        return composite;
    }

    //
    // TODO pkr: To Neil. I think this is where we need to change
    //

    public void setInput(ResolutionResult resolutionResult) {
        if (composite == null)
            throw new IllegalStateException("Control not created");
        else if (composite.isDisposed())
            throw new IllegalStateException("Control already disposed");

        ResolutionException resolutionException = resolutionResult.getResolutionException();
        Collection<Requirement> unresolved = resolutionException != null ? resolutionException.getUnresolvedRequirements() : Collections.<Requirement> emptyList();

        if (resolutionException != null && resolutionException.getUnresolvedRequirements() != null && !resolutionException.getUnresolvedRequirements().isEmpty()) {
            //
            // In this case I think we need to close the upper sash (right name?) with the exception
            // and only show the bottom one (the resolution result. The previous exception trace was
            // kind of silly
            //
            String diagnostic = formatFailureStatus(resolutionResult.getStatus(), false, "").replaceAll(":", ":\n  ");

            processingErrorsText.setText(diagnostic);
            sectUnresolved.setExpanded(true);
        } else {
            processingErrorsText.setText(formatFailureStatus(resolutionResult.getStatus(), true, ""));
        }

        //
        // This might be a bit more fundamental. First,
        // the URL to search on JPM can be found on {@link RequirementLabelProvider.java#requirementToUrl(Requirement)}.
        // However, we have an alternative option. The JPM Repo implements SearchableRepository which
        // has a findRequirement(Requirement,boolean) method. This would allow us to click on a requirement
        // and show a list of resources as a consequence, and allow people to add it to the repository.
        //
        unresolvedViewer.setInput(unresolved);
        unresolvedViewer.expandToLevel(2);
    }

    private static String formatFailureStatus(IStatus status, boolean exceptions, String indent) {
        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);

        if (status.isMultiStatus()) {
            IStatus[] children = status.getChildren();
            for (IStatus child : children)
                pw.print(formatFailureStatus(child, exceptions, indent + "  "));
        } else {
            pw.println(status.getMessage());
            if (exceptions) {
                Throwable exception = status.getException();
                if (exception != null)
                    exception.printStackTrace(pw);
            }
        }
        pw.close();
        return writer.toString();
    }

    public void dispose() {
        clipboardImg.dispose();
        treeViewImg.dispose();
        flatViewImg.dispose();
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

        Clipboard clipboard = new Clipboard(composite.getDisplay());
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
