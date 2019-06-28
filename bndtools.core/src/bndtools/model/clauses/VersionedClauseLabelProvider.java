package bndtools.model.clauses;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;

public class VersionedClauseLabelProvider extends StyledCellLabelProvider {

	Image bundleImg = Icons.desc("bundle")
		.createImage();

	@Override
	public void update(ViewerCell cell) {
		aQute.bnd.build.model.clauses.VersionedClause clause = (aQute.bnd.build.model.clauses.VersionedClause) cell
			.getElement();
		StyledString label = new StyledString(clause.getName());
		String version = clause.getVersionRange();
		if (version != null) {
			label.append(" " + version, StyledString.COUNTER_STYLER);
		}
		cell.setText(label.getString());
		cell.setStyleRanges(label.getStyleRanges());
		cell.setImage(bundleImg);
	}

	@Override
	public void dispose() {
		super.dispose();
		bundleImg.dispose();
	}
}
