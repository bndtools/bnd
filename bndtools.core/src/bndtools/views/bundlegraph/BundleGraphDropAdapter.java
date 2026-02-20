package bndtools.views.bundlegraph;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.part.ResourceTransfer;

import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleVersion;
import bndtools.views.bundlegraph.model.BundleEdge;
import bndtools.views.bundlegraph.model.BundleGraphModel;
import bndtools.views.bundlegraph.model.BundleNode;
import bndtools.views.bundlegraph.model.SimpleBundleGraphModel;

/**
 * Drop adapter for the Bundle Graph view's <em>Available bundles</em> and <em>Selected input bundles</em> lists.
 * <p>
 * Accepted transfer types:
 * <ul>
 * <li>{@link LocalSelectionTransfer} – dragging resources from Eclipse Package Explorer / Project Explorer, and from
 * the bndtools <em>Repositories</em> view</li>
 * <li>{@link ResourceTransfer} – also used by Eclipse resource drag-and-drop</li>
 * </ul>
 * Supported drop payloads:
 * <ul>
 * <li>{@code .bndrun} files → bundles resolved via {@link BndrunUniverseProvider}</li>
 * <li>{@link IProject} resources → bundles resolved via {@link ProjectUniverseProvider}</li>
 * <li>{@link RepositoryBundleVersion} → exact BSN + version from the Repositories view</li>
 * <li>{@link RepositoryBundle} → BSN with its latest version from the Repositories view</li>
 * <li>{@link RepositoryPlugin} → all bundles in that repository (delegated to
 * {@link RepositoryUniverseProvider})</li>
 * </ul>
 * When dropped on <em>Available bundles</em>, the resolved nodes are merged into the universe. When dropped on
 * <em>Selected input bundles</em>, the resolved nodes are merged into the universe <em>and</em> added to the selected
 * set.
 */
class BundleGraphDropAdapter extends ViewerDropAdapter {

	private static final ILogger	logger	= Logger.getLogger(BundleGraphDropAdapter.class);

	private final BundleGraphView	view;
	private final boolean			addToSelected;

