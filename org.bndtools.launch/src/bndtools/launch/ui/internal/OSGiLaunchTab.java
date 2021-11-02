package bndtools.launch.ui.internal;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.swt.graphics.Image;

import bndtools.launch.ui.FrameworkLaunchTabPiece;
import bndtools.launch.ui.GenericStackedLaunchTab;
import bndtools.launch.ui.ILaunchTabPiece;
import bndtools.launch.ui.ProjectLaunchTabPiece;

public class OSGiLaunchTab extends GenericStackedLaunchTab {

	@Override
	protected ILaunchTabPiece[] createStack() {
		return new ILaunchTabPiece[] {
			new ProjectLaunchTabPiece(), new FrameworkLaunchTabPiece()
		};
	}

	@Override
	public String getName() {
		return "OSGi";
	}

	@Override
	public Image getImage() {
		return Icons.image("bundle");
	}

}
