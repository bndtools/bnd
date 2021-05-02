package aQute.bnd.build;

import static aQute.bnd.classindex.ClassIndexerAnalyzer.BND_HASHES;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Workspace.ResourceRepositoryStrategy;
import aQute.bnd.classindex.ClassIndexerAnalyzer;
import aQute.bnd.osgi.BundleId;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.resource.RequirementBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.result.Result;
import aQute.bnd.version.Version;
import aQute.lib.collections.MultiMap;
import aQute.bnd.exceptions.Exceptions;
import aQute.lib.hierarchy.Hierarchy;
import aQute.lib.hierarchy.NamedNode;
import aQute.lib.zip.JarIndex;

class WorkspaceClassIndex implements AutoCloseable {
	final Workspace workspace;

	WorkspaceClassIndex(Workspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * Search for the bundles that export the given partialFqn. A partialFqn is
	 * either a package name, package prefix or a full FQN class name.
	 * <p>
	 * The result is a map that has the full class name (FQN) as the key and a
	 * list of bundle ids as value.
	 * <p>
	 * This method uses a heuristic to split the FQN into its package and class
	 * portion - the first element that starts with a capital letter is taken to
	 * be the top-level class - everything after that is nested classes,
	 * everything before that is the package hierarchy. This method is pretty
	 * good for most cases, but not perfect. If your calling context has a more
	 * reliable way to split the FQN into the package and class name portions,
	 * you will get more accurate results by using the
	 * {@link #search(String, String)} method.
	 *
	 * @param partialFqn package and/or class name
	 * @return a multimap of fqn|pack->bundleid
	 * @see #search(String, String)
	 */
	Result<Map<String, List<BundleId>>, String> search(String partialFqn) throws Exception {

		Result<String[], String> determine = Descriptors.determine(partialFqn);
		if (determine.isErr())
			return determine.map(x -> Collections.emptyMap());

		String[] parts = determine.unwrap();
		String packageName = parts[0];
		String className = parts[1];

		return search(packageName, className);
	}

	/**
	 * Search for the bundles that export the given class from the specified
	 * package.
	 * <p>
	 * The result is a map that has the full class name (FQN) as the key and a
	 * list of bundle ids as value.
	 *
	 * @param packageName the package in which to search for the matching class.
	 * @param className the name of the class to search for.
	 * @return a multimap of fqn|pack->bundleid
	 * @see #search(String)
	 */
	public Result<Map<String, List<BundleId>>, String> search(String packageName, String className) throws Exception {

		String filter = createFilter(packageName, className);

		Map<Resource, List<Capability>> index = getMatchingResources(filter);

		MultiMap<BundleId, String> result = new MultiMap<>();

		nextResource: for (Entry<Resource, List<Capability>> e : index.entrySet()) {

			Resource resource = e.getKey();
			BundleId bundle = ResourceUtils.getBundleId(resource);
			if (bundle == null)
				continue nextResource;

			if (packageName != null && className == null) {
				addLongestMatchingPackagePrefix(packageName, e.getValue(), bundle, result);
				continue nextResource;
			}

			assert className != null : "we handle pack !class, class || package class left";

			String binaryClassPath = Descriptors.classToPath(className);

			String error = matchClassNameAgainstResource(binaryClassPath, e.getValue(), bundle, result);
			if (error != null) {
				return Result.err(error);
			}
		}
		return Result.ok(result.transpose(true));
	}

	/*
	 * we have a set of package capabilities and a class name. These caps were
	 * found via the hashes or the package prefix. We try to find the class name
	 * in the package directory of the resource
	 */
	private String matchClassNameAgainstResource(String binaryClassName, List<Capability> caps, BundleId bundle,
		MultiMap<BundleId, String> result) {
		try {

			Result<File, String> r = workspace.getBundle(bundle.getBsn(), Version.valueOf(bundle.getVersion()), null);
			if (r.isErr()) {
				return r.error()
					.get();
			}

			Hierarchy zipIndex = new JarIndex(r.unwrap());

			caps: for (Capability cap : caps) {

				String foundPackage = (String) cap.getAttributes()
					.get(PackageNamespace.PACKAGE_NAMESPACE);
				if (foundPackage == null)
					continue caps;

				String[] foundPath = foundPackage.split("\\.");

				zipIndex.findFolder(foundPath)
					.ifPresent(folder -> {

						NamedNode clazz = folder.get(binaryClassName)
							.orElse(null);
						if (clazz != null) {
							String path = clazz.path();
							String fqn = Descriptors.binaryClassToFQN(path);
							result.add(bundle, fqn);
							return;
						}

					});
			}
		} catch (IOException e1) {
			return Exceptions.causes(e1);
		}
		return null;
	}

	/*
	 * We used a wildcard for the package name, we've got a number of potential
	 * packages. We will look for the longest package
	 */
	private void addLongestMatchingPackagePrefix(String packageName, List<Capability> caps, BundleId bundle,
		MultiMap<BundleId, String> result) {
		caps.stream()
			.map(cap -> cap.getAttributes()
				.get(PackageNamespace.PACKAGE_NAMESPACE))
			.filter(Objects::nonNull)
			.map(String.class::cast)
			.filter(s -> s.startsWith(packageName))
			.sorted((a, b) -> Integer.compare(a.length(), b.length()))
			.findFirst()
			.ifPresent(s -> {
				result.add(bundle, s);
			});
	}

	/*
	 * Find the resources in the workspace repo that match the filter.
	 */
	private Map<Resource, List<Capability>> getMatchingResources(String filter) throws Exception {
		RequirementBuilder rb = new RequirementBuilder(PackageNamespace.PACKAGE_NAMESPACE);
		rb.filter(filter);
		Requirement requirement = rb.buildSyntheticRequirement();

		Repository repository = workspace.getResourceRepository(ResourceRepositoryStrategy.ALL);
		Collection<Capability> caps = repository.findProviders(Collections.singleton(requirement))
			.get(requirement);
		Map<Resource, List<Capability>> index = ResourceUtils.getIndexedByResource(caps);
		return index;
	}

	/*
	 * Create a filter. <pre> package -> prefix match on package name class ->
	 * on hashes package class -> on exact package name </pre>
	 */
	private String createFilter(String packageName, String className) {
		StringBuilder sb = new StringBuilder();

		assert packageName != null || className != null : "Only 3 case, cannot both be null";

		if (className != null) {
			if (packageName == null) {

				// assert className != null && packageName == null : "class";

				int hash = ClassIndexerAnalyzer.hash(className);
				sb.append('(')
					.append(BND_HASHES)
					.append('=')
					.append(hash)
					.append(')');
			} else {

				// assert packageName != null : "package class";

				//
				// We could actually add the hash to the check, however,
				// as long as the hashes are not ubiquitous for XML
				// repos, we should play it safe
				//

				sb.append('(')
					.append(PackageNamespace.PACKAGE_NAMESPACE)
					.append('=')
					.append(packageName)
					.append(')');
			}
		} else {

			assert packageName != null && className == null : "lonely package, use wildcard, could be prefix";

			sb.append('(')
				.append(PackageNamespace.PACKAGE_NAMESPACE)
				.append('=')
				.append(packageName)
				.append('*')
				.append(')'); // not sure if we got the full package
		}
		return sb.toString();
	}

	@Override
	public void close() {}

}
