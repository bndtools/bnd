package bndtools.views.bundlegraph;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;

import aQute.bnd.build.Project;
import aQute.bnd.version.Version;
import bndtools.central.Central;
import bndtools.views.bundlegraph.model.BundleEdge;
import bndtools.views.bundlegraph.model.BundleGraphModel;
import bndtools.views.bundlegraph.model.BundleNode;
import bndtools.views.bundlegraph.model.SimpleBundleGraphModel;

/**
 * Builds a {@link BundleGraphModel} from a list of Eclipse {@link IProject} resources. One node is created per BSN
 * found in each project. Dependency edges are derived from the {@code Import-Package} / {@code Export-Package} headers
 * in the generated JAR manifests.
 */
public class ProjectUniverseProvider {


	/**
	 * Creates a {@link BundleGraphModel} for the given list of Eclipse projects.
	 *
	 * @param projects the Eclipse projects
	 * @return a BundleGraphModel (never null)
	 */
	public BundleGraphModel createModel(List<IProject> projects) {
		Set<BundleNode> nodes = new HashSet<>();
		Map<BundleNode, File> nodeToJar = new HashMap<>();

		for (IProject eclipseProject : projects) {
			try {
				Project bndProject = Central.getProject(eclipseProject);
				if (bndProject == null) {
					continue;
				}
				Map<String, Version> versions = bndProject.getVersions();
				if (versions == null || versions.isEmpty()) {
					BundleNode node = new BundleNode(eclipseProject.getName(), "", eclipseProject.getName());
					nodes.add(node);
					// Try default JAR name
					try {
						File jar = bndProject.getOutputFile(eclipseProject.getName());
						nodeToJar.put(node, jar);
					} catch (Exception ignored) {
						// Output file lookup failed (e.g., project not yet built); proceed without JAR
					}				} else {
					for (Map.Entry<String, Version> entry : versions.entrySet()) {
						String bsn = entry.getKey();
						String version = entry.getValue() != null ? entry.getValue()
							.toString() : "";
						BundleNode node = new BundleNode(bsn, version, eclipseProject.getName());
						nodes.add(node);
						try {
							File jar = bndProject.getOutputFile(bsn, version);
							nodeToJar.put(node, jar);
						} catch (Exception ignored) {
							// Output file lookup failed (e.g., project not yet built); proceed without JAR
						}					}
				}
			} catch (Exception e) {
				// If we can't get project info, add a node with the project name as BSN
				nodes.add(new BundleNode(eclipseProject.getName(), "", eclipseProject.getName()));
			}
		}

		Set<BundleEdge> edges = ManifestDependencyCalculator.calculateEdges(nodeToJar);
		return new SimpleBundleGraphModel(nodes, edges, nodeToJar);
	}
}
