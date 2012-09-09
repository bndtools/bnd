package bndtools.ace.launch.ui;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.launch.ui.GenericStackedLaunchTab;
import bndtools.launch.ui.ILaunchTabPiece;
import bndtools.launch.ui.ProjectLaunchTabPiece;

public class AceLaunchTab extends GenericStackedLaunchTab {
	public static final String PLUGIN_ID = "bndtools.core";
	
    private Image image = null;


    @Override
    protected ILaunchTabPiece[] createStack() {
        return new ILaunchTabPiece[] {
                new ProjectLaunchTabPiece(),
                new AceConfigLaunchTabPiece()
        };
    }

    public String getName() {
        return "ACE";
    }

    @Override
    public Image getImage() {
        synchronized (this) {
            if (image == null) {
                image = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, "/icons/brick.png").createImage();
            }
        }
        return image;
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
