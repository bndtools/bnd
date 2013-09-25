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
	private boolean         verbose;
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
					Version version = Version.parseVersion(runBundle.getVersion());

					File foundJar = null;
					for (RepositoryPlugin repo : repositories) {
						foundJar = repo.get(bsn, version, null);
						if (foundJar != null) {
							break;
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
						log(bsn + " could not be found in any repository", org.apache.tools.ant.Project.MSG_WARN);
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
		File releaseDir = new File(outputDir);
		boolean deleted = releaseDir.delete();
		if (deleted) {
			log("Deleted directory " + outputDir, getLogLevel());
		}

		boolean created = releaseDir.mkdirs();
		if (created) {
			log("Created directory " + outputDir, getLogLevel());
		} else if (!releaseDir.exists() && releaseDir.isDirectory()) {
			throw new BuildException("Output directory '" + outputDir + "' could not be created");
		}

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
					// Probably not a bundle...
					log("Error creating a bundle from " + generatedFile.getAbsolutePath(), org.apache.tools.ant.Project.MSG_WARN);
					e.printStackTrace();
				}
			}
		}

		log("Indexed " + snapshots.size() + " snapshots", getLogLevel());
		return snapshots;
	}

	private static class NonTestProjectFileFilter implements FileFilter {
		public NonTestProjectFileFilter() {}

		public boolean accept(File projectFolder) {
			return !projectFolder.getName().endsWith(".test") && containsGeneratedFolder(projectFolder);
		}

		private boolean containsGeneratedFolder(File projectFolder) {
			if (!projectFolder.isDirectory()) {
				return false;
			}
			return new File(projectFolder, "generated").isDirectory();
		}
	}

	private static class JarFileFilter implements FileFilter {
		public JarFileFilter() {}

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

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	private int getLogLevel() {
		return this.verbose ? org.apache.tools.ant.Project.MSG_INFO : org.apache.tools.ant.Project.MSG_VERBOSE;
	}
}
