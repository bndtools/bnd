package aQute.bnd.maven;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.maven.GetDependencyPom;
import aQute.bnd.service.maven.PomDependency;
import aQute.bnd.service.maven.PomDependency.PomRevision;
import aQute.bnd.util.dto.DTO;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;

public class PomDependencyFinder {
	private static final String VERSION = "version";

	public Collection<PomDependency> getMavenDependencies(Manifest manifest, Processor processor) throws Exception {
		final Domain domain = Domain.domain(manifest);
		final String bsn = domain.getBundleSymbolicName()
			.getKey();
		final Workspace workspace = this.getWorkspace(processor);
		if (workspace == null)
			return null;
		final Project project = workspace.getProject(bsn);
		if (project == null)
			return null;
		return this.getMavenDependenciesByBuildpath(bsn, workspace, project);
	}

	/**
	 * First the MATA-INF/.../pom.xml file will be checked then the maven
	 * repository will be checked if the bundle originated from there the maven
	 * information of the repository will be used. If the bundle has no pom.xml
	 * the properties of the bnd project and the MANIFEST.MF will be checked. If
	 * no properties are set a groupId/artifactId will be generated based on the
	 * default bnd configurations.
	 * 
	 * @param bundle
	 * @return
	 * @throws Exception
	 */
	private PomDependency toMavenResource(Container bundle) throws Exception {
		try (Jar binary = new Jar(bundle.getFile())) {
			PomDependency pomDependency = getDependencyFromFile(binary);
			if (pomDependency != null) {
				return pomDependency;
			}

			final Manifest manifest = this.getManifest(binary);
			if (manifest != null) {
				try (Processor projectProcessor = new Processor(bundle.getProject())) {
					final BundleRevision bundleRev = this.createBundleRevision(bundle, projectProcessor, manifest);
					pomDependency = this.getDependencyFromRepositories(bundle, bundleRev);
					if (pomDependency != null) {
						return pomDependency;
					}
					manifest.getMainAttributes()
						.putValue(Constants.BUNDLE_SYMBOLICNAME, bundleRev.bsn);
					manifest.getMainAttributes()
						.putValue(Constants.BUNDLE_VERSION, bundleRev.version.toString());
					try (PomResource pom = new PomResource(projectProcessor, manifest)) {
						PomRevision pomRev = createPomRevision(pom);
						PomDependency pomDep = new PomDependency();
						pomDep.revision = pomRev;
						return pomDep;
					}
				}
			}
		}
		return null;
	}

	private Manifest getManifest(Jar jar) throws Exception {
		for (final Map.Entry<String, aQute.bnd.osgi.Resource> e : jar.getResources()
			.entrySet()) {
			final String path = e.getKey();

			if (path.equals("META-INF/MANIFEST.MF")) {
				try (final InputStream in = e.getValue()
					.openInputStream()) {
					return new Manifest(in);
				}
			}
		}
		return null;
	}

	private aQute.bnd.osgi.Resource getPomResource(Jar jar) {
		for (final Map.Entry<String, aQute.bnd.osgi.Resource> e : jar.getResources()
			.entrySet()) {
			final String path = e.getKey();

			if (path.startsWith("META-INF/maven/") && path.endsWith("/pom.xml")) {
				return e.getValue();
			}
		}
		return null;
	}

	private Collection<PomDependency> getMavenDependenciesByBuildpath(String bsn, Workspace workspace, Project project)
		throws Exception {
		final Set<PomDependency> requiredResources = new HashSet<>();
		final Collection<Container> buildpath = project.getBuildpath();
		for (final Container dep : buildpath) {
			final PomDependency mavenRes = this.toMavenResource(dep);
			if (mavenRes == null) {
				continue;
			}
			requiredResources.add(mavenRes);
		}
		return requiredResources;
	}

	private PomDependency getDependencyFromRepositories(Container bundle, BundleRevision bundleRev) throws Exception {
		final List<RepositoryPlugin> repos = bundle.getProject()
			.getWorkspace()
			.getRepositories();
		return getDependencyFromRepositories(bundleRev, repos);
	}

