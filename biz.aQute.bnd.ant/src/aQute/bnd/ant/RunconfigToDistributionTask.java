package aQute.bnd.ant;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.apache.tools.ant.BuildException;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.io.IO;

public class RunconfigToDistributionTask extends BaseTask {

	private File				rootDir;
	private File				buildProject;
	private String				outputDir;
	private File				bndFile;
	private boolean				allowSnapshots;
	private Map<String, Jar>	snapshots;

	@Override
	public void execute() throws BuildException {
		try {
			createReleaseDir();
			BndEditModel model = new BndEditModel();
			model.loadFrom(bndFile);
			Project bndProject = new Project(new Workspace(rootDir), buildProject, bndFile);
			List<RepositoryPlugin> repositories = bndProject.getPlugins(RepositoryPlugin.class);
			if (allowSnapshots) {
				snapshots = indexBundleSnapshots();
			}

			for (VersionedClause runBundle : model.getRunBundles()) {

				String bsn = runBundle.getName();
				if (bsn.endsWith(".jar")) {
					bsn = bsn.substring(0, bsn.indexOf(".jar"));
				}
				if (allowSnapshots && snapshots.containsKey(bsn)) {
					Jar jar = snapshots.get(bsn);
					jar.write(new File(outputDir, jar.getName() + "-" + jar.getVersion() + ".jar"));
				} else {
					Version version = null;
					File foundJar = null;

					for (RepositoryPlugin repo : repositories) {
						SortedSet<Version> versions = repo.versions(bsn);
						for (Version availableVersion : versions) {
							VersionRange range = null;

							if (runBundle.getVersionRange() != null && !runBundle.getVersionRange()
								.equals(Constants.VERSION_ATTR_LATEST)) {
								range = new VersionRange(runBundle.getVersionRange());
							}

							boolean rangeMatches = range == null || range.includes(availableVersion);
							boolean availableMatches = version == null || availableVersion.compareTo(version) > 0;

							if (rangeMatches && availableMatches) {
								version = availableVersion;
								foundJar = repo.get(bsn, version, null);
							}
						}
					}

					if (foundJar != null) {
						File outputFile = new File(outputDir, foundJar.getName());
						Files.copy(foundJar.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					} else {
						log(bsn + " could not be found in any repository");
					}
				}
			}

			bndProject.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new BuildException(e);
		}
	}

	private File createReleaseDir() {
		File releaseDir = new File(outputDir);
		try {
			IO.deleteWithException(releaseDir);
			log("Deleted directory " + outputDir);
		} catch (IOException e1) {
			// ignore
		}

		try {
			IO.mkdirs(releaseDir);
			log("Created directory " + outputDir);
		} catch (IOException e) {
			throw new BuildException("Output directory '" + outputDir + "' could not be created", e);
		}

		return releaseDir;
	}

	private Map<String, Jar> indexBundleSnapshots() {
		Map<String, Jar> snapshots = new HashMap<>();
		File[] projectFolders = rootDir.listFiles(new NonTestProjectFileFilter());
		for (File projectFolder : projectFolders) {
			File[] generatedFiles = new File(projectFolder, "generated").listFiles(new JarFileFilter());
			for (File generatedFile : generatedFiles) {
				Jar jar;
				try {
					jar = new Jar(generatedFile);
					snapshots.put(jar.getBsn(), jar);
				} catch (Exception e) {
					log("Error creating a bundle from " + generatedFile.getAbsolutePath());
					e.printStackTrace();
				}
			}
		}

		log("Indexed " + snapshots.size() + " snapshots");
		return snapshots;
	}

	private static class NonTestProjectFileFilter implements FileFilter {
		public NonTestProjectFileFilter() {}

		@Override
		public boolean accept(File projectFolder) {
			return !projectFolder.getName()
				.endsWith(".test") && containsGeneratedFolder(projectFolder);
		}

		private boolean containsGeneratedFolder(File projectFolder) {
			if (projectFolder.isDirectory()) {
				List<File> files = Arrays.asList(projectFolder.listFiles());
				for (File file : files) {
					if (file.isDirectory() && file.getName()
						.equals("generated")) {
						return true;
					}
				}
			}

			return false;
		}
	}

	private static class JarFileFilter implements FileFilter {
		public JarFileFilter() {}

		@Override
		public boolean accept(File file) {
			return file.getName()
				.endsWith(".jar");
		}
	}

	public void setRootDir(File rootDir) {
		this.rootDir = rootDir;
	}

	public void setBuildProject(File buildProject) {
		this.buildProject = buildProject;
	}

	public void setOutputDir(String outputDir) {
		this.outputDir = outputDir;
	}

	public void setBndFile(File bndFile) {
		this.bndFile = bndFile;
	}

	public void setAllowSnapshots(boolean allowSnapshots) {
		this.allowSnapshots = allowSnapshots;
	}

}
