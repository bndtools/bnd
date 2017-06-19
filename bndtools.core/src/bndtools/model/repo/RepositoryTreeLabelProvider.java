package bndtools.model.repo;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.core.ui.icons.Icons;
import org.bndtools.utils.jface.HyperlinkStyler;
import org.bndtools.utils.repos.RepoUtils;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.build.Project;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.IndexProvider;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.Plugin;

public class RepositoryTreeLabelProvider extends StyledCellLabelProvider implements ILabelProvider {
    private static final ILogger logger = Logger.getLogger(RepositoryTreeLabelProvider.class);

    final Image arrowImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/arrow_down.png").createImage();
    final Image localRepoImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/database.png").createImage();
    final Image remoteRepoImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/database_link.png").createImage();
    final Image bundleImg = Icons.desc("bundle").createImage();
    final Image matchImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/star-small.png").createImage();
    final Image projectImg = PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
    final Image loadingImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/loading_16x16.gif").createImage();

    private final boolean showRepoId;

    public RepositoryTreeLabelProvider(boolean showRepoId) {
        this.showRepoId = showRepoId;
    }

    @Override
    public void update(ViewerCell cell) {
        Object element = cell.getElement();
        int index = cell.getColumnIndex();

        if (element instanceof RepositoryPlugin) {
            if (index == 0) {
                RepositoryPlugin repo = (RepositoryPlugin) element;
                cell.setText(repo.getName());

                Image image;
                if (RepoUtils.isWorkspaceRepo(repo))
                    image = projectImg;
                else if (isRemoteRepo((RepositoryPlugin) element))
                    image = remoteRepoImg;
                else
                    image = localRepoImg;

                cell.setImage(image);
            }
        } else if (element instanceof Project) {
            if (index == 0) {
                @SuppressWarnings("resource")
                Project project = (Project) element;
                StyledString label = new StyledString(project.getName());
                if (showRepoId)
                    label.append("  [Workspace]", StyledString.QUALIFIER_STYLER);

                cell.setText(label.getString());
                cell.setStyleRanges(label.getStyleRanges());
                cell.setImage(projectImg);
            }
        } else if (element instanceof ProjectBundle) {
            if (index == 0) {
                StyledString label = new StyledString(((ProjectBundle) element).getBsn());
                if (showRepoId)
                    label.append("  [Workspace]", StyledString.QUALIFIER_STYLER);

                cell.setText(label.getString());
                cell.setStyleRanges(label.getStyleRanges());
                cell.setImage(bundleImg);
            }
        } else if (element instanceof RepositoryBundle) {
            if (index == 0) {
                RepositoryBundle bundle = (RepositoryBundle) element;
                StyledString label = new StyledString(bundle.getText());
                if (showRepoId)
                    label.append("  [" + bundle.getRepo().getName() + "]", StyledString.QUALIFIER_STYLER);
                cell.setText(label.getString());
                cell.setStyleRanges(label.getStyleRanges());
                cell.setImage(bundleImg);
            }
        } else if (element instanceof RepositoryBundleVersion) {
            if (index == 0) {
                RepositoryBundleVersion bundleVersion = (RepositoryBundleVersion) element;
                String versionText = bundleVersion.getText();

                if (versionText.contains(" \u21E9")) {
                    versionText = versionText.replaceAll(" \u21E9", "");
                    cell.setImage(arrowImg);
                }

                StyledString styledString = new StyledString(versionText, StyledString.COUNTER_STYLER);
                cell.setText(styledString.getString());
                cell.setStyleRanges(styledString.getStyleRanges());
            }
        } else if (element instanceof RepositoryResourceElement) {
            RepositoryResourceElement resourceElem = (RepositoryResourceElement) element;
            StyledString label = new StyledString();
            label.append(resourceElem.getIdentity()).append(" ");
            label.append(resourceElem.getVersionString(), StyledString.COUNTER_STYLER);

            cell.setText(label.getString());
            cell.setStyleRanges(label.getStyleRanges());
            cell.setImage(matchImg);
        } else if (element instanceof ContinueSearchElement) {
            StyledString label = new StyledString("Continue Search on JPM4J.org...", new HyperlinkStyler());
            cell.setText(label.getString());
            cell.setStyleRanges(label.getStyleRanges());
        } else if (element instanceof LoadingContentElement) {
            cell.setText(element.toString());
            cell.setImage(loadingImg);
        } else if (element != null) {
            // Catch-all
            cell.setText(element.toString());
        }
    }

    private static boolean isRemoteRepo(RepositoryPlugin repository) {
        List< ? > locations = Collections.emptyList();
        if (repository instanceof IndexProvider) {
            try {
                locations = ((IndexProvider) repository).getIndexLocations();
            } catch (Exception e) {
                logger.logError("Unable to get repository index list", e);
            }
        }

        for (Object locationObj : locations) {
            try {
                URI location;
                if (locationObj instanceof URI)
                    location = (URI) locationObj;
                else if (locationObj instanceof URL)
                    location = ((URL) locationObj).toURI();
                else
                    return false;
                String protocol = location.getScheme();
                if ("http".equals(protocol) || "https".equals(protocol))
                    return true;
            } catch (URISyntaxException e) {
                return false;
            }
        }

        return false;
    }

    @Override
    public void dispose() {
        super.dispose();
        arrowImg.dispose();
        localRepoImg.dispose();
        remoteRepoImg.dispose();
        bundleImg.dispose();
        matchImg.dispose();
    }

    @Override
    public Image getImage(Object element) {
        Image img = null;
        if (element instanceof RepositoryPlugin) {
            RepositoryPlugin repo = (RepositoryPlugin) element;
            if (RepoUtils.isWorkspaceRepo(repo))
                img = projectImg;
            else
                img = isRemoteRepo(repo) ? remoteRepoImg : localRepoImg;
        } else if (element instanceof Project) {
            img = projectImg;
        } else if (element instanceof ProjectBundle) {
            img = bundleImg;
        } else if (element instanceof RepositoryBundle) {
            img = bundleImg;
        } else if (element instanceof LoadingContentElement) {
            img = loadingImg;
        }

        return img;
    }

    @Override
    public String getText(Object element) {
        try {
            if (element instanceof Actionable) {
                return ((Actionable) element).title();
            } else if (element instanceof RepositoryPlugin) {
                return ((RepositoryPlugin) element).getName();
            } else if (element instanceof Project) {
                Project project = (Project) element;
                return project.getName();
            } else if (element instanceof ProjectBundle) {
                return ((ProjectBundle) element).getBsn();
            }
        } catch (Exception e) {
            // just take the default
        }
        return null;
    }

    /**
     * Return the text to be shown as a tooltip.
     * <p/>
     * TODO allow markdown to be used. Not sure how to create a rich text tooltip though. Would also be nice if we could
     * copy/paste from the tooltip like in the JDT.
     */
    @Override
    public String getToolTipText(Object element) {
        try {
            if (element instanceof Actionable)
                return ((Actionable) element).tooltip();
        } catch (Exception e) {
            // ignore, use default
        }
        return null;
    }
}
