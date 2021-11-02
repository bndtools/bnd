package bndtools.editor.pkgpatterns;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;

import aQute.bnd.build.model.clauses.HeaderClause;

public abstract class HeaderClauseLabelProvider<C extends HeaderClause> extends StyledCellLabelProvider {

	private final static Image packageImg = Icons.image("package");

	@Override
	public void update(ViewerCell cell) {
		@SuppressWarnings("unchecked")
		C clause = (C) cell.getElement();

		cell.setImage(packageImg);

		StyledString styledString = new StyledString(clause.getName());
		String version = clause.getAttribs()
			.get(org.osgi.framework.Constants.VERSION_ATTRIBUTE);
		if (version != null) {
			styledString.append(": " + version, StyledString.COUNTER_STYLER);
		}

		decorate(styledString, clause);

		cell.setText(styledString.getString());
		cell.setStyleRanges(styledString.getStyleRanges());
	}

	@SuppressWarnings("unused")
	protected void decorate(StyledString label, C clause) {}
}
