package bndtools.editor.common;

import java.util.Map;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

public class PropertiesTableLabelProvider extends StyledCellLabelProvider {

	@Override
	public void update(ViewerCell cell) {
		ColumnViewer viewer = getViewer();
		@SuppressWarnings("unchecked")
		Map<String, String> map = (Map<String, String>) viewer.getInput();

		String key = (String) cell.getElement();

		if (cell.getColumnIndex() == 0) {
			cell.setText(key);
		} else if (cell.getColumnIndex() == 1) {
			cell.setText(map.get(key));
		}
	}
}
