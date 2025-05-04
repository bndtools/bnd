package org.bndtools.core.editors.actions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Container;
import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.Project;
import aQute.bnd.build.RepoCollector;
import aQute.bnd.build.Workspace;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.MultiReleaseNamespace;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.service.RepositoryPlugin;
import biz.aQute.resolve.Bndrun;

class BndRunAnalyzer {

	public record Result(Map<String, Collection<String>> bsnsPerRepo) {

		public String print() {
			String output = "Showing unreferenced Bundles. That means, 'Bundle Symbolic Names' (BSN) over all repositories, which are not referenced in -runfw, -runbundles, -buildpath, -testpath, -includeresource with ${repo;...}): \n"
				+ "You can use that to cleanup your repositories and remove unused bundles.\n\n";

			for (String repoName : bsnsPerRepo.keySet()) {

				Collection<String> collection = bsnsPerRepo.get(repoName);

				if (collection != null && !collection.isEmpty()) {

					output += repoName + " (" + collection.size() + ")" + ":\n\n";
					output += bsnsPerRepo.get(repoName)
						.stream()
						.collect(Collectors.joining("\n"));

					output += "\n\n";
				}
			}
			return output;
		}

	}

	public BndRunAnalyzer() {}

	public Result analyze(List<RepositoryPlugin> repos, Collection<Container> runBundles) {

		Set<String> projectAndRunbundlesBsns = new TreeSet<>();

		try {
			collect(projectAndRunbundlesBsns, runBundles);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}

		return new Result(bsnsPerRepo(projectAndRunbundlesBsns, repos));
	}

	public static List<Container> collectReferences(Workspace ws, List<File> bndRuns) {
		Collection<Container> projectRefs = collectReferences(ws.getAllProjects());
		Collection<Container> runBundles = runBndRunReferences(ws, bndRuns);
		List<Container> combined = Stream.of(projectRefs, runBundles)
			.flatMap(Collection::stream)
			.toList();
		return combined;
	}

	private Map<String, Collection<String>> bsnsPerRepo(Set<String> projectAndRunbundlesBsns,
		List<RepositoryPlugin> repos) {
		Map<String, Collection<String>> repoToBsnsMap = new LinkedHashMap<>();

		for (RepositoryPlugin repoPlugin : repos) {

			Set<String> repoBsns = new TreeSet<>();

			if (repoPlugin instanceof Repository osgiRepo) {

				Set<Requirement> wildcard = Collections.singleton(ResourceUtils.createWildcardRequirement());
				Map<Requirement, Collection<Capability>> providers = osgiRepo.findProviders(wildcard);

				for (Entry<Requirement, Collection<Capability>> providersEntry : providers.entrySet()) {

					for (Capability providerCap : providersEntry.getValue()) {

						Resource resource = providerCap.getResource();
						List<Capability> capabilities = resource
							.getCapabilities(MultiReleaseNamespace.MULTI_RELEASE_NAMESPACE);

						if (capabilities.size() > 0) {
							// skip synthetic / multirelease jar
							// resources
							continue;
						}

						String bsn = aQute.bnd.osgi.resource.ResourceUtils.getIdentity(resource);
						aQute.bnd.version.Version version = aQute.bnd.osgi.resource.ResourceUtils.getVersion(resource);

						repoBsns.add(bsn);
					}

				}

				projectAndRunbundlesBsns.forEach(bsn -> {
					repoBsns.remove(bsn);
				});

				repoToBsnsMap.computeIfAbsent(repoPlugin.getName(), (String name) -> new LinkedHashSet<>())
					.addAll(repoBsns);

			}

		}
		return repoToBsnsMap;
	}

	private void collect(Set<String> projectAndRunbundlesBsns, Collection<Container> containers) throws Exception {

		for (Container c : containers) {
			String bsn = bsnFromContainer(c);
			projectAndRunbundlesBsns.add(bsn);
		}
	}

	private String bsnFromContainer(Container c) throws Exception {
		String bsn = c.getBundleSymbolicName();

		if (isMavenGAV(bsn)) {

			Manifest manifest = c.getManifest();

			if (manifest != null) {

				String tmp = manifest.getMainAttributes()
					.getValue(Constants.BUNDLE_SYMBOLICNAME);

				if (tmp != null) {
					bsn = tmp;
				}
			}
		}
		return bsn;
	}

	private boolean isMavenGAV(String bsn) {
		return bsn != null && bsn.indexOf(':') != -1;
	}

	private static Collection<Container> collectReferences(Collection<Project> projects) {

		final Collection<Container> result = new LinkedHashSet<>();

		projects.stream()
			.forEach(project -> {

				try {

					result.addAll(project.getBuildpath());
					result.addAll(project.getTestpath());
					result.addAll(project.getClasspath());
					result.addAll(collectRepoReferences(project));
					result.addAll(project.getSubProjects()
						.stream()
						.map(sp -> {

							try {
								return collectRepoReferences(sp);
							} catch (IOException e) {
								throw Exceptions.duck(e);
							}
						})
						.flatMap(Collection::stream)
						.toList());

				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			});

		return result;
	}

	private static List<Container> collectRepoReferences(Processor project) throws IOException {

		try (RepoCollector repoCollector = new RepoCollector(project)) {

			Collection<Container> repoRefs = repoCollector.repoRefs();
			// only consider type=REPO because we
			// are
			// interested in bundles added via
			// '-includeresource:
			// ${repo;bsn;latest}'
			List<Container> filtered = repoRefs.stream()
				.filter(repoRef -> repoRef != null && TYPE.REPO == repoRef.getType())
				.toList();
			return filtered;
		}
	}

	/**
	 * @return containers for -runbundles, -runfw
	 */
	private static Set<Container> runBndRunReferences(Workspace ws, List<File> bndRuns) {
		return bndRuns.stream()
			.map(file -> {

				try {
					Bndrun run = Bndrun.createBndrun(ws, file);
					Collection<Container> runbundles = run.getRunbundles();
					Collection<Container> runFw = run.getRunFw();

					ArrayList<Container> combined = new ArrayList<>();
					combined.addAll(runbundles);
					combined.addAll(runFw);
					return combined;
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			})
			.flatMap(Collection::stream)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

}