	private PomDependency getDependencyFromRepositories(BundleRevision bundle, List<RepositoryPlugin> repos)
		throws Exception {
		for (final RepositoryPlugin repo : repos) {
			if (repo instanceof GetDependencyPom) {
				final GetDependencyPom pomRepo = (GetDependencyPom) repo;
				final PomDependency pomDep = pomRepo.getPomDependency(bundle.bsn, bundle.version);
				if (pomDep != null) {
					return pomDep;
				}
			}
		}
		return null;
	}

	private PomDependency getDependencyFromFile(Jar binary) throws Exception {
		final Resource pomFile = this.getPomResource(binary);
		if (pomFile == null)
			return null;
		final Path tmpPomFile = Files.createTempFile("tmp", ".pom");
		try (InputStream in = pomFile.openInputStream(); OutputStream out = Files.newOutputStream(tmpPomFile)) {
			IO.copy(in, out);
			final Domain pomDomain = Domain.domain(tmpPomFile.toFile());
			final PomRevision pomRevision = createPomRevision(pomDomain);
			if (pomRevision != null) {
				final PomDependency pomDependency = new PomDependency();
				pomDependency.revision = pomRevision;
				return pomDependency;
			}
		} finally {
			Files.delete(tmpPomFile);
		}
		return null;
	}

	private PomRevision createPomRevision(PomResource pomResource) {
		PomRevision pomRev = new PomRevision();
		pomRev.artifact = pomResource.getArtifactId();
		pomRev.group = pomResource.getGroupId();
		pomRev.version = MavenVersion.parseMavenString(pomResource.getVersion());
		if (pomRev.group == null || pomRev.artifact == null || pomRev.version == null)
			return null;
		return pomRev;
	}

	private PomRevision createPomRevision(Domain pomDomain) {
		PomRevision pomRev = new PomRevision();
		pomRev.group = pomDomain.get("pom.groupId");
		if (pomRev.group != null) {
			pomRev.group = pomRev.group.trim();
			if (pomRev.group.isEmpty()) {
				pomRev.group = null;
			}
		}
		pomRev.artifact = pomDomain.get("pom.artifactId");
		if (pomRev.artifact != null) {
			pomRev.artifact = pomRev.artifact.trim();
			if (pomRev.artifact.isEmpty()) {
				pomRev.artifact = null;
			}
		}
		String pVersion = pomDomain.get("pom.version");
		if (pVersion != null) {
			pVersion = pVersion.trim();
			pomRev.version = MavenVersion.parseMavenString(pVersion);
		}
		if (pomRev.group == null || pomRev.artifact == null || pomRev.version == null)
			return null;
		return pomRev;
	}

	/**
	 * Creates a BundleRevision DTO object based on the properties of the passed
	 * variables
	 * 
	 * @param bundle
	 * @param projectProcessor
	 * @param manifest
	 * @return
	 * @throws Exception
	 */
	private BundleRevision createBundleRevision(Container bundle, Processor projectProcessor, Manifest manifest)
		throws Exception {
		Domain manifestDomain = Domain.domain(manifest);
		String bsn;
		if (manifestDomain.getBundleSymbolicName() == null) {
			bsn = bundle.getBundleSymbolicName();
			// there are cases that the bsn has a suffix
			if (bsn.endsWith(".jar")) {
				bsn = bsn.substring(0, bsn.length() - 4);
			}
		} else {
			bsn = manifestDomain.getBundleSymbolicName()
				.getKey();
		}
		String version = projectProcessor.get(VERSION);
		if (version == null)
			version = manifestDomain.getBundleVersion();
		if (version == null)
			version = "0";
		BundleRevision bundleRev = new BundleRevision();
		bundleRev.bsn = bsn;
		bundleRev.version = Version.parseVersion(version);
		return bundleRev;
	}

	/**
	 * Iterate processor tree to find the workspace
	 */
	private Workspace getWorkspace(Processor processor) {
		while (processor != null) {
			if (processor instanceof Workspace) {
				return (Workspace) processor;
			} else if (processor instanceof Project) {
				return ((Project) processor).getWorkspace();
			}
			processor = processor.getParent();
		}
		return null;
	}

	private static class BundleRevision extends DTO {
		private String	bsn;
		private Version	version;
	}
}
