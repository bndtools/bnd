package bndtools.preferences.ui.obr;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.service.RepositoryPlugin;
import bndtools.Plugin;
import bndtools.WorkspaceObrProvider;

public class ObrIndexProviderLabelProvider extends StyledCellLabelProvider {

    private final Image repositoryImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/bundlefolder.png").createImage();
    private final Image projectImg = PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);

    @Override
    public void update(ViewerCell cell) {
        Object element = cell.getElement();

        cell.setText(getName(element));

        if (element instanceof WorkspaceObrProvider)
            cell.setImage(projectImg);
        else
            cell.setImage(repositoryImg);
    }

    String getName(Object element) {
        String name;
        if (element instanceof RepositoryPlugin) {
            name = ((RepositoryPlugin) element).getName();
        } else {
            name = element.toString();
        }
        return name;
    }
}
