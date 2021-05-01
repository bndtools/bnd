package bndtools.model.repo;

import org.bndtools.core.ui.icons.Icons;
import org.bndtools.core.ui.icons.Icons.IconBuilder;
import org.bndtools.utils.jface.HyperlinkStyler;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;

import aQute.bnd.build.Project;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.exceptions.Exceptions;

public class RepositoryTreeLabelProvider extends StyledCellLabelProvider {

	final Image				arrowImg	= Icons.image("arrow_down");
	final Image				bundleImg	= Icons.image("bundle");
	final Image				matchImg	= Icons.image("match");
	final Image				projectImg	= Icons.image("project");
	final Image				loadingImg	= Icons.image("loading");

	private final boolean	showRepoId;

	public RepositoryTreeLabelProvider(boolean showRepoId) {
		this.showRepoId = showRepoId;
	}

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		int index = cell.getColumnIndex();
		StyledString label = new StyledString();
		Image image = null;
		try {
			if (element instanceof RepositoryPlugin) {
				if (index == 0) {
					RepositoryPlugin repo = (RepositoryPlugin) element;
					String status = repo.getStatus();
					String name = null;
					if (repo instanceof Actionable) {
						name = ((Actionable) repo).title();
					}
					if (name == null)
						name = repo.getName();
					label.append(name);

					IconBuilder ib = Icons.builder(repo.getIcon());
					if (repo.canWrite()) {
						ib.bottomLeft("writable_decorator");
					}
					if (repo.isRemote()) {
						ib.bottomRight("remote_decorator");
					}

					if (status != null) {
						ib.topLeft("error_decorator");
						label.append(" : ");
						label.append(status, StyledString.QUALIFIER_STYLER);
					}
					image = ib.build();
				}
			} else if (element instanceof Project) {
				if (index == 0) {
					@SuppressWarnings("resource")
					Project project = (Project) element;
					boolean isOk = project.isOk();

					label.append(project.getName());

					if (showRepoId) {
						label.append(" ");
						label.append("[Workspace]", StyledString.QUALIFIER_STYLER);
					}

					if (!isOk) {
						label.append(" ");
						label.append("Errors: " + project.getErrors()
							.size(), StyledString.COUNTER_STYLER);
					}

					image = projectImg;
				}
			} else if (element instanceof ProjectBundle) {
				if (index == 0) {
					ProjectBundle projectBundle = (ProjectBundle) element;

					label.append(projectBundle.getBsn());
					if (showRepoId) {
						label.append(" ");
						if (projectBundle.isSub()) {
							label.append("[Workspace:" + projectBundle.getProject() + "]",
								StyledString.QUALIFIER_STYLER);
						} else {
							label.append("[Workspace]", StyledString.QUALIFIER_STYLER);
						}
					}
					image = bundleImg;
				}
			} else if (element instanceof RepositoryBundle) {
				if (index == 0) {
					RepositoryBundle bundle = (RepositoryBundle) element;
					label.append(bundle.getText());
					if (showRepoId) {
						label.append(" ");
						label.append("[" + bundle.getRepo()
							.getName() + "]", StyledString.QUALIFIER_STYLER);
					}
					image = bundleImg;
				}
			} else if (element instanceof RepositoryBundleVersion) {
				if (index == 0) {
					RepositoryBundleVersion bundleVersion = (RepositoryBundleVersion) element;
					String versionText = bundleVersion.getText();

					if (versionText.contains(" \u21E9")) {
						versionText = versionText.replaceAll(" \u21E9", "");
						image = arrowImg;
					}
					label.append(versionText, StyledString.COUNTER_STYLER);
				}
			} else if (element instanceof RepositoryResourceElement) {
				RepositoryResourceElement resourceElem = (RepositoryResourceElement) element;

				label.append(resourceElem.getIdentity())
					.append(" ");
				label.append(resourceElem.getVersionString(), StyledString.COUNTER_STYLER);

				image = matchImg;
			} else if (element instanceof ContinueSearchElement) {
				label.append("Continue Search on repository...", new HyperlinkStyler());
				image = null;
			} else if (element instanceof LoadingContentElement) {
				label.append(element.toString());
				image = loadingImg;
			} else if (element != null) {
				label.append(element.toString());
			}
		} catch (Exception e) {
			label.append("error: " + Exceptions.causes(e));
			image = Icons.image("error");
		}

		cell.setText(label.getString());
		cell.setStyleRanges(label.getStyleRanges());
		cell.setImage(image);
	}

	/**
	 * Return the text to be shown as a tooltip.
	 * <p/>
	 * TODO allow markdown to be used. Not sure how to create a rich text
	 * tooltip though. Would also be nice if we could copy/paste from the
	 * tooltip like in the JDT.
	 */
	@Override
	public String getToolTipText(Object element) {
		try {
			if (element instanceof Actionable) {
				return ((Actionable) element).tooltip();
			}
		} catch (Exception e) {
			// ignore, use default
		}
		return null;
	}
}