	/**
	 * @param viewer the viewer this adapter is registered on
	 * @param view the host view
	 * @param addToSelected {@code true} if drops should populate the "Selected input bundles" list; {@code false} to
	 *            populate the "Available bundles" universe only
	 */
	BundleGraphDropAdapter(Viewer viewer, BundleGraphView view, boolean addToSelected) {
		super(viewer);
		this.view = view;
		this.addToSelected = addToSelected;
	}

	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		return LocalSelectionTransfer.getTransfer()
			.isSupportedType(transferType)
			|| ResourceTransfer.getInstance()
				.isSupportedType(transferType);
	}

	@Override
	public void dragEnter(DropTargetEvent event) {
		super.dragEnter(event);
		event.detail = DND.DROP_COPY;
	}

	@Override
	public boolean performDrop(Object data) {
		List<IFile> bndrunFiles = new ArrayList<>();
		List<IProject> projects = new ArrayList<>();
		List<RepositoryPlugin> wholeRepos = new ArrayList<>();
		List<RepositoryBundleVersion> repoVersions = new ArrayList<>();
		List<RepositoryBundle> repoBundles = new ArrayList<>();

		// Extract from the transfer data (ResourceTransfer or LocalSelectionTransfer)
		if (data instanceof IResource[]) {
			for (IResource resource : (IResource[]) data) {
				categorize(resource, bndrunFiles, projects);
			}
		} else if (data instanceof IStructuredSelection) {
			extractFromSelection((IStructuredSelection) data, bndrunFiles, projects, wholeRepos, repoVersions,
				repoBundles);
		}

		// Fallback: also check the LocalSelectionTransfer directly (some DnD paths set it there)
		if (bndrunFiles.isEmpty() && projects.isEmpty() && wholeRepos.isEmpty() && repoVersions.isEmpty()
			&& repoBundles.isEmpty()) {
			Object localSel = LocalSelectionTransfer.getTransfer()
				.getSelection();
			if (localSel instanceof IStructuredSelection) {
				extractFromSelection((IStructuredSelection) localSel, bndrunFiles, projects, wholeRepos, repoVersions,
					repoBundles);
			}
		}

		if (bndrunFiles.isEmpty() && projects.isEmpty() && wholeRepos.isEmpty() && repoVersions.isEmpty()
			&& repoBundles.isEmpty()) {
			return false;
		}

		// Accumulate a combined nodeToJar map across all drop sources, then build one model so that
		// mergeIntoUniverse() can recompute cross-source edges over the full merged jar set.
		Map<BundleNode, File> combinedJarMap = new HashMap<>();
		Set<BundleNode> combinedNodes = new LinkedHashSet<>();

		for (IFile bndrunFile : bndrunFiles) {
			BundleGraphModel m = new BndrunUniverseProvider().createModel(bndrunFile);
			if (m != null) {
				combinedNodes.addAll(m.nodes());
				combinedJarMap.putAll(m.nodeToJar());
			}
		}

		if (!projects.isEmpty()) {
			BundleGraphModel m = new ProjectUniverseProvider().createModel(projects);
			if (m != null) {
				combinedNodes.addAll(m.nodes());
				combinedJarMap.putAll(m.nodeToJar());
			}
		}

		// Whole-repository drops: delegate to RepositoryUniverseProvider (downloads all JARs, computes edges)
		if (!wholeRepos.isEmpty()) {
			BundleGraphModel m = new RepositoryUniverseProvider().createModel(wholeRepos);
			combinedNodes.addAll(m.nodes());
			combinedJarMap.putAll(m.nodeToJar());
		}

		// Individual bundle/version drops from the Repositories view
		if (!repoVersions.isEmpty() || !repoBundles.isEmpty()) {
			for (RepositoryBundleVersion rbv : repoVersions) {
				addRepoEntry(rbv.getRepo(), rbv.getBsn(), rbv.getVersion(), combinedNodes, combinedJarMap);
			}
			for (RepositoryBundle rb : repoBundles) {
				try {
					SortedSet<Version> vs = rb.getRepo()
						.versions(rb.getBsn());
					if (vs != null && !vs.isEmpty()) {
						addRepoEntry(rb.getRepo(), rb.getBsn(), vs.last(), combinedNodes, combinedJarMap);
					}
				} catch (Exception e) {
					logger.logWarning("Failed to determine latest version for " + rb.getBsn(), e);
				}
			}
		}

		if (combinedNodes.isEmpty()) {
			return false;
		}

		// Compute edges over the combined jar map; mergeIntoUniverse will recompute again over the full universe
		Set<BundleEdge> combinedEdges = ManifestDependencyCalculator.calculateEdges(combinedJarMap);
		BundleGraphModel combined = new SimpleBundleGraphModel(combinedNodes, combinedEdges, combinedJarMap);

		if (addToSelected) {
			view.addNodesToSelected(combined);
		} else {
			view.mergeIntoUniverse(combined);
		}

		return true;
	}

	/**
	 * Downloads the JAR for the given BSN/version from the repository and registers it in the combined maps.
	 */
	private void addRepoEntry(RepositoryPlugin repo, String bsn, Version version, Set<BundleNode> nodes,
		Map<BundleNode, File> jarMap) {
		BundleNode node = new BundleNode(bsn, version.toString(), "");
		if (nodes.add(node)) {
			try {
				File jar = repo.get(bsn, version, Collections.emptyMap());
				if (jar != null) {
					jarMap.put(node, jar);
				}
			} catch (Exception e) {
				logger.logWarning("Failed to retrieve JAR for " + bsn + " " + version, e);
			}
		}
	}

	private void extractFromSelection(IStructuredSelection sel, List<IFile> bndrunFiles, List<IProject> projects,
		List<RepositoryPlugin> wholeRepos, List<RepositoryBundleVersion> repoVersions,
		List<RepositoryBundle> repoBundles) {
		for (Iterator<?> it = sel.iterator(); it.hasNext();) {
			Object element = it.next();
			// Repository view items – checked before IAdaptable to avoid masking by an IProject adapter
			if (element instanceof RepositoryBundleVersion) {
				repoVersions.add((RepositoryBundleVersion) element);
			} else if (element instanceof RepositoryBundle) {
				repoBundles.add((RepositoryBundle) element);
			} else if (element instanceof RepositoryPlugin) {
				wholeRepos.add((RepositoryPlugin) element);
			} else if (element instanceof IResource) {
				categorize((IResource) element, bndrunFiles, projects);
			} else if (element instanceof IAdaptable) {
				IResource resource = ((IAdaptable) element).getAdapter(IResource.class);
				if (resource != null) {
					categorize(resource, bndrunFiles, projects);
				}
			}
		}
	}

	private void categorize(IResource resource, List<IFile> bndrunFiles, List<IProject> projects) {
		if (resource instanceof IFile) {
			IFile file = (IFile) resource;
			if ("bndrun".equals(file.getFileExtension())) {
				bndrunFiles.add(file);
			}
		} else if (resource instanceof IProject) {
			projects.add((IProject) resource);
		}
	}
}
