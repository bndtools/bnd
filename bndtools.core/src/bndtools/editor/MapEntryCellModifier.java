/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package bndtools.editor;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Map;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;


public class MapEntryCellModifier<K,V> implements ICellModifier {
	
	private final PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);
	
	private static final String PROP_NAME = "NAME";
	private static final String PROP_VALUE = "VALUE";
	
	private static final String[] PROPS = new String[] { PROP_NAME, PROP_VALUE };
	private static final String[] LABELS = new String[] { "Name", "Value" };
	
	private TableViewer viewer;
	
	public MapEntryCellModifier(TableViewer viewer) {
		this.viewer = viewer;
	}
	
	public boolean canModify(Object element, String property) {
		return PROP_NAME.equals(property) || PROP_VALUE.equals(property);
	}

	public Object getValue(Object element, String property) {
        Object result = null;
		
		@SuppressWarnings("unchecked")
		K key = (K) element;
		if(PROP_NAME.equals(property)) {
			result = key;
		} else if(PROP_VALUE.equals(property)) {
			@SuppressWarnings("unchecked")
			Map<K,V> map = (Map<K, V>) viewer.getInput();
			result = map.get(key);
		}
        if (result == null)
            result = "";
		return result;
	}
	public void modify(Object element, String property, Object editResult) {
		@SuppressWarnings("unchecked")
		Map<K,V> map = (Map<K, V>) viewer.getInput();

		if(element instanceof Item) {
			element = ((Item) element).getData();
		}
		@SuppressWarnings("unchecked")
		K key = (K) element;
		
		boolean changed = false;
		if(PROP_VALUE.equals(property)) {
			@SuppressWarnings("unchecked")
			V newValue = (V) editResult;
			V previous = map.put(key, newValue);
			
			changed = (newValue == null && previous != null)
					|| !newValue.equals(previous);
			viewer.refresh(key);
		} else if(PROP_NAME.equals(property)) {
			if(!element.equals(editResult)) {
				V value = map.remove(key);
				viewer.remove(key);
				
				@SuppressWarnings("unchecked")
				K newKey = (K) editResult;
				map.put(newKey, value);
				viewer.add(newKey);
				changed = true;
			}
		}
		
		if(changed)
			propertySupport.firePropertyChange(property, null, editResult);
	}
	public void addColumnsToTable() {
		Table table = viewer.getTable();
		
		for (String label : LABELS) {
			TableColumn col = new TableColumn(table, SWT.NONE);
			col.setText(label);
			col.setWidth(175);
		}
	}
	public String[] getColumnProperties() {
		return PROPS;
	}
	public void addCellEditorsToViewer() {
		CellEditor[] cellEditors = new CellEditor[PROPS.length];
		for (int i = 0; i < PROPS.length; i++) {
			cellEditors[i] = new TextCellEditor(viewer.getTable());
		}
		viewer.setCellEditors(cellEditors);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertySupport.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertySupport.removePropertyChangeListener(listener);
	}
}
