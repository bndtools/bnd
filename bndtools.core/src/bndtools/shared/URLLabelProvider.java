package bndtools.shared;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;

public class URLLabelProvider extends StyledCellLabelProvider {

	private final Image	linkImg;
	private final Image	fileImg;

	public URLLabelProvider(Device device) {
		linkImg = Icons.desc("link")
			.createImage(device);
		fileImg = Icons.desc("file")
			.createImage();
	}

	@Override
	public void update(ViewerCell cell) {
		Image img;
		String text;

		Object element = cell.getElement();
		if (element instanceof OBRLink) {
			StyledString label = ((OBRLink) element).getLabel();
			cell.setStyleRanges(label.getStyleRanges());
			text = label.getString();
		} else {
			text = (element == null ? "null" : element.toString());
		}

		if (text.startsWith("file:"))
			img = fileImg;
		else
			img = linkImg;

		cell.setText(text);
		cell.setImage(img);
	}

	@Override
	public void dispose() {
		super.dispose();
		linkImg.dispose();
		fileImg.dispose();
	}
}
