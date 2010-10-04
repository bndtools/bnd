package bndtools.wizards.workspace;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.felix.bundlerepository.Resource;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class RequiredBundlesDialog extends TitleAreaDialog {

    private final Collection<Resource> required;
    private final Collection<Resource> optional;

    private CheckboxTableViewer requiredViewer;
    private CheckboxTableViewer optionalViewer;

    private Object[] selectedRequired;
    private Object[] selectedOptional;

    public RequiredBundlesDialog(Shell shell, Collection<Resource> required, Collection<Resource> optional) {
        super(shell);
        setShellStyle(SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
        this.required = required;
        this.optional = optional;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle("Additional Bundles");
        setMessage("The selected bundles depend on one or more required or optional bundles. Select which bundles to import.");

        Composite composite = (Composite) super.createDialogArea(parent);
        Composite composite2 = new Composite(composite, SWT.NONE);

        new Label(composite2, SWT.NONE).setText("Required Bundles:");
        requiredViewer = createTableAndCheckButtons(composite2);
        requiredViewer.setInput(required);
        requiredViewer.setAllChecked(true);

        new Label(composite2, SWT.NONE).setText("Optional Bundles:");
        optionalViewer = createTableAndCheckButtons(composite2);
        optionalViewer.setInput(optional);
        optionalViewer.setAllChecked(false);

        updateSelectedItems();

        // LISTENERS
        ICheckStateListener checkListener = new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                updateSelectedItems();
            }
        };
        requiredViewer.addCheckStateListener(checkListener);
        optionalViewer.addCheckStateListener(checkListener);

        // LAYOUT
        GridData gd;
        GridLayout layout;

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        composite2.setLayoutData(gd);

        layout = new GridLayout();
        composite2.setLayout(layout);

        return composite;
    }

    protected void updateSelectedItems() {
        selectedRequired = requiredViewer.getCheckedElements();
        selectedOptional = optionalViewer.getCheckedElements();
    }

    private CheckboxTableViewer createTableAndCheckButtons(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);

        Table table = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
        final CheckboxTableViewer viewer = new CheckboxTableViewer(table);
        viewer.setContentProvider(new ArrayContentProvider());
        viewer.setLabelProvider(new ResourceLabelProvider());

        Button checkAll = new Button(composite, SWT.PUSH);
        checkAll.setText("Check All");

        Button uncheckAll = new Button(composite, SWT.PUSH);
        uncheckAll.setText("Uncheck All");

        // LISTENERS
        checkAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                viewer.setAllChecked(true);
            }
        });
        uncheckAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                viewer.setAllChecked(false);
            }
        });

        // LAYOUT
        GridLayout layout;
        GridData gd;

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        composite.setLayoutData(gd);

        layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 3);
        gd.widthHint = 400;
        gd.heightHint = 100;
        table.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        checkAll.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        uncheckAll.setLayoutData(gd);

        return viewer;
    }

    public Collection<Resource> getAllSelected() {
        if (selectedRequired == null) selectedRequired = new Object[0];
        if (selectedOptional == null) selectedOptional = new Object[0];

        Collection<Resource> result = new ArrayList<Resource>(selectedRequired.length + selectedOptional.length);
        for (Object obj : selectedRequired)
            result.add((Resource) obj);
        for (Object obj : selectedOptional)
            result.add((Resource) obj);

        return result;
    }

    private static class ResourceLabelProvider extends StyledCellLabelProvider {

        private Image bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();

        @Override
        public void update(ViewerCell cell) {
            Resource resource = (Resource) cell.getElement();

            StyledString string = new StyledString(resource.getSymbolicName());
            string.append(" (" + resource.getVersion() + ")", StyledString.COUNTER_STYLER);
            string.append(" " + resource.getURI(), StyledString.DECORATIONS_STYLER);

            cell.setText(string.getString());
            cell.setStyleRanges(string.getStyleRanges());
            cell.setImage(bundleImg);
        }

        @Override
        public void dispose() {
            super.dispose();
            bundleImg.dispose();
        }
    }
}
