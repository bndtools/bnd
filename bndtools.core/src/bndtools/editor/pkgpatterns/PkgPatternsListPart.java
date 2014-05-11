/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools.editor.pkgpatterns;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.bndtools.utils.collections.CollectionUtils;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Constants;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.HeaderClause;
import bndtools.Plugin;
import bndtools.editor.common.PackageDropAdapter;

public abstract class PkgPatternsListPart<C extends HeaderClause> extends SectionPart implements PropertyChangeListener {

    protected static final String PROP_SELECTION = "selection";
    private final PropertyChangeSupport propChangeSupport = new PropertyChangeSupport(this);

    private final String propertyName;
    private final IBaseLabelProvider labelProvider;
    protected ArrayList<C> clauses = new ArrayList<C>();

    private IManagedForm managedForm;
    private TableViewer viewer;
    private BndEditModel model;

    private List<C> selection;

    private final Image imgAnalyse = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/cog_go.png").createImage();
    private final Image imgInsert = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/table_row_insert.png").createImage();
    private final Image imgUp = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/arrow_up.png").createImage();
    private final Image imgDown = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/arrow_down.png").createImage();

    protected final String title;

    public PkgPatternsListPart(Composite parent, FormToolkit toolkit, int style, String propertyName, String title, IBaseLabelProvider labelProvider) {
        super(parent, toolkit, style);
        this.propertyName = propertyName;
        this.title = title;
        this.labelProvider = labelProvider;

        Section section = getSection();
        section.setText(title);
        createSection(section, toolkit);
    }

