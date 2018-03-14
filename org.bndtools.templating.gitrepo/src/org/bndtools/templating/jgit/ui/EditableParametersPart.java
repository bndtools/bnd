package org.bndtools.templating.jgit.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bndtools.templating.jgit.GitRepoPreferences;
import org.bndtools.utils.jface.BoldStyler;
import org.bndtools.utils.swt.AddRemoveButtonBarPart;
import org.bndtools.utils.swt.AddRemoveButtonBarPart.AddRemoveListener;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.libg.tuple.Pair;

public class EditableParametersPart {

    private static final Bundle BUNDLE = FrameworkUtil.getBundle(EditableParametersPart.class);

    private final String title;
    private final NewEntryDialogFactory dialogFactory;

    private List<Pair<String, Attrs>> entries;

    private Composite parent;
    private TableViewer viewer;
    private final ImageDescriptor imageDescriptor;

    public EditableParametersPart(String title, ImageDescriptor imageDescriptor, NewEntryDialogFactory dialogFactory) {
        this.title = title;
        this.imageDescriptor = imageDescriptor;
        this.dialogFactory = dialogFactory;
    }

    public void setParameters(Parameters params) {
        entries = new ArrayList<>(params.size());
        for (Entry<String, Attrs> entry : params.entrySet()) {
            entries.add(new Pair<>(GitRepoPreferences.removeDuplicateMarker(entry.getKey()), entry.getValue()));
        }
    }

    public Parameters getParameters() {
        Parameters params = new Parameters();
        for (Pair<String, Attrs> entry : entries) {
            params.add(entry.getFirst(), entry.getSecond());
        }
        return params;
    }

    public Control createControl(Composite parent) {
        this.parent = parent;
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        composite.setLayout(layout);

        Label titleLabel = new Label(composite, SWT.NONE);
        titleLabel.setText(title);
        titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));

        Table table = new Table(composite, SWT.BORDER | SWT.MULTI);
        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        final Image iconImg = imageDescriptor.createImage(parent.getDisplay());
        viewer.setLabelProvider(new StyledCellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                @SuppressWarnings("unchecked")
                Pair<String, Attrs> entry = (Pair<String, Attrs>) cell.getElement();
                StyledString label = new StyledString(entry.getFirst(), BoldStyler.INSTANCE_DEFAULT);
                for (Entry<String, String> attribEntry : entry.getSecond()
                    .entrySet()) {
                    label.append("; " + attribEntry.getKey() + "=", StyledString.QUALIFIER_STYLER);
                    label.append(attribEntry.getValue());
                }
                cell.setText(label.toString());
                cell.setStyleRanges(label.getStyleRanges());
                cell.setImage(iconImg);
            }
        });
        viewer.setInput(entries);
        GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.widthHint = 280;
        gd.heightHint = 80;
        table.setLayoutData(gd);

        final AddRemoveButtonBarPart buttonBarPart = new AddRemoveButtonBarPart();
        ToolBar buttonBar = buttonBarPart.createControl(composite, SWT.FLAT | SWT.VERTICAL);
        buttonBar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        final Image imgEdit = ImageDescriptor.createFromURL(BUNDLE.getEntry("icons/edit.gif"))
            .createImage(parent.getDisplay());
        final Image imgEditDisabled = ImageDescriptor.createFromURL(BUNDLE.getEntry("icons/edit-disabled.gif"))
            .createImage(parent.getDisplay());
        final ToolItem btnEdit = new ToolItem(buttonBar, SWT.PUSH);
        btnEdit.setImage(imgEdit);
        btnEdit.setDisabledImage(imgEditDisabled);

        buttonBarPart.setRemoveEnabled(false);
        btnEdit.setEnabled(false);
        buttonBarPart.addListener(new AddRemoveListener() {
            @Override
            public void addSelected() {
                doAdd();
            }

            @Override
            public void removeSelected() {
                doRemove();
            }
        });
        btnEdit.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doEdit();
            }
        });
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                boolean enabled = !viewer.getSelection()
                    .isEmpty();
                buttonBarPart.setRemoveEnabled(enabled);
                btnEdit.setEnabled(enabled);
            }
        });
        viewer.addOpenListener(new IOpenListener() {
            @Override
            public void open(OpenEvent event) {
                doEdit();
            }
        });
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.DEL && e.stateMask == 0)
                    doRemove();
            }
        });
        composite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent ev) {
                iconImg.dispose();
                imgEdit.dispose();
                imgEditDisabled.dispose();
            }
        });

        return composite;
    }

    void doAdd() {
        AbstractNewEntryDialog dialog = dialogFactory.create(parent.getShell());
        if (dialog.open() == Window.OK) {
            Pair<String, Attrs> entry = dialog.getEntry();
            entries.add(entry);
            viewer.add(entry);
        }
    }

    void doRemove() {
        int[] indices = viewer.getTable()
            .getSelectionIndices();
        if (indices == null)
            return;

        List<Object> selected = new ArrayList<>(indices.length);
        for (int index : indices)
            selected.add(entries.get(index));
        entries.removeAll(selected);
        viewer.remove(selected.toArray());
    }

    void doEdit() {
        @SuppressWarnings("unchecked")
        Pair<String, Attrs> selected = (Pair<String, Attrs>) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
        AbstractNewEntryDialog dialog = dialogFactory.create(parent.getShell());
        dialog.setEntry(selected);
        if (dialog.open() == Window.OK) {
            Pair<String, Attrs> newEntry = dialog.getEntry();

            int index = entries.indexOf(selected);
            entries.set(index, newEntry);
            viewer.refresh();
        }
    }
}
