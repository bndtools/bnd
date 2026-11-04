package bndtools.editor.project;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.osgi.Constants;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.ResolutionPhase;
import bndtools.central.Central;
import bndtools.central.RepositoriesViewRefresher;
import bndtools.central.RepositoryUtils;
import bndtools.editor.common.BndEditorPart;
import bndtools.jface.util.FilteredTree;
import bndtools.jface.util.TreeFilter;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryFeature;
import bndtools.model.repo.RepositoryTreeLabelProvider;
import bndtools.model.repo.SearchableRepositoryTreeContentProvider;
import bndtools.utils.SelectionDragAdapter;

public class AvailableBundlesPart extends BndEditorPart implements RepositoriesViewRefresher.RefreshModel {

	private final SearchableRepositoryTreeContentProvider	contentProvider		= new SearchableRepositoryTreeContentProvider(
		ResolutionPhase.runtime);
	private TreeViewer							viewer;

	private Set<String>							includedRepos;

	private final ViewerFilter					includedRepoFilter	= new ViewerFilter() {
																		@Override
																		public boolean select(Viewer viewer,
																			Object parentElement, Object element) {
																			boolean select = false;
																			if (element instanceof RepositoryBundle) {
																				RepositoryBundle repoBundle = (RepositoryBundle) element;
																				RepositoryPlugin repo = repoBundle
																					.getRepo();

																				if (includedRepos == null) {
																					select = true;
																				} else if (repo instanceof WorkspaceRepository) {
																					select = includedRepos
																						.contains("Workspace");
																				} else {
																					select = includedRepos
																						.contains(repoBundle.getRepo()
																							.getName());
																				}
																			} else if (element instanceof RepositoryFeature) {
																				// Check RepositoryFeature since both RepositoryBundle and RepositoryFeature extend RepositoryEntry
																				RepositoryFeature repoFeature = (RepositoryFeature) element;
																				RepositoryPlugin repo = repoFeature
																					.getRepo();

																				if (includedRepos == null) {
																					select = true;
																				} else if (repo instanceof WorkspaceRepository) {
																					select = includedRepos
																						.contains("Workspace");
																				} else {
																					select = includedRepos
																						.contains(repoFeature.getRepo()
																							.getName());
																				}
																			} else {
																				select = true;
																			}
																			return select;
																		}
																	};
	private RepositoryTreeLabelProvider labelProvider;

	public AvailableBundlesPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		Section section = getSection();
		createClient(section, toolkit);
	}

	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);
		Central.addRepositoriesViewer(viewer, this);
	}

	private void createClient(Section section, FormToolkit toolkit) {
		section.setText("Browse Repos");

		// Create contents
		Composite container = toolkit.createComposite(section);
		section.setClient(container);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		container.setLayout(layout);

		contentProvider.setShowRepos(false);

		FilteredTree ftree = new FilteredTree(container, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL, new TreeFilter() {
			@Override
			public boolean isElementSelectable(Object element) {
				return element instanceof RepositoryBundle || element instanceof RepositoryFeature
					|| element instanceof Project;
			}

			@Override
			public boolean isElementVisible(Viewer viewer, Object element) {
				if (element instanceof RepositoryBundle || element instanceof RepositoryFeature) {
					return super.isLeafMatch(viewer, element);
				}
				return true;
			}

		},
			true, true);
		ftree.setExpand(false);
		viewer = ftree.getViewer();
		viewer.setContentProvider(contentProvider);
		labelProvider = new RepositoryTreeLabelProvider(true);
		viewer.setLabelProvider(labelProvider);
		viewer.addFilter(includedRepoFilter);
		viewer.addDragSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] {
			LocalSelectionTransfer.getTransfer()
		}, new SelectionDragAdapter(viewer) {
			@Override
			public void dragStart(DragSourceEvent event) {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				if (!selection.isEmpty()) {
					LocalSelectionTransfer.getTransfer()
						.setSelection(selection);
					LocalSelectionTransfer.getTransfer()
						.setSelectionSetTime(event.time & 0xFFFFFFFFL);
				} else {
					event.doit = false;
				}
			}
		});
	}

	@Override
	protected String[] getProperties() {
		return new String[] {
			Constants.RUNREPOS, BndEditModel.PROP_WORKSPACE
		};
	}

	@Override
	protected void refreshFromModel() {
		viewer.setInput(getRepositories());
	}

	@Override
	public List<RepositoryPlugin> getRepositories() {
		List<String> tmp = model.getRunRepos();
		includedRepos = (tmp == null) ? null : new HashSet<>(tmp);
		Workspace workspace = model.getWorkspace();

		if (workspace == null) {
			return Collections.emptyList();
		}
		try {
			return RepositoryUtils.listRepositories(workspace, true);
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	@Override
	protected void commitToModel(boolean onSave) {
		// Nothing to do
	}

	@Override
	public void dispose() {
		Central.removeRepositoriesViewer(viewer);
		super.dispose();
	}

}
