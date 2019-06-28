package bndtools.m2e;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import aQute.bnd.osgi.Domain;
import aQute.bnd.version.Version;
import aQute.lib.collections.SortedList;

public class MavenWorkspaceRepository extends AbstractMavenRepository {

	private final static ILogger					logger	= Logger.getLogger(MavenWorkspaceRepository.class);

	private boolean									inited	= false;

	private final Map<String, IMavenProjectFacade>	bsnMap	= new HashMap<>();

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		return Collections.emptyMap();
	}

	@Override
	public File get(final String bsn, final Version version, Map<String, String> properties,
		final DownloadListener... listeners) throws Exception {
		if (!inited) {
			init();
		}

		final IMavenProjectFacade projectFacade = bsnMap.get(bsn);

		if (projectFacade == null) {
			return null;
		}

		// add the eclipse project that this comes from so we can look it up in
		// the launch
		// see
		// bndtools.launch.BndDependencySourceContainer.createSourceContainers()
		final String projectName = projectFacade.getProject()
			.getName();
		properties.put("sourceProjectName", projectName);

		File bundleFile = guessBundleFile(projectFacade);

		if (bundleFile == null || !bundleFile.exists()) {
			MavenProject mavenProject = getMavenProject(projectFacade, new NullProgressMonitor());
			bundleFile = getBundleFile(mavenProject);
		}

		if (!bundleFile.exists()) {
			for (DownloadListener listener : listeners) {
				try {
					listener.failure(bundleFile, "Could not get bundle file for " + bsn + ":" + version);
				} catch (Throwable t) {
					logger.logError("Download listener error", t);
				}
			}

			return null;
		}

		for (DownloadListener listener : listeners) {
			try {
				listener.success(bundleFile);
			} catch (Throwable t) {
				logger.logError("Download listener error", t);
			}
		}

		return bundleFile;
	}

	private File getBundleFile(final MavenProject mavenProject) {
		String finalName = null;

		// first check maven-jar-plugin config first, if it is empty use
		// project.build.finalName
		Plugin jarPlugin = mavenProject.getPlugin("org.apache.maven.plugins:maven-jar-plugin");

		if (jarPlugin != null) {
			Object config = jarPlugin.getConfiguration();

			if (config instanceof Xpp3Dom) {
				Xpp3Dom dom = (Xpp3Dom) config;

				Xpp3Dom finalNameNode = dom.getChild("finalName");

				if (finalNameNode != null) {
					finalName = finalNameNode.getValue();
				}
			}
		}

		if (finalName == null) {
			finalName = mavenProject.getBuild()
				.getFinalName();
		}

		return new File(mavenProject.getBuild()
			.getDirectory(), finalName + ".jar");
	}

	private String getBsnFromMavenProject(MavenProject mavenProject) throws Exception {
		final File bundleFile = getBundleFile(mavenProject);

		if (bundleFile.exists()) {
			Domain domain = Domain.domain(bundleFile);
			String bsn = domain.getBundleSymbolicName()
				.getKey();
			return bsn;
		}

		return null;
	}

	private void init() {
		inited = true;

		final IProgressMonitor monitor = new NullProgressMonitor();

		for (IMavenProjectFacade projectFacade : mavenProjectRegistry.getProjects()) {
			try {
				String bsnGuess = guessBsnFromProjectFacade(projectFacade);

				if (bsnGuess != null) {
					bsnMap.put(bsnGuess, projectFacade);
				} else {
					MavenProject mavenProject = getMavenProject(projectFacade, monitor);
					String bsn = getBsnFromMavenProject(mavenProject);

					if (bsn != null) {
						bsnMap.put(bsn, projectFacade);
					}
				}
			} catch (Exception e) {
				logger.logError("Unable to get bundle symbolic name for " + projectFacade.getProject()
					.getName(), e);
			}
		}

		mavenProjectRegistry.addMavenProjectChangedListener(this);
	}

	private String guessBsnFromProjectFacade(IMavenProjectFacade projectFacade) throws IOException {
		File bundleFileGuess = guessBundleFile(projectFacade);

		if (bundleFileGuess.exists()) {
			Domain domain = Domain.domain(bundleFileGuess);
			String bsn = domain.getBundleSymbolicName()
				.getKey();

			return bsn;
		}

		return null;
	}

	private MavenProject getMavenProject(final IMavenProjectFacade projectFacade, final IProgressMonitor monitor)
		throws CoreException {
		MavenProject mavenProject = projectFacade.getMavenProject();

		if (mavenProject == null) {
			mavenProject = projectFacade.getMavenProject(monitor);
		}

		return mavenProject;
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		return Collections.emptyList();
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		if (!inited) {
			init();
		}

		Version version = null;

		final IMavenProjectFacade projectFacade = bsnMap.get(bsn);

		if (projectFacade != null) {
			File bundleFile = guessBundleFile(projectFacade);

			if (bundleFile == null || !bundleFile.exists()) {
				MavenProject mavenProject = getMavenProject(projectFacade, new NullProgressMonitor());
				bundleFile = getBundleFile(mavenProject);
			}

			if (bundleFile != null && bundleFile.exists()) {
				Domain domain = Domain.domain(bundleFile);
				version = new Version(domain.getBundleVersion());
			}
		}

		if (version == null) {
			return SortedList.empty();
		}

		return new SortedList<>(version);
	}

	@Override
	public String getName() {
		return "Maven Workspace Repository";
	}

	@Override
	public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
		if (events == null)
			return;

		for (MavenProjectChangedEvent event : events) {
			final IMavenProjectFacade oldProject = event.getOldMavenProject();

			Iterator<Entry<String, IMavenProjectFacade>> entries = bsnMap.entrySet()
				.iterator();

			while (entries.hasNext()) {
				Entry<String, IMavenProjectFacade> entry = entries.next();

				if (entry.getValue()
					.equals(oldProject)) {
					String bsn = entry.getKey();

					bsnMap.remove(bsn);
					break;
				}
			}
		}
	}

}
