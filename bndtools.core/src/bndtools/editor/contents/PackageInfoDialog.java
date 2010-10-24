package bndtools.editor.contents;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class PackageInfoDialog extends TitleAreaDialog {

    private final Map<String, File> packages;
    private final Collection<String> selection;

    private boolean dontAsk = false;

    private Table table;
    private CheckboxTableViewer viewer;

    /**
     * Create the dialog.
     *
     * @param parentShell
     * @param packages
     */
    public PackageInfoDialog(Shell parentShell, Map<String, File> packages) {
        super(parentShell);
        setShellStyle(SWT.BORDER | SWT.CLOSE | SWT.RESIZE);
        this.packages = packages;

        selection = new ArrayList<String>(packages.size());
        selection.addAll(packages.keySet());
    }

    /**
     * Create contents of the dialog.
     *
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage(Messages.PackageInfoDialog_Message);
        setTitle(Messages.PackageInfoDialog_Title);
        Composite container = (Composite) super.createDialogArea(parent);

        Composite composite = new Composite(container, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 10;
        layout.marginWidth = 10;
        composite.setLayout(layout);

        table = new Table(composite, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2);
        gd_table.heightHint = 100;
        table.setLayoutData(gd_table);
        table.setLinesVisible(true);

        TableColumn tblclmnPackage = new TableColumn(table, SWT.NONE);
        tblclmnPackage.setWidth(267);
        tblclmnPackage.setText(Messages.PackageInfoDialog_ExportedPackage);

        TableColumn tblclmnVersion = new TableColumn(table, SWT.NONE);
        tblclmnVersion.setWidth(77);
        tblclmnVersion.setText(Messages.PackageInfoDialog_Version);

        Button btnCheckAll = new Button(composite, SWT.NONE);
        btnCheckAll.setText(Messages.PackageInfoDialog_btnCheckAll_text);
        btnCheckAll.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
        btnCheckAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selection.clear();
                selection.addAll(packages.keySet());
                viewer.setCheckedElements(selection.toArray());
                validate();
            }
        });

        Button btnUncheckAll = new Button(composite, SWT.NONE);
        btnUncheckAll.setText(Messages.PackageInfoDialog_btnUncheckAll_text_1);
        btnUncheckAll.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
        btnUncheckAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selection.clear();
                viewer.setCheckedElements(selection.toArray());
                validate();
            }
        });

        final Button btnAlwaysGenerate = new Button(composite, SWT.CHECK);
        btnAlwaysGenerate.setText(Messages.PackageInfoDialog_AlwaysGenerate);

        viewer = new CheckboxTableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new PackageInfoLabelProvider(table.getDisplay()));
        viewer.setSorter(new ViewerSorter() {
            @Override
            public int compare(Viewer viewer, Object e1, Object e2) {
                String s1 = (String) e1;
                String s2 = (String) e2;
                return s1.compareTo(s2);
            }
        });
        viewer.setInput(packages.keySet());
        viewer.setCheckedElements(selection.toArray());
        new Label(composite, SWT.NONE);

        btnAlwaysGenerate.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dontAsk = btnAlwaysGenerate.getSelection();
            }
        });
        viewer.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                String pkgName = (String) event.getElement();

                if (event.getChecked())
                    selection.add(pkgName);
                else
                    selection.remove(pkgName);
                validate();
            }
        });

        return container;
    }

    private void validate() {
        String warning = null;

        if (selection.size() < packages.size())
            warning = Messages.PackageInfoDialog_Warning;

        if (warning != null)
            setMessage(warning, IMessageProvider.WARNING);
        else
            setMessage(Messages.PackageInfoDialog_Message);
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        return new Point(500, 300);
    }

    public boolean isDontAsk() {
        return dontAsk;
    }

    public Collection<File> getSelectedPackageDirs() {
        Collection<File> result = new ArrayList<File>(selection.size());

        for (String pkgName : selection) {
            File file = packages.get(pkgName);
            if (file != null)
                result.add(file);
        }

        return result;
    }

    private static class PackageInfoLabelProvider extends StyledCellLabelProvider {

        private final Image image;

        public PackageInfoLabelProvider(Device device) {
            image = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_obj.gif").createImage(device); //$NON-NLS-1$
        }

        @Override
        public void update(ViewerCell cell) {
            String pkgName = (String) cell.getElement();

            if (cell.getColumnIndex() == 0) {
                cell.setImage(image);
                cell.setText(pkgName);
            } else if (cell.getColumnIndex() == 1) {
                StyledString label = new StyledString("1.0", StyledString.COUNTER_STYLER); //$NON-NLS-1$
                cell.setText(label.getString());
                cell.setStyleRanges(label.getStyleRanges());
            }
        }

        @Override
        public void dispose() {
            super.dispose();
            image.dispose();
        }
    }

}
