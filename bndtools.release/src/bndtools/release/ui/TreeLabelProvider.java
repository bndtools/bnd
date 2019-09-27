package bndtools.release.ui;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

import aQute.bnd.differ.Baseline;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Tree;
import aQute.bnd.service.diff.Type;

public class TreeLabelProvider extends ColumnLabelProvider {

	@Override
	public String getText(Object element) {
		if (element instanceof Baseline) {
			return ((Baseline) element).getBsn();
		}

		if (element instanceof Diff) {
			Diff diff = (Diff) element;
			switch (diff.getType()) {
				case API :
					return Type.API.toString();
				case MANIFEST :
					return Type.MANIFEST.toString();
				case RESOURCES :
					return Type.RESOURCES.toString();
				case RESOURCE :
					return diff.getName();
				default :
					return diff.getName();
			}
		}
		if (element instanceof Tree) {
			Tree tree = (Tree) element;
			switch (tree.getType()) {
				case API :
					return Type.API.toString();
				case MANIFEST :
					return Type.MANIFEST.toString();
				case RESOURCES :
					return Type.RESOURCES.toString();
				case RESOURCE :
					return tree.getName();
				default :
					return tree.getName();
			}
		}
		return "";
	}

	@Override
	public Image getImage(Object element) {

		if (element instanceof Baseline) {
			return BundleTreeImages.resolveImage("bundle", ((Baseline) element).getDiff() //$NON-NLS-1$
				.getDelta()
				.toString()
				.toLowerCase(), null, null);
		}
		if (element instanceof Diff) {
			Diff tree = (Diff) element;
			String type = tree.getType()
				.toString()
				.toLowerCase();

			String strDelta = getDeltaString(tree);
			String impExp = null;
			if (tree.getType() == Type.PACKAGE) {
				impExp = "export"; //$NON-NLS-1$
			} else if (tree.getType() == Type.RESOURCE) {
				String name = tree.getName();
				int idx = name.lastIndexOf('.');
				if (idx > -1) {
					type = "dot_" + name.substring(idx + 1); //$NON-NLS-1$
				}
			}
			return BundleTreeImages.resolveImage(type, strDelta, impExp, null);
		}
		if (element instanceof Tree) {
			Tree tree = (Tree) element;
			String type = tree.getType()
				.toString()
				.toLowerCase();
			String impExp = null;
			if (tree.getType() == Type.PACKAGE) {
				impExp = "export"; //$NON-NLS-1$
			} else if (tree.getType() == Type.RESOURCE) {
				String name = tree.getName();
				int idx = name.lastIndexOf('.');
				if (idx > 0) {
					type = "dot_" + name.substring(idx + 1); //$NON-NLS-1$
				}
			}
			return BundleTreeImages.resolveImage(type, null, impExp, null);
		}
		return null;
	}

	private static String getDeltaString(Diff diff) {
		return diff.getDelta()
			.toString()
			.toLowerCase();
	}

}
