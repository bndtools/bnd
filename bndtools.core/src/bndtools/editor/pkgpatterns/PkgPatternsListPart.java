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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Constants;

import bndtools.Plugin;
import bndtools.editor.model.BndEditModel;
import bndtools.editor.model.HeaderClause;
import bndtools.utils.CollectionUtils;
import bndtools.utils.PackageDropAdapter;

public abstract class PkgPatternsListPart<C extends HeaderClause> extends SectionPart implements PropertyChangeListener {

	private final String propertyName;
	protected ArrayList<C> clauses = new ArrayList<C>();
	
	private IManagedForm managedForm;
	private TableViewer viewer;
	private BndEditModel model;
	
	private final Image imgAnalyse = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/cog_go.png").createImage();
	
	public PkgPatternsListPart(Composite parent, FormToolkit toolkit, int style, String propertyName) {
		super(parent, toolkit, style);
		this.propertyName = propertyName;
		createSection(getSection(), toolkit);
	}
	void createSection(Section section, FormToolkit toolkit) {
		section.setText("Package Patterns");
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		Table table = toolkit.createTable(composite, SWT.MULTI | SWT.FULL_SELECTION);
		viewer = new TableViewer(table);
		viewer.setUseHashlookup(true);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new PkgPatternsLabelProvider());
		
		final Button btnAdd = toolkit.createButton(composite, "Add...", SWT.PUSH);
		final Button btnInsert = toolkit.createButton(composite, "Insert", SWT.PUSH);
		final Button btnRemove = toolkit.createButton(composite, "Remove", SWT.PUSH);
		final Button btnMoveUp = toolkit.createButton(composite, "Up", SWT.PUSH);
		final Button btnMoveDown = toolkit.createButton(composite, "Down", SWT.PUSH);
		toolkit.createLabel(composite, ""); // Spacer
		
		// Listeners
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				managedForm.fireSelectionChanged(PkgPatternsListPart.this, event.getSelection());
				boolean enabled = !viewer.getSelection().isEmpty();
				btnInsert.setEnabled(enabled);
				btnRemove.setEnabled(enabled);
				btnMoveUp.setEnabled(enabled);
				btnMoveDown.setEnabled(enabled);
			}
		});
		viewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] { ResourceTransfer.getInstance(), TextTransfer.getInstance() }, new PackageDropAdapter<C>(viewer) {
			@Override
			protected C createNewEntry(String packageName) {
				return newHeaderClause(packageName);
			}
			@Override
			protected void addRows(int index, Collection<C> rows) {
				doAddClauses(rows, index, true);
				validate();
				markDirty();
			}
			@Override
			protected int indexOf(Object object) {
				return clauses.indexOf(object);
			}
		});
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAddClausesAfterSelection(generateClauses());
			}
		});
		btnInsert.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doInsertClausesAtSelection(generateClauses());
			}
		});
		btnRemove.addSelectionListener(new SelectionAdapter() {
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
		layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 6));
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnInsert.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnMoveUp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnMoveDown.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
	}
	protected abstract C newHeaderClause(String text);
	protected abstract Collection<C> loadFromModel(BndEditModel model);
	protected abstract void saveToModel(BndEditModel model, Collection<? extends C> clauses);
	protected List<C> getClauses() {
		return clauses;
	}
	protected Collection<? extends C> generateClauses() {
		Collection<C> result = new ArrayList<C>();
		result.add(newHeaderClause(""));
		return result;
	}
	/**
	 * Add the specified clauses to the view.
	 * @param newClauses The new clauses.
	 * @param index The index at which to insert the new clauses OR -1 to append at the end.
	 */
	protected void doAddClauses(Collection<? extends C> newClauses, int index, boolean select) {
		Object[] newClausesArray = newClauses.toArray(new Object[newClauses.size()]);
		
		if(index == -1 || index == this.clauses.size()) {
			clauses.addAll(newClauses);
			viewer.add(newClausesArray);
		} else {
			clauses.addAll(index, newClauses);
			viewer.refresh();
		}
		
		if(select)
			viewer.setSelection(new StructuredSelection(newClausesArray), true);
		validate();
		markDirty();
	}
	private void doAddClausesAfterSelection(Collection<? extends C> newClauses) {
		if(newClauses != null && !newClauses.isEmpty()) {
			int selectedIndex = -1;
			IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
			if(!selection.isEmpty()) {
				// find the highest selected index
				for(Iterator<?> iter = selection.iterator(); iter.hasNext(); ) {
					int index = this.clauses.indexOf(iter.next());
					if(index > selectedIndex) selectedIndex = index;
				}
			}
			doAddClauses(newClauses, selectedIndex, true);
		}
	}
	private void doInsertClausesAtSelection(Collection<? extends C> newClauses) {
		if(newClauses != null && !newClauses.isEmpty()) {
			int selectedIndex;
			IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
			if(selection.isEmpty())
				return;
			selectedIndex = this.clauses.indexOf(selection.getFirstElement());
			
			doAddClauses(newClauses, selectedIndex, true);
		}
	}
	protected void doRemoveClauses(List<?> toRemove) {
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
		CollectionUtils.moveUp(clauses, selectedIndexes);
		viewer.refresh();
		validate();
		markDirty();
	}
	void doMoveDown() {
		int[] selectedIndexes = findSelectedIndexes();
		CollectionUtils.moveDown(clauses, selectedIndexes);
		viewer.refresh();
		validate();
		markDirty();
	}
	int[] findSelectedIndexes() {
		Object[] selection = ((IStructuredSelection) viewer.getSelection()).toArray();
		int[] selectionIndexes = new int[selection.length];
		
		for(int i=0; i<selection.length; i++) {
			selectionIndexes[i] = clauses.indexOf(selection[i]);
		}
		return selectionIndexes;
	}
	/*
	void resetSelection(int[] selectedIndexes) {
		ArrayList<ImportPattern> selection = new ArrayList<ImportPattern>(selectedIndexes.length);
		for (int index : selectedIndexes) {
			selection.add(patterns.get(index));
		}
		viewer.setSelection(new StructuredSelection(selection), true);
	}
	*/
;	@Override
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
	}
	@Override
	public void refresh() {
		super.refresh();
		
		// Deep-copy the model
		Collection<C> tmp = loadFromModel(model);
		if(tmp != null) {
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
	public void propertyChange(PropertyChangeEvent evt) {
		IFormPage page = (IFormPage) managedForm.getContainer();
		if(page.isActive())
			refresh();
		else
			markStale();
	}
	public void updateLabels(Object[] elements) {
		viewer.update(elements, null);
	}
	public void setSelectedClause(C clause) {
		viewer.setSelection(new StructuredSelection(clause));
	}
}
