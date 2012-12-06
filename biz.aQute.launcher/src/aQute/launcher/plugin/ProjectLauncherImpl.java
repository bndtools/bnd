package aQute.launcher.plugin;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.jar.*;

import aQute.bnd.build.*;
import aQute.bnd.osgi.*;
import aQute.launcher.constants.*;

public class ProjectLauncherImpl extends ProjectLauncher {
	final private Project	project;
	final private File		propertiesFile;
	boolean					prepared;

	public ProjectLauncherImpl(Project project) throws Exception {
		super(project);
		project.trace("created a aQute launcher plugin");
		this.project = project;
		propertiesFile = File.createTempFile("launch", ".properties", project.getTarget());
		project.trace(MessageFormat.format("launcher plugin using temp launch file {0}",
				propertiesFile.getAbsolutePath()));
		addRunVM("-D" + LauncherConstants.LAUNCHER_PROPERTIES + "=" + propertiesFile.getAbsolutePath());

		if (project.getRunProperties().get("noframework") != null) {
			setRunFramework(NONE);
			project.warning("The noframework property in -runproperties is replaced by a project setting: '-runframework: none'");
		}

		super.addDefault(Constants.DEFAULT_LAUNCHER_BSN);
	}

	/**
	 * Cleanup the properties file. Is called after the process terminates.
	 */

	@Override
	public void cleanup() {
		propertiesFile.delete();
		project.trace("Deleted ", propertiesFile.getAbsolutePath());
	}

	@Override
	public String getMainTypeName() {
		return "aQute.launcher.Launcher";
	}

	@Override
	public void update() throws Exception {
		updateFromProject();
		writeProperties();
	}

	@Override
	public int launch() throws Exception {
		prepare();
		return super.launch();
	}

	@Override
	public void prepare() throws Exception {
		if (prepared)
			return;
		prepared = true;
		writeProperties();
	}

	void writeProperties() throws Exception {
		LauncherConstants lc = getConstants(getRunBundles());
		OutputStream out = new FileOutputStream(propertiesFile);
		try {
			lc.getProperties().store(out, "Launching " + project);
		}
		finally {
			out.close();
		}
	}

	/**
	 * @return
	 * @throws Exception
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private LauncherConstants getConstants(Collection<String> runbundles) throws Exception, FileNotFoundException,
			IOException {
		project.trace("preparing the aQute launcher plugin");

		LauncherConstants lc = new LauncherConstants();

		lc.runProperties = getRunProperties();
		lc.storageDir = getStorageDir();
		lc.keep = isKeep();
		lc.runbundles.addAll(runbundles);
		lc.trace = getTrace();
		lc.timeout = getTimeout();
		lc.services = super.getRunFramework() == SERVICES ? true : false;
		lc.activators.addAll(getActivators());
		lc.name = getProject().getName();

		try {
			// If the workspace contains a newer version of biz.aQute.launcher than the version of bnd(tools) used
			// then this could throw NoSuchMethodError. For now just ignore it.
			Map<String, ? extends Map<String,String>> systemPkgs = getSystemPackages();
			if (systemPkgs != null && !systemPkgs.isEmpty())
				lc.systemPackages = Processor.printClauses(systemPkgs);
		} catch (Throwable e) {
		}
		
		try {
			// If the workspace contains a newer version of biz.aQute.launcher than the version of bnd(tools) used
			// then this could throw NoSuchMethodError. For now just ignore it.
			Map<String, ? extends Map<String,String>> systemCaps = getSystemCapabilities();
			if (systemCaps != null && !systemCaps.isEmpty())
				lc.systemCapabilities = Processor.printClauses(systemCaps);
		} catch (Throwable e) {
		}
		return lc;

	}

	/**
	 * Create a standalone executable. All entries on the runpath are rolled out
	 * into the JAR and the runbundles are copied to a directory in the jar. The
	 * launcher will see that it starts in embedded mode and will automatically
	 * detect that it should load the bundles from inside. This is drive by the
	 * launcher.embedded flag.
	 * 
	 * @throws Exception
	 */

	@Override
	public Jar executable() throws Exception {
		Jar jar = new Jar(project.getName());

		// Copy the class path of the launched VM to this bundle
		// but in reverse order so that the order matches the classpath (first
		// wins).
		List<String> runpath = getRunpath();
		Collections.reverse(runpath);

		for (String path : runpath) {
			project.trace("embedding runpath %s", path);
			File file = new File(path);
			if (!file.isFile())
				project.error("Invalid entry on runpath %s", file);
			else {
				Jar from = new Jar(file);
				jar.addAll(from);
			}
		}

		// Copy the bundles to the JAR

		List<String> runbundles = (List<String>) getRunBundles();
		List<String> actualPaths = new ArrayList<String>();

		for (String path : runbundles) {
			project.trace("embedding run bundles %s", path);
			File file = new File(path);
			String newPath = "jar/" + file.getName();
			jar.putResource(newPath, new FileResource(file));
			actualPaths.add(newPath);
		}

		LauncherConstants lc = getConstants(actualPaths);
		lc.embedded = true;
		lc.storageDir = null; // cannot use local info

		final Properties p = lc.getProperties();

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		p.store(bout, "");
		jar.putResource(LauncherConstants.DEFAULT_LAUNCHER_PROPERTIES, new EmbeddedResource(bout.toByteArray(), 0L));

		// Remove signer files, we have a different manifest now
		Set<String> set = new HashSet<String>();
		for (Object pp : jar.getResources().keySet()) {
			String path = (String) pp;
			if (path.matches("META-INF/.*\\.(SF|RSA|DSA)$"))
				set.add(path);
		}

		for (String path : set)
			jar.remove(path);

		// And set the manifest
		Manifest m = new Manifest();
		m.getMainAttributes().putValue("Main-Class", "aQute.launcher.Launcher");
		jar.setManifest(m);
		return jar;
	}

}
