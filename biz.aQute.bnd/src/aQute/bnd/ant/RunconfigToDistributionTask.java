package aQute.bnd.ant;

import java.io.*;
import java.nio.channels.*;
import java.util.*;

import org.apache.tools.ant.*;

import aQute.bnd.build.*;
import aQute.bnd.build.Project;
import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.bnd.version.*;

public class RunconfigToDistributionTask extends Task {

	private File			rootDir;
	private File			buildProject;
	private String			outputDir;
	private File			bndFile;
	private boolean			allowSnapshots;
	private Map<String,Jar>	snapshots;

	@Override
	public void execute() throws BuildException {
		try {
			createReleaseDir();
			Project bndProject = new Project(new Workspace(rootDir), buildProject, bndFile);
			List<RepositoryPlugin> repositories = bndProject.getPlugins(RepositoryPlugin.class);
			if (allowSnapshots) {
				snapshots = indexBundleSnapshots();
			}

			for (Container runBundle : bndProject.getRunbundles()) {
				String bsn = runBundle.getBundleSymbolicName();
				if (bsn.endsWith(".jar")) {
					bsn = bsn.substring(0, bsn.indexOf(".jar"));
				}
				if (allowSnapshots && snapshots.containsKey(bsn)) {
					Jar jar = snapshots.get(bsn);
					jar.write(new File(outputDir, runBundle.getFile().getName()));
				} else {
					Version version = null;
					File foundJar = null;

					for (RepositoryPlugin repo : repositories) {
						SortedSet<Version> versions = repo.versions(bsn);
						if (!versions.isEmpty()) {
							Version foundVersion = versions.last();
							if (version == null || foundVersion.compareTo(version) == 1) {
								version = foundVersion;
								foundJar = repo.get(bsn, version, null);
							}
						}
					}

					if (foundJar != null) {
						File outputFile = new File(outputDir, foundJar.getName());
						FileChannel source = null;
						FileChannel destination = null;

						try {
							source = new FileInputStream(foundJar).getChannel();
							destination = new FileOutputStream(outputFile).getChannel();
							destination.transferFrom(source, 0, source.size());
						}
						finally {
							if (source != null) {
								source.close();
							}

							if (destination != null) {
								destination.close();
							}
						}
					} else {
						log(bsn + " could not be found in any repository");
					}
				}
			}

			bndProject.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private File createReleaseDir() {
		log("Deleting directory " + outputDir);
		File releaseDir = new File(outputDir);
		releaseDir.delete();
		releaseDir.mkdir();
		log("Created directory " + outputDir);
		return releaseDir;
	}

	private Map<String,Jar> indexBundleSnapshots() {
		Map<String,Jar> snapshots = new HashMap<String,Jar>();
		File[] projectFolders = rootDir.listFiles(new NonTestProjectFileFilter());
		for (File projectFolder : projectFolders) {
			File[] generatedFiles = new File(projectFolder, "generated").listFiles(new JarFileFilter());
			for (File generatedFile : generatedFiles) {
				Jar jar;
				try {
					jar = new Jar(generatedFile);
					snapshots.put(jar.getBsn(), jar);
				}
				catch (Exception e) {
					log("Error creating a bundle from " + generatedFile.getAbsolutePath());
					e.printStackTrace();
				}
			}
		}

		log("Indexed " + snapshots.size() + " snapshots");
		return snapshots;
	}

	class NonTestProjectFileFilter implements FileFilter {
		public boolean accept(File projectFolder) {
			return !projectFolder.getName().endsWith(".test") && containsGeneratedFolder(projectFolder);
		}

		private boolean containsGeneratedFolder(File projectFolder) {
			if (projectFolder.isDirectory()) {
				List<File> files = Arrays.asList(projectFolder.listFiles());
				for (File file : files) {
					if (file.isDirectory() && file.getName().equals("generated")) {
						return true;
					}
				}
			}

			return false;
		}
	}

	class JarFileFilter implements FileFilter {
		public boolean accept(File file) {
			return file.getName().endsWith(".jar");
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
