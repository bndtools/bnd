package bndtools.release.ui;

import java.util.List;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class OverlayImage {

	private Point			size;
	private String			baseImage;
	private List<Overlay>	overlays;
	private ImageRegistry	reg;
	private String			key;

	public OverlayImage(ImageRegistry reg, String baseImageKey, List<Overlay> overlays) {
		this.reg = reg;
		this.baseImage = baseImageKey;
		this.overlays = overlays;
		this.size = new Point(16, 16);

		StringBuilder keyBuilder = new StringBuilder(baseImage);
		for (Overlay overlay : overlays) {
			keyBuilder.append('_');
			keyBuilder.append(overlay.getKey());
		}
		key = keyBuilder.toString();

	}

	private class OverlayImageDescriptor extends CompositeImageDescriptor {
		@SuppressWarnings("deprecation")
		@Override
		protected void drawCompositeImage(int width, int height) {

			Image id = reg.get(baseImage);

			// Draw the base image using the base image's image data
			drawImage(id.getImageData(), 0, 0);

			for (Overlay overlay : overlays) {
				// Overlaying the icon in the top left corner i.e. x and y
				// coordinates are both zero
				ImageDescriptor imgDescr = reg.getDescriptor(overlay.getKey());
				if (imgDescr == null) {
					return;
				}
				drawImage(imgDescr.getImageData(), overlay.getXValue(), overlay.getYValue());
			}
		}

		@Override
		protected Point getSize() {
			return size;
		}
	}

	public static class Overlay {
		private int		xValue	= 5;
		private int		yValue	= 6;
		private String	key;

		public Overlay(String key) {
			this.key = key;
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

		public String getKey() {
			return key;
		}
	}

	public Image getImage() {
		Image image = reg.get(key);
		if (image != null) {
			return image;
		}
		OverlayImageDescriptor descriptor = new OverlayImageDescriptor();
		reg.put(key, descriptor);
		return reg.get(key);
	}
}
