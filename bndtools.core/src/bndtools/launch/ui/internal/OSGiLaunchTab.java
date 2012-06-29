package bndtools.launch.ui.internal;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.launch.ui.FrameworkLaunchTabPiece;
import bndtools.launch.ui.GenericStackedLaunchTab;
import bndtools.launch.ui.ILaunchTabPiece;
import bndtools.launch.ui.ProjectLaunchTabPiece;

public class OSGiLaunchTab extends GenericStackedLaunchTab {

    private Image image = null;

    @Override
    protected ILaunchTabPiece[] createStack() {
        return new ILaunchTabPiece[] {
                new ProjectLaunchTabPiece(), new FrameworkLaunchTabPiece()
        };
    }

    public String getName() {
        return "OSGi";
    }

    @Override
    public Image getImage() {
        synchronized (this) {
            if (image == null) {
                image = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();
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