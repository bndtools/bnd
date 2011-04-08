package aQute.launcher.plugin;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.jar.*;

import aQute.bnd.build.*;
import aQute.launcher.constants.*;
import aQute.lib.io.*;
import aQute.lib.osgi.*;
import aQute.libg.generics.*;
import static aQute.lib.osgi.Constants.*;

public class ProjectLauncherImpl extends ProjectLauncher {
	final private Project	project;
	final private File		propertiesFile;
	boolean					prepared;

	public ProjectLauncherImpl(Project project) throws Exception {
		super(project);
		project.trace("created a aQute launcher plugin");
		this.project = project;
		propertiesFile = File.createTempFile("launch", ".properties", project.getTarget());
		project.trace(MessageFormat.format("launcher plugin using temp launch file {0}", propertiesFile.getAbsolutePath()));
		addRunVM("-D" + LauncherConstants.LAUNCHER_PROPERTIES + "="
				+ propertiesFile.getAbsolutePath());

		if (project.getRunProperties().get("noframework") != null) {
			setRunFramework(NONE);
			project.warning("The noframework property in -runproperties is replaced by a project setting: '-runframework: none'");
		}

		super.addDefault(Constants.DEFAULT_LAUNCHER_BSN);
	}

	public String getMainTypeName() {
		return "aQute.launcher.Launcher";
	}

	public void update() throws Exception {
		updateFromProject();
		writeProperties();
	}
	
	
	public int launch() throws Exception {
		prepare();
		return super.launch();
	}

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
		} finally {
			out.close();
		}
	}

	/**
	 * @return 
	 * @throws Exception
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private LauncherConstants getConstants(Collection<String> runbundles) throws Exception,
			FileNotFoundException, IOException {
		project.trace("preparing the aQute launcher plugin");

		LauncherConstants lc = new LauncherConstants();

		lc.runProperties = getRunProperties();
		lc.storageDir = getStorageDir();
		lc.keep = isKeep();
		lc.runbundles.addAll(getRunBundles());
		lc.trace = getTrace();
		lc.timeout = getTimeout();
		lc.services = super.getRunFramework() == SERVICES ? true : false;
		lc.activators.addAll(getActivators());

		if (!getSystemPackages().isEmpty()) {
			lc.systemPackages = Processor.printClauses(getSystemPackages(), null);
		}
		return lc;
		
	}

	/**
	 * Create a standalone executable
	 * @throws Exception 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */

	public Jar executable() throws Exception {
		Jar jar = new Jar(project.getName());
		
		Collection<String> runbundles = copyBundles(jar, getRunBundles());
		LauncherConstants lc = getConstants(runbundles);
		final Properties p = lc.getProperties();
		p.setProperty(RUNBUNDLES, Processor.join(runbundles,", \\\n  ") );

		jar.putResource("descriptor.properties", new WriteResource() {
			@Override public void write(OutputStream outStream) throws IOException, Exception {
				p.store(outStream, "comment");
			}
			@Override public long lastModified() {
				return 0L;
			}
		});

		List<String> paths = Create.list();
		paths.addAll( getRunpath());
		paths.add( project.getBundle("biz.aQute.launcher", null, STRATEGY_HIGHEST, null).getFile().getAbsolutePath());
		
		for ( String path : paths) {
			File f = IO.getFile(project.getBase(), path);
			Jar roll = new Jar(f);
			jar.addAll(roll);
		}
		
		Manifest m = new Manifest();
		m.getMainAttributes().putValue("Main-Class", "aQute.launcher.Launcher");
		jar.setManifest(m);
		return jar;
	}

	/**
	 * @param jar
	 */
	private Collection<String> copyBundles(Jar jar, Collection<String> runbundles) {
		List<String> list = Create.list();
		
		for (String s : runbundles) {
			File f = IO.getFile(new File("").getAbsoluteFile(), s);
			if (!f.isFile()) {
				project.error("In exec, cannot find runbundle %s for project %s", f, project);
			} else {
				String path = "jar/" + f.getName();
				list.add(path);
			}
		}
		return list;
	}

}
