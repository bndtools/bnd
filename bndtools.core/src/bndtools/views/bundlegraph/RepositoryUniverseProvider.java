package bndtools.views.bundlegraph;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;

import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import bndtools.views.bundlegraph.model.BundleEdge;
import bndtools.views.bundlegraph.model.BundleGraphModel;
import bndtools.views.bundlegraph.model.BundleNode;
import bndtools.views.bundlegraph.model.SimpleBundleGraphModel;

/**
 * Builds a {@link BundleGraphModel} from a list of bnd {@link RepositoryPlugin}s. One node is created for the latest
 * version of every BSN found in the given repositories. Dependency edges are derived from the
 * {@code Import-Package}/{@code Export-Package} headers in each bundle's JAR manifest.
 */
public class RepositoryUniverseProvider {

	private static final ILogger logger = Logger.getLogger(RepositoryUniverseProvider.class);


	/**
	 * Creates a {@link BundleGraphModel} from the bundles present in the given
	 * repositories.
	 * <p>
	 * Each BSN+version is used. The JAR file is fetched from the repository and
	 * its manifest is analysed to derive dependency edges. Each edge records
	 * whether it is optional via
	 * {@link bndtools.views.bundlegraph.model.BundleEdge#optional()}.
	 *
	 * @param repos the repositories to query (must not be null)
	 * @return a BundleGraphModel (never null)
	 */
	public BundleGraphModel createModel(List<RepositoryPlugin> repos) {
		Set<BundleNode> nodes = new HashSet<>();
		Map<BundleNode, File> nodeToJar = new HashMap<>();

		for (RepositoryPlugin repo : repos) {
			try {
				List<String> bsns = repo.list(null);
				if (bsns == null) {
					continue;
				}
				for (String bsn : bsns) {
					try {
						SortedSet<Version> versions = repo.versions(bsn);
						if (versions == null || versions.isEmpty()) {
							continue;
						}

						for (Version v : versions) {
							BundleNode node = new BundleNode(bsn, v.toString(), repo.getName());
							if (nodes.add(node)) {
								// Download / locate the JAR file for manifest
								// analysis
								File jar = repo.get(bsn, v, Collections.emptyMap());
								if (jar != null) {
									nodeToJar.put(node, jar);
								}
							}
						}


					} catch (Exception e) {
						logger.logWarning("Failed to load bundle " + bsn + " from repository " + repo.getName(), e);
					}
				}
			} catch (Exception e) {
				logger.logWarning("Failed to query repository " + repo.getName(), e);
			}
		}

		Set<BundleEdge> edges = ManifestDependencyCalculator.calculateEdges(nodeToJar);
		return new SimpleBundleGraphModel(nodes, edges, nodeToJar);
	}
}
