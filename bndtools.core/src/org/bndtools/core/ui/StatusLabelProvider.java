package org.bndtools.core.ui;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;

public class StatusLabelProvider extends StyledCellLabelProvider {

	private final static Image	imgError	= Icons.image("icons/error_obj.gif");
	private final static Image	imgWarning	= Icons.image("icons/warning_obj.gif");
	private final static Image	imgInfo		= Icons.image("icons/information.gif");

	@Override
	public void update(ViewerCell cell) {
		IStatus status = (IStatus) cell.getElement();

		switch (status.getSeverity()) {
			case IStatus.ERROR :
				cell.setImage(imgError);
				break;
			case IStatus.WARNING :
				cell.setImage(imgWarning);
				break;
			case IStatus.INFO :
				cell.setImage(imgInfo);
				break;
			case IStatus.OK :
			case IStatus.CANCEL :
			default :
				break;
		}

		StyledString label = new StyledString(status.getMessage());
		if (status.getException() != null)
			label.append(": " + status.getException()
				.toString(), StyledString.QUALIFIER_STYLER);

		cell.setText(label.getString());
		cell.setStyleRanges(label.getStyleRanges());
	}
}
