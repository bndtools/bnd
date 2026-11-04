package bndtools.views.bundlegraph;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Manifest;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import bndtools.views.bundlegraph.model.BundleEdge;
import bndtools.views.bundlegraph.model.BundleNode;

/**
 * Calculates bundle dependency edges by reading {@code Import-Package} and {@code Export-Package} headers from JAR
 * manifests.
 * <p>
 * An edge {@code A → B} is emitted when bundle A imports a package that bundle B exports. Every edge records whether
 * it is <em>optional</em> (all contributing {@code Import-Package} entries carry {@code resolution:=optional}) or
 * <em>mandatory</em> (at least one import is mandatory). Callers can use that flag to filter or style edges at render
 * time without rebuilding the model.
 */
public final class ManifestDependencyCalculator {

	/** The value of the {@code resolution} directive that marks an import as optional. */
	private static final String RESOLUTION_OPTIONAL = "optional";

	private ManifestDependencyCalculator() {}

	/**
	 * Computes dependency edges for a set of bundles. Both mandatory and optional imports are always included; each
	 * resulting {@link BundleEdge} records whether it is optional via {@link BundleEdge#optional()}.
	 *
	 * @param nodeToJar map from {@link BundleNode} to its JAR {@link File}. Missing files are skipped silently.
	 * @return map where {@code (A, {B, C})} means A depends on B and C.
	 */
	public static Map<BundleNode, Set<BundleNode>> calculateDependencies(Map<BundleNode, File> nodeToJar) {
		Map<BundleNode, Set<BundleNode>> result = new LinkedHashMap<>();
		for (BundleEdge edge : calculateEdges(nodeToJar)) {
			result.computeIfAbsent(edge.from(), k -> new LinkedHashSet<>())
				.add(edge.to());
		}
		return result;
	}

	/**
	 * Computes dependency edges as {@link BundleEdge} objects, preserving per-edge optionality information. Both
	 * mandatory and optional imports are always included; use {@link BundleEdge#optional()} to distinguish them at
	 * render time.
	 * <p>
	 * An edge is optional when every {@code Import-Package} entry contributing to that edge carries
	 * {@code resolution:=optional}; if at least one import is mandatory, the edge is mandatory.
	 *
	 * @param nodeToJar map from {@link BundleNode} to its JAR {@link File}. Missing files are skipped silently.
	 * @return set of {@link BundleEdge}s (one per importer–exporter pair).
	 */
	public static Set<BundleEdge> calculateEdges(Map<BundleNode, File> nodeToJar) {
		// Step 1: collect exports (package → set of exporting nodes) and imports with optionality per package.
		// A package may be exported by multiple bundles (split packages, fragments, etc.); we record all of them.
		Map<String, Set<BundleNode>> exportedBy = new HashMap<>();
		// importer → { pkg → isOptional }
		Map<BundleNode, Map<String, Boolean>> importsWithOptional = new LinkedHashMap<>();

		for (Map.Entry<BundleNode, File> entry : nodeToJar.entrySet()) {
			BundleNode node = entry.getKey();
			File jarFile = entry.getValue();
			if (jarFile == null || !jarFile.exists()) {
				continue;
			}
			try (Jar jar = new Jar(jarFile)) {
				Manifest manifest = jar.getManifest();
				if (manifest == null) {
					continue;
				}
				Domain domain = Domain.domain(manifest);

				Parameters exportedPkgs = domain.getExportPackage();
				for (String pkg : exportedPkgs.keySet()) {
					exportedBy.computeIfAbsent(pkg, k -> new LinkedHashSet<>())
						.add(node);
				}

				Parameters importedPkgs = domain.getImportPackage();
				if (!importedPkgs.isEmpty()) {
					Map<String, Boolean> pkgOptional = new LinkedHashMap<>();
					for (Entry<String, Attrs> pkgEntry : importedPkgs.entrySet()) {
						String pkg = pkgEntry.getKey();
						boolean isOptional = RESOLUTION_OPTIONAL
							.equals(pkgEntry.getValue().get(Constants.RESOLUTION_DIRECTIVE));
						pkgOptional.put(pkg, isOptional);
					}
					if (!pkgOptional.isEmpty()) {
						importsWithOptional.put(node, pkgOptional);
					}
				}
			} catch (Exception e) {
				// Skip bundles whose JAR cannot be opened / parsed
			}
		}

		// Step 2: match imports against exports; accumulate per-edge optionality.
		// A single Import-Package entry may match multiple exporters (split packages);
		// we emit an edge to each exporter.
		// (importer, exporter) → list of per-package optional flags
		Map<BundleNode, Map<BundleNode, List<Boolean>>> edgeOptionals = new LinkedHashMap<>();
		// (importer, exporter) → first contributing package name
		Map<BundleNode, Map<BundleNode, String>> edgeFirstPackage = new LinkedHashMap<>();

		for (Map.Entry<BundleNode, Map<String, Boolean>> entry : importsWithOptional.entrySet()) {
			BundleNode importer = entry.getKey();
			for (Map.Entry<String, Boolean> pkgEntry : entry.getValue().entrySet()) {
				String pkg = pkgEntry.getKey();
				boolean isOptional = pkgEntry.getValue();
				for (BundleNode exporter : exportedBy.getOrDefault(pkg, Collections.emptySet())) {
					if (!exporter.equals(importer)) {
						edgeOptionals.computeIfAbsent(importer, k -> new LinkedHashMap<>())
							.computeIfAbsent(exporter, k -> new ArrayList<>())
							.add(isOptional);
						// Record the first package that established this
						// (importer, exporter) edge
						edgeFirstPackage.computeIfAbsent(importer, k -> new LinkedHashMap<>())
							.putIfAbsent(exporter, pkg);
					}
				}
			}
		}

		// Step 3: build BundleEdge set; edge is optional iff ALL contributing imports are optional
		Set<BundleEdge> edges = new LinkedHashSet<>();
		for (Map.Entry<BundleNode, Map<BundleNode, List<Boolean>>> fromEntry : edgeOptionals.entrySet()) {
			BundleNode from = fromEntry.getKey();
			for (Map.Entry<BundleNode, List<Boolean>> toEntry : fromEntry.getValue().entrySet()) {
				BundleNode to = toEntry.getKey();
				List<Boolean> flags = toEntry.getValue();
				boolean edgeIsOptional = flags.stream()
					.allMatch(b -> b);
				String firstPkg = edgeFirstPackage.getOrDefault(from, Collections.emptyMap())
					.getOrDefault(to, "");
				edges.add(new BundleEdge(from, to, edgeIsOptional, firstPkg));
			}
		}
		return edges;
	}
}
