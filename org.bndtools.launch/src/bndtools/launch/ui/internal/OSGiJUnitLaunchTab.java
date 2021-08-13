package bndtools.launch.ui.internal;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.swt.graphics.Image;

import bndtools.launch.ui.FrameworkLaunchTabPiece;
import bndtools.launch.ui.GenericStackedLaunchTab;
import bndtools.launch.ui.ILaunchTabPiece;
import bndtools.launch.ui.ProjectLaunchTabPiece;

public class OSGiJUnitLaunchTab extends GenericStackedLaunchTab {

	private Image image = null;

	@Override
	protected ILaunchTabPiece[] createStack() {
		return new ILaunchTabPiece[] {
			new ProjectLaunchTabPiece(), new FrameworkLaunchTabPiece(), new JUnitTestParamsLaunchTabPiece()
		};
	}

	@Override
	public String getName() {
		return "OSGi Tests";
	}

	@Override
	public Image getImage() {
		synchronized (this) {
			if (image == null) {
				image = Icons.desc("bundle")
					.createImage();
			}
			return image;
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		synchronized (this) {
			if (image != null)
				image.dispose();
		}
	}
}
