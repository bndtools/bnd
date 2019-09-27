package bndtools.release.ui;

import java.util.Arrays;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class TableSortingEnabler {

	public static void applyTableColumnSorting(TableViewer tableViewer) {
		TableSortingEnabler enabler = new TableSortingEnabler();
		enabler.setTableViewer(tableViewer);
	}

	private TableViewer tableViewer;

	private TableSortingEnabler() {}

	private void setTableViewer(TableViewer tableViewer) {
		this.tableViewer = tableViewer;
		addColumnSelectionListeners(tableViewer);
		tableViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				return compareElements(e1, e2);
			}
		});
	}

	private void addColumnSelectionListeners(TableViewer tableViewer) {
		TableColumn[] columns = tableViewer.getTable()
			.getColumns();
		for (int i = 0; i < columns.length; i++) {
			addColumnSelectionListener(columns[i]);
		}
	}

	private void addColumnSelectionListener(TableColumn column) {
		column.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tableColumnClicked((TableColumn) e.widget);
			}
		});
	}

	private void tableColumnClicked(TableColumn column) {
		Table table = column.getParent();
		if (column.equals(table.getSortColumn())) {
			int direction = getSortDirection(table.getSortDirection());
			table.setSortDirection(direction);
			if (direction == SWT.NONE) {
				table.setSortColumn(null);
			}
		} else {
			table.setSortColumn(column);
			table.setSortDirection(getSortDirection(SWT.NONE));
		}
		tableViewer.refresh();
	}

	private static int getSortDirection(int currentDirection) {
		switch (currentDirection) {
			case SWT.NONE :
				return SWT.UP;
			case SWT.UP :
				return SWT.DOWN;
			default :
				return SWT.NONE;
		}
	}

	@SuppressWarnings({
		"rawtypes", "unchecked"
	})
	private int compareElements(Object e1, Object e2) {
		IColumnContentProvider columnValueProvider = (IColumnContentProvider) tableViewer.getContentProvider();
		Table table = tableViewer.getTable();
		int index = Arrays.asList(table.getColumns())
			.indexOf(table.getSortColumn());
		int result = 0;
		if (index != -1) {
			Comparable c1 = columnValueProvider.getValue(e1, index);
			Comparable c2 = columnValueProvider.getValue(e2, index);
			result = c1.compareTo(c2);
		}
		return table.getSortDirection() == SWT.UP ? result : -result;
	}

	public interface IColumnContentProvider {
		Comparable<?> getValue(Object element, int column);
	}
}