    protected void createSection(Section section, FormToolkit toolkit) {
        Composite composite = toolkit.createComposite(section);
        section.setClient(composite);

        ToolBar toolbar = new ToolBar(section, SWT.FLAT);
        section.setTextClient(toolbar);
        final ToolItem addItem = new ToolItem(toolbar, SWT.PUSH);
        addItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
        addItem.setToolTipText("Add");

        final ToolItem insertItem = new ToolItem(toolbar, SWT.PUSH);
        insertItem.setImage(imgInsert);
        insertItem.setToolTipText("Insert");
        insertItem.setEnabled(false);

        final ToolItem removeItem = new ToolItem(toolbar, SWT.PUSH);
        removeItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
        removeItem.setDisabledImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE_DISABLED));
        removeItem.setToolTipText("Remove");
        removeItem.setEnabled(false);

        Table table = toolkit.createTable(composite, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
        viewer = new TableViewer(table);
        viewer.setUseHashlookup(false);
        viewer.setContentProvider(new ArrayContentProvider());
        viewer.setLabelProvider(labelProvider);

        toolbar = new ToolBar(composite, SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);

        final ToolItem btnMoveUp = new ToolItem(toolbar, SWT.PUSH);
        btnMoveUp.setText("Up");
        btnMoveUp.setImage(imgUp);
        btnMoveUp.setEnabled(false);

        final ToolItem btnMoveDown = new ToolItem(toolbar, SWT.PUSH);
        btnMoveDown.setText("Down");
        btnMoveDown.setImage(imgDown);
        btnMoveDown.setEnabled(false);

        // Listeners
        table.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                ISelection selection = viewer.getSelection();
                if (!selection.isEmpty())
                    managedForm.fireSelectionChanged(PkgPatternsListPart.this, selection);
            }
        });
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                List<C> oldSelection = selection;
                IStructuredSelection structSel = (IStructuredSelection) event.getSelection();

                @SuppressWarnings("unchecked")
                List<C> newSelection = structSel.toList();
                selection = newSelection;

                propChangeSupport.firePropertyChange(PROP_SELECTION, oldSelection, selection);

                managedForm.fireSelectionChanged(PkgPatternsListPart.this, event.getSelection());
                boolean enabled = !viewer.getSelection().isEmpty();
                insertItem.setEnabled(enabled);
                removeItem.setEnabled(enabled);
                btnMoveUp.setEnabled(enabled);
                btnMoveDown.setEnabled(enabled);
            }
        });
        viewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] {
                LocalSelectionTransfer.getTransfer(), ResourceTransfer.getInstance(), TextTransfer.getInstance()
        }, new PackageDropAdapter<C>(viewer) {
            @Override
            protected C createNewEntry(String packageName) {
                return newHeaderClause(packageName);
            }

            @Override
            protected void addRows(int index, Collection<C> rows) {
                doAddClauses(rows, index, true);
            }

            @Override
            protected int indexOf(Object object) {
                return clauses.indexOf(object);
            }
        });
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.character == SWT.DEL) {
                    doRemoveSelectedClauses();
                } else if (e.character == '+') {
                    doAddClausesAfterSelection(generateClauses());
                }
            }
        });
        addItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doAddClausesAfterSelection(generateClauses());
            }
        });
        insertItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doInsertClausesAtSelection(generateClauses());
            }
        });
        removeItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doRemoveSelectedClauses();
            }
        });
        btnMoveUp.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doMoveUp();
            }
        });
        btnMoveDown.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doMoveDown();
            }
        });

        // Layout
        GridLayout layout;

        layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 0;
        layout.horizontalSpacing = 0;
        composite.setLayout(layout);

        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 75;
        gd.heightHint = 75;
        table.setLayoutData(gd);
        toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    }

    protected abstract C newHeaderClause(String text);

    protected abstract List<C> loadFromModel(BndEditModel model);

    protected abstract void saveToModel(BndEditModel model, List< ? extends C> clauses);

    protected List<C> getClauses() {
        return clauses;
    }

    protected Collection< ? extends C> generateClauses() {
        Collection<C> result = new ArrayList<C>();
        result.add(newHeaderClause(""));
        return result;
    }

    /**
     * Add the specified clauses to the view.
     *
     * @param newClauses
     *            The new clauses.
     * @param index
     *            The index at which to insert the new clauses OR -1 to append at the end.
     */
    protected void doAddClauses(Collection< ? extends C> newClauses, int index, boolean select) {
        Object[] newClausesArray = newClauses.toArray(new Object[newClauses.size()]);

        if (index == -1 || index == this.clauses.size()) {
            clauses.addAll(newClauses);
            viewer.add(newClausesArray);
        } else {
            clauses.addAll(index, newClauses);
            viewer.refresh();
        }

        if (select)
            viewer.setSelection(new StructuredSelection(newClausesArray), true);
        validate();
        markDirty();
    }

    private void doAddClausesAfterSelection(Collection< ? extends C> newClauses) {
        if (newClauses != null && !newClauses.isEmpty()) {
            int selectedIndex = -1;
            IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
            if (!selection.isEmpty()) {
                // find the highest selected index
                for (Iterator< ? > iter = selection.iterator(); iter.hasNext();) {
                    int index = this.clauses.indexOf(iter.next());
                    if (index > selectedIndex)
                        selectedIndex = index;
                }
            }
            doAddClauses(newClauses, selectedIndex, true);
        }
    }

    private void doInsertClausesAtSelection(Collection< ? extends C> newClauses) {
        if (newClauses != null && !newClauses.isEmpty()) {
            int selectedIndex;
            IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
            if (selection.isEmpty())
                return;
            selectedIndex = this.clauses.indexOf(selection.getFirstElement());

            doAddClauses(newClauses, selectedIndex, true);
        }
    }

    protected void doRemoveClauses(List< ? > toRemove) {
        clauses.removeAll(toRemove);
        viewer.remove(toRemove.toArray());

        validate();
        markDirty();
    }

    private void doRemoveSelectedClauses() {
        doRemoveClauses(((IStructuredSelection) viewer.getSelection()).toList());
        validate();
        markDirty();
    }

    void doMoveUp() {
        int[] selectedIndexes = findSelectedIndexes();
        if (CollectionUtils.moveUp(clauses, selectedIndexes)) {
            viewer.refresh();
            validate();
            markDirty();
        }
    }

    void doMoveDown() {
        int[] selectedIndexes = findSelectedIndexes();
        if (CollectionUtils.moveDown(clauses, selectedIndexes)) {
            viewer.refresh();
            validate();
            markDirty();
        }
    }

    int[] findSelectedIndexes() {
        Object[] selection = ((IStructuredSelection) viewer.getSelection()).toArray();
        int[] selectionIndexes = new int[selection.length];

        for (int i = 0; i < selection.length; i++) {
            selectionIndexes[i] = clauses.indexOf(selection[i]);
        }
        return selectionIndexes;
    }

    @Override
    public void initialize(IManagedForm form) {
        super.initialize(form);

        this.managedForm = form;
        this.model = (BndEditModel) form.getInput();
        this.model.addPropertyChangeListener(Constants.IMPORT_PACKAGE, this);
    }

    @Override
    public void dispose() {
        super.dispose();
        this.model.removePropertyChangeListener(Constants.IMPORT_PACKAGE, this);
        imgAnalyse.dispose();
        imgInsert.dispose();
        imgUp.dispose();
        imgDown.dispose();
    }

    @Override
    public void refresh() {
        super.refresh();

        // Deep-copy the model
        Collection<C> tmp = loadFromModel(model);
        if (tmp != null) {
            clauses = new ArrayList<C>(tmp.size());
            for (C clause : tmp) {
                @SuppressWarnings("unchecked")
                C clone = (C) clause.clone();
                clauses.add(clone);
            }
        } else {
            clauses = new ArrayList<C>();
        }
        viewer.setInput(clauses);
        validate();
    }

    public void validate() {
        // Do nothing.
    }

    @Override
    public void commit(boolean onSave) {
        try {
            model.removePropertyChangeListener(propertyName, this);
            saveToModel(model, clauses.isEmpty() ? null : clauses);
        } finally {
            super.commit(onSave);
            model.addPropertyChangeListener(propertyName, this);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        IFormPage page = (IFormPage) managedForm.getContainer();
        if (page.isActive())
            refresh();
        else
            markStale();
    }

    public void updateLabels(Collection< ? > elements) {
        updateLabels(elements.toArray());
    }

    public void updateLabels(Object[] elements) {
        viewer.update(elements, null);
    }

    public ISelectionProvider getSelectionProvider() {
        return viewer;
    }

    public List<C> getSelection() {
        return selection;
    }

    public void setSelection(List<C> selection) {
        List<C> oldSelection = selection;
        this.selection = selection;
        propChangeSupport.firePropertyChange(PROP_SELECTION, oldSelection, selection);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        propChangeSupport.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        propChangeSupport.addPropertyChangeListener(l);
    }

    public void addPropertyChangeListener(String name, PropertyChangeListener l) {
        propChangeSupport.addPropertyChangeListener(name, l);
    }

    public void removePropertyChangeListener(String name, PropertyChangeListener l) {
        propChangeSupport.addPropertyChangeListener(name, l);
    }

}
