package bndtools.model.clauses;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.namespace.IdentityNamespace;

import aQute.bnd.osgi.resource.ResourceUtils;

public class VersionedClauseLabelProvider extends StyledCellLabelProvider {

	final static Image	bundleImg	= Icons.image("bundle");
	final static Image	featureImg	= Icons.image("feature");

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
		boolean isFeature = ResourceUtils.TYPE_ECLIPSE_FEATURE.equals(clause.getAttribs()
			.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
		cell.setImage(isFeature ? featureImg : bundleImg);
	}

}
