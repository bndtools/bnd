package bndtools.views.bundlegraph;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;

import aQute.bnd.build.Container;
import biz.aQute.resolve.Bndrun;
import bndtools.central.Central;
import bndtools.views.bundlegraph.model.BundleEdge;
import bndtools.views.bundlegraph.model.BundleGraphModel;
import bndtools.views.bundlegraph.model.BundleNode;
import bndtools.views.bundlegraph.model.SimpleBundleGraphModel;

/**
 * Builds a {@link BundleGraphModel} from a {@code .bndrun} file by reading its {@code -runbundles} header.
 * Dependency edges are derived from the {@code Import-Package} / {@code Export-Package} headers in the generated JAR
 * manifests of workspace projects.
 */
public class BndrunUniverseProvider {


	/**
	 * Creates a {@link BundleGraphModel} for the given {@code .bndrun} file.
	 * The model contains one node per {@code -runbundles} entry. Dependency
	 * edges are derived by matching Import-Package / Export-Package from the
	 * generated JARs. Each edge records whether it is optional via
	 * {@link bndtools.views.bundlegraph.model.BundleEdge#optional()}.
	 *
	 * @param bndrunFile the .bndrun IFile
	 * @return a BundleGraphModel (never null)
	 */
	public BundleGraphModel createModel(IFile bndrunFile) {
		try {
			File file = bndrunFile.getLocation()
				.toFile();

			Set<BundleNode> nodes = new HashSet<>();
			Map<BundleNode, File> nodeToJar = new HashMap<>();
			Bndrun bndrun = Bndrun.createBndrun(Central.getWorkspace(), file);

			Collection<Container> runBundles = bndrun.getRunbundles();
			for (Container container : runBundles) {
				String bsn = container.getBundleSymbolicName();
				String version = container.getVersion();

				BundleNode node = new BundleNode(bsn, version, "");
				nodes.add(node);
				File jar = container.getFile();
				if (jar != null) {
					nodeToJar.put(node, jar);
				}

			}

			Set<BundleEdge> edges = ManifestDependencyCalculator.calculateEdges(nodeToJar);
			return new SimpleBundleGraphModel(nodes, edges, nodeToJar);
		} catch (Exception e) {
			return new SimpleBundleGraphModel(java.util.Collections.emptySet(), java.util.Collections.emptySet(),
				java.util.Collections.emptyMap());
		}
	}

}
