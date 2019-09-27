package bndtools.editor.common;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Map;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Item;

public class MapEntryCellModifier<K, V> implements ICellModifier {

	private final PropertyChangeSupport	propertySupport	= new PropertyChangeSupport(this);

	private static final String			PROP_NAME		= "NAME";
	private static final String			PROP_VALUE		= "VALUE";

	private static final String[]		PROPS			= new String[] {
		PROP_NAME, PROP_VALUE
	};

	private final TableViewer			viewer;

	public MapEntryCellModifier(TableViewer viewer) {
		this.viewer = viewer;
	}

	@Override
	public boolean canModify(Object element, String property) {
		return PROP_NAME.equals(property) || PROP_VALUE.equals(property);
	}

	@Override
	public Object getValue(Object element, String property) {
		Object result = null;

		@SuppressWarnings("unchecked")
		K key = (K) element;
		if (PROP_NAME.equals(property)) {
			result = key;
		} else if (PROP_VALUE.equals(property)) {
			@SuppressWarnings("unchecked")
			Map<K, V> map = (Map<K, V>) viewer.getInput();
			result = map.get(key);
		}
		if (result == null)
			result = "";
		return result;
	}

	@Override
	public void modify(Object element, String property, Object editResult) {
		@SuppressWarnings("unchecked")
		Map<K, V> map = (Map<K, V>) viewer.getInput();

		Object e = element;
		if (e instanceof Item) {
			e = ((Item) e).getData();
		}
		@SuppressWarnings("unchecked")
		K key = (K) e;

		boolean changed = false;
		if (PROP_VALUE.equals(property)) {
			@SuppressWarnings("unchecked")
			V newValue = (V) editResult;
			V previous = map.put(key, newValue);

			changed = (newValue == null && previous != null) || (newValue != null && !newValue.equals(previous));
			viewer.refresh(key);
		} else if (PROP_NAME.equals(property)) {
			if (!e.equals(editResult)) {
				V value = map.remove(key);
				viewer.remove(key);

				@SuppressWarnings("unchecked")
				K newKey = (K) editResult;
				map.put(newKey, value);
				viewer.add(newKey);
				changed = true;
			}
		}

		if (changed)
			propertySupport.firePropertyChange(property, null, editResult);
	}

	public static String[] getColumnProperties() {
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
