package bndtools.model.repo;

import org.bndtools.core.ui.icons.Icons;
import org.bndtools.core.ui.icons.Icons.IconBuilder;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;

import aQute.bnd.build.Project;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.tags.Tags;

public class RepositoryTreeLabelProvider extends StyledCellLabelProvider
	implements org.eclipse.jface.viewers.ILabelProvider {

	final Image				arrowImg	= Icons.image("arrow_down");
	final Image				bundleImg	= Icons.image("bundle");
	final Image				matchImg	= Icons.image("match");
	final Image				projectImg	= Icons.image("project");
	final Image				loadingImg	= Icons.image("loading");
	final Image				featureImg	= Icons.image("feature");
	final Image				folderImg	= Icons.image("fldr_obj");

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

					Tags tags = repo.getTags();
					if (!tags.isEmpty()) {
						label.append(" " + Tags.print(tags), StyledString.QUALIFIER_STYLER);
					}

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
			} else if (element instanceof RepositoryFeature) {
				// Check RepositoryFeature BEFORE RepositoryBundle since both extend RepositoryEntry
				if (index == 0) {
					RepositoryFeature feature = (RepositoryFeature) element;
					label.append(feature.getText());
					if (showRepoId) {
						label.append(" ");
						label.append("[" + feature.getRepo()
							.getName() + "]", StyledString.QUALIFIER_STYLER);
					}
					image = featureImg;
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

			} else if (element instanceof RepositoryFeature) {
				if (index == 0) {
					RepositoryFeature feature = (RepositoryFeature) element;
					label.append(feature.getText());
					if (showRepoId) {
						label.append(" ");
						label.append("[" + feature.getRepo()
							.getName() + "]", StyledString.QUALIFIER_STYLER);
					}
					image = featureImg;
				}
			} else if (element instanceof FeatureFolderNode) {
				if (index == 0) {
					FeatureFolderNode folder = (FeatureFolderNode) element;
					label.append(folder.getLabel());
					image = folderImg;
				}
			} else if (element instanceof IncludedFeatureItem) {
				if (index == 0) {
					IncludedFeatureItem item = (IncludedFeatureItem) element;
					label.append(item.getText());
					image = featureImg;
				}
			} else if (element instanceof RequiredFeatureItem) {
				if (index == 0) {
					RequiredFeatureItem item = (RequiredFeatureItem) element;
					label.append(item.getText());
					image = featureImg;
				}
			} else if (element instanceof IncludedBundleItem) {
				if (index == 0) {
					IncludedBundleItem item = (IncludedBundleItem) element;
					label.append(item.getText());
					image = bundleImg;
				}
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

	@Override
	public Image getImage(Object element) {
		return arrowImg;
	}

	/**
	 *
	 */
	@Override
	public String getText(Object element) {
		if (element instanceof RepositoryPlugin rp)
			return rp.getName();

		if (element instanceof Project pr)
			return pr.getName();

		if (element instanceof ProjectBundle pb)
			return pb.getBsn();

		if (element instanceof RepositoryBundle rb)
			return rb.getText();

		if (element instanceof RepositoryBundleVersion rbv)
			return rbv.getText();

		if (element instanceof RepositoryResourceElement re)
			return re.getIdentity();

		if (element instanceof RepositoryFeature rf)
			return rf.getText();

		if (element instanceof FeatureFolderNode ffn)
			return ffn.getLabel();

		if (element instanceof IncludedFeatureItem ifi)
			return ifi.getText();

		if (element instanceof RequiredFeatureItem rfi)
			return rfi.getText();

		if (element instanceof IncludedBundleItem ibi)
			return ibi.getText();

		return element.toString();
	}
}
