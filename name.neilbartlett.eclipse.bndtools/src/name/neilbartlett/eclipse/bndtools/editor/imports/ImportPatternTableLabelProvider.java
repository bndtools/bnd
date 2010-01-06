package name.neilbartlett.eclipse.bndtools.editor.imports;

import java.util.Map;

import name.neilbartlett.eclipse.bndtools.editor.model.ImportPattern;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class ImportPatternTableLabelProvider extends LabelProvider implements
		ITableLabelProvider {

	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		String text;
		ImportPattern pattern = (ImportPattern) element;
		switch(columnIndex) {
		case 0:
			text = pattern.getPattern();
			break;
		case 1:
			text = pattern.isOptional() ? Boolean.TRUE.toString() : null;
			break;
		case 2:
			Map<String, String> attribs = pattern.getAttributes();
			text = (attribs != null && !attribs.isEmpty())? attribs.toString() : null;
			break;
		default:
			text = "<<error>>";
		}
		return text;
	}

}
