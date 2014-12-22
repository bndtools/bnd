package bndtools.launch.ui.internal;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.graphics.Image;

import bndtools.launch.LaunchConstants;
import bndtools.launch.ui.GenericStackedLaunchTab;
import bndtools.launch.ui.ILaunchTabPiece;
import bndtools.launch.ui.ProjectLaunchTabPiece;

public class OSGiJUnitLaunchTab extends GenericStackedLaunchTab {

    private Image image = null;

    @Override
    protected ILaunchTabPiece[] createStack() {
        return new ILaunchTabPiece[] {
                new ProjectLaunchTabPiece(), new JUnitTestParamsLaunchTabPiece()
        };
    }

    @Override
    public String getName() {
        return "OSGi Tests";
    }

    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        super.performApply(configuration);
        configuration.setAttribute(LaunchConstants.ATTR_DYNAMIC_BUNDLES, false);
    }

    @Override
    public Image getImage() {
        synchronized (this) {
            if (image == null) {
                image = Icons.desc("bundle").createImage();
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