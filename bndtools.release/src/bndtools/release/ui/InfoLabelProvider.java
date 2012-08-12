package bndtools.release.ui;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.service.diff.Diff;

public class InfoLabelProvider extends ColumnLabelProvider {

	@Override
    public String getText(Object element) {
		if (element instanceof Baseline) {
			return ((Baseline) element).getBsn();
		}

		if (element instanceof Info) {
			return ((Info) element).packageName;
		}
		return "";
	}

	@Override
    public Image getImage(Object element) {

		if (element instanceof Baseline) {
		    Diff apiDiff = ((Baseline)element).getDiff() != null ? ((Baseline)element).getDiff().get("<api>") : null;
		    if (apiDiff == null) {
		        apiDiff = ((Baseline)element).getDiff();
		    }
            return BundleTreeImages.resolveImage("bundle", apiDiff.getDelta().toString().toLowerCase(), null, null);
		}
		if (element instanceof Info) {
			Info tree = (Info) element;
			String type = "package";
			String delta = "changed" + '_' + tree.packageDiff.getDelta().toString().toLowerCase();
			String impExp = "export";
			return BundleTreeImages.resolveImage(type, delta, impExp, null);
		}
		return null;
	}
}