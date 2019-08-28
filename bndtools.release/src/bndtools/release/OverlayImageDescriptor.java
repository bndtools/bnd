package bndtools.release;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class OverlayImageDescriptor extends CompositeImageDescriptor {

	private Point			size;
	private String			baseImage;
	private String			overlayImage;
	private int				xValue	= 5;
	private int				yValue	= 6;
	private ImageRegistry	reg;

	public OverlayImageDescriptor(ImageRegistry reg, String baseImageKey, String overlayImageKey) {
		this.reg = reg;
		this.baseImage = baseImageKey;
		this.overlayImage = overlayImageKey;
		this.size = new Point(16, 16);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void drawCompositeImage(int width, int height) {

		Image id = reg.get(baseImage);

		// Draw the base image using the base image's image data
		drawImage(id.getImageData(), 0, 0);

		// Overlaying the icon in the top left corner i.e. x and y
		// coordinates are both zero
		ImageDescriptor imgDescr = reg.getDescriptor(overlayImage);
		if (imgDescr == null) {
			return;
		}
		drawImage(imgDescr.getImageData(), xValue, yValue);
	}

	@Override
	protected Point getSize() {
		return size;
	}

	public int getXValue() {
		return xValue;
	}

	public void setXValue(int xValue) {
		this.xValue = xValue;
	}

	public int getYValue() {
		return yValue;
	}

	public void setYValue(int yValue) {
		this.yValue = yValue;
	}
}
