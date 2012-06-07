package bndtools.model.repo;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.build.Project;
import aQute.bnd.service.IndexProvider;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.Central;
import bndtools.Plugin;
import bndtools.WorkspaceObrProvider;

public class RepositoryTreeLabelProvider extends StyledCellLabelProvider implements ILabelProvider {

    Image localRepoImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/database.png").createImage();
    Image remoteRepoImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/database_link.png").createImage();
    Image bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/brick.png").createImage();
    Image projectImg = PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);

    @Override
    public void update(ViewerCell cell) {
        WorkspaceObrProvider workspaceObr;
        try {
            workspaceObr = Central.getWorkspaceObrProvider();
        } catch (Exception e) {
            workspaceObr = null;
        }

        Object element = cell.getElement();
        int index = cell.getColumnIndex();

        if (element instanceof RepositoryPlugin) {
            if (index == 0) {
                RepositoryPlugin repo = (RepositoryPlugin) element;
                StyledString label = new StyledString(repo.getName());
                if (!(element instanceof WorkspaceObrProvider))
                    label.append(" " + repo.getLocation(), StyledString.QUALIFIER_STYLER);

                cell.setText(label.getString());
                cell.setStyleRanges(label.getStyleRanges());

                Image image;
                if (element instanceof WorkspaceObrProvider)
                    image = projectImg;
                else if (isRemoteRepo((RepositoryPlugin) element))
                    image = remoteRepoImg;
                else
                    image = localRepoImg;
                
                cell.setImage(image);
            }
        } else if (element instanceof Project) {
            if (index == 0) {
                Project project = (Project) element;
                cell.setText(project.getName());
                cell.setImage(projectImg);
            }
        } else if (element instanceof ProjectBundle) {
            if (index == 0) {
                String name = ((ProjectBundle) element).getBsn();
                cell.setText(name);
                cell.setImage(bundleImg);
            }
        } else if (element instanceof RepositoryBundle) {
            if (index == 0) {
                RepositoryBundle bundle = (RepositoryBundle) element;
                cell.setText(bundle.getBsn());
                cell.setImage(bundleImg);
            }
        } else if (element instanceof RepositoryBundleVersion) {
            if (index == 0) {
                RepositoryBundleVersion bundleVersion = (RepositoryBundleVersion) element;
                StyledString styledString = new StyledString(bundleVersion.getVersion().toString(), StyledString.COUNTER_STYLER);
                cell.setText(styledString.getString());
                cell.setStyleRanges(styledString.getStyleRanges());
            }
        }
    }
    
    private static boolean isRemoteRepo(RepositoryPlugin repository) {
        List<URL> locations = Collections.emptyList();
        if (repository instanceof IndexProvider) {
            try {
                locations = ((IndexProvider) repository).getIndexLocations();
            } catch (Exception e) {
                Plugin.getDefault().getLogger().logError("Unable to get repository index list", e);
            }
        }
        
        for (URL location : locations) {
            String protocol = location.getProtocol();
            if ("http".equals(protocol) || "https".equals(protocol))
                return true;
        }
        
        return false;
    }

    @Override
    public void dispose() {
        super.dispose();
        localRepoImg.dispose();
        remoteRepoImg.dispose();
        bundleImg.dispose();
    }

    public Image getImage(Object element) {
        WorkspaceObrProvider workspace;
        try {
            workspace = Central.getWorkspaceObrProvider();
        } catch (Exception e) {
            workspace = null;
        }

        if (element == workspace) {
            return projectImg;
        } else if (element instanceof RepositoryPlugin) {
            return isRemoteRepo((RepositoryPlugin) element) ? remoteRepoImg : localRepoImg;
        } else if (element instanceof Project) {
            return projectImg;
        } else if (element instanceof ProjectBundle) {
            return bundleImg;
        } else if (element instanceof RepositoryBundle) {
            return bundleImg;
        }
        return null;
    }

    public String getText(Object element) {
        if (element instanceof RepositoryPlugin) {
            return ((RepositoryPlugin) element).getName();
        } else if (element instanceof Project) {
            Project project = (Project) element;
            return project.getName();
        } else if (element instanceof ProjectBundle) {
            return ((ProjectBundle) element).getBsn();
        } else if (element instanceof RepositoryBundle) {
            RepositoryBundle bundle = (RepositoryBundle) element;
            return bundle.getBsn();
        } else if (element instanceof RepositoryBundleVersion) {
            RepositoryBundleVersion bundleVersion = (RepositoryBundleVersion) element;
            return bundleVersion.getVersion().toString();
        }
        return null;
    }
}
