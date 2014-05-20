package aQute.launcher.plugin;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.*;

import aQute.bnd.build.*;
import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.launcher.constants.*;
import aQute.launcher.pre.*;
import aQute.libg.cryptography.*;

public class ProjectLauncherImpl extends ProjectLauncher {
	private static final String	EMBEDDED_LAUNCHER_FQN	= "aQute.launcher.pre.EmbeddedLauncher";
	private static final String	EMBEDDED_LAUNCHER		= "aQute/launcher/pre/EmbeddedLauncher.class";
	private static final String	JPM_LAUNCHER			= "aQute/launcher/pre/JpmLauncher.class";
	private static final String	JPM_LAUNCHER_FQN		= "aQute.launcher.pre.JpmLauncher";
	
	final private Project		project;
	final private File			propertiesFile;
	boolean						prepared;
	
	DatagramSocket 		listenerComms;

	public ProjectLauncherImpl(Project project) throws Exception {
		super(project);
		project.trace("created a aQute launcher plugin");
		this.project = project;
		propertiesFile = File.createTempFile("launch", ".properties", project.getTarget());
		project.trace(MessageFormat.format("launcher plugin using temp launch file {0}",
				propertiesFile.getAbsolutePath()));
		addRunVM("-D" + LauncherConstants.LAUNCHER_PROPERTIES + "=\"" + propertiesFile.getAbsolutePath() + "\"");

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
		if(listenerComms != null) {
			listenerComms.close();
			listenerComms = null;
		}
		prepared = false;
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
		LauncherConstants lc = getConstants(getRunBundles(), false);
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
	private LauncherConstants getConstants(Collection<String> runbundles, boolean exported) throws Exception, FileNotFoundException,
			IOException {
		project.trace("preparing the aQute launcher plugin");

		LauncherConstants lc = new LauncherConstants();
		lc.noreferences = Processor.isTrue(project.getProperty(Constants.RUNNOREFERENCES));
		lc.runProperties = getRunProperties();
		lc.storageDir = getStorageDir();
		lc.keep = isKeep();
		lc.runbundles.addAll(runbundles);
		lc.trace = getTrace();
		lc.timeout = getTimeout();
		lc.services = super.getRunFramework() == SERVICES ? true : false;
		lc.activators.addAll(getActivators());
		lc.name = getProject().getName();
		
		if(!exported && !getNotificationListeners().isEmpty()) {
			if(listenerComms == null) {
				listenerComms = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0));
				new Thread(new Runnable() {
					public void run() {
						DatagramSocket socket = listenerComms;
						DatagramPacket packet = new DatagramPacket(new byte[65536], 65536);
						while(!socket.isClosed()) {
							try {
								socket.receive(packet);
								DataInputStream dai = new DataInputStream(new ByteArrayInputStream(
										packet.getData(), packet.getOffset(), packet.getLength()));
								NotificationType type = NotificationType.values()[dai.readInt()];
								String message = dai.readUTF();
								for(NotificationListener listener : getNotificationListeners()) {
									listener.notify(type, message);
								}
							}
							catch (IOException e) {
							}
						}
					}
				}).start();
			}
			lc.notificationPort = listenerComms.getLocalPort();
		} else {
			lc.notificationPort = -1;
		}

		try {
			// If the workspace contains a newer version of biz.aQute.launcher
			// than the version of bnd(tools) used
			// then this could throw NoSuchMethodError. For now just ignore it.
			Map<String, ? extends Map<String,String>> systemPkgs = getSystemPackages();
			if (systemPkgs != null && !systemPkgs.isEmpty())
				lc.systemPackages = Processor.printClauses(systemPkgs);
		}
		catch (Throwable e) {}

		try {
			// If the workspace contains a newer version of biz.aQute.launcher
			// than the version of bnd(tools) used
			// then this could throw NoSuchMethodError. For now just ignore it.
			String systemCaps = getSystemCapabilities();
			if (systemCaps != null) {
				systemCaps = systemCaps.trim();
				if (systemCaps.length() > 0)
					lc.systemCapabilities = systemCaps;
			}
		}
		catch (Throwable e) {}
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
		
		// TODO use constants in the future
		Parameters packageHeader = OSGiHeader.parseHeader(project.getProperty("-package"));
		boolean useShas = packageHeader.containsKey("jpm");
		project.trace("Useshas %s %s", useShas, packageHeader);

		Jar jar = new Jar(project.getName());
		
		Builder b = new Builder();
		project.addClose(b);
		
		if (!project.getIncludeResource().isEmpty()) {
			b.setIncludeResource(project.getIncludeResource().toString());
			b.setProperty(Constants.RESOURCEONLY, "true");
			b.build();
			if ( b.isOk()) {
				jar.addAll(b.getJar());
			}
			project.getInfo(b);
		}
		
		List<String> runpath = getRunpath();

		Set<String> runpathShas = new LinkedHashSet<String>();
		Set<String> runbundleShas = new LinkedHashSet<String>();
		List<String> classpath = new ArrayList<String>();

		for (String path : runpath) {
			project.trace("embedding runpath %s", path);
			File file = new File(path);
			if (file.isFile()) {
				if (useShas) {
					String sha = SHA1.digest(file).asHex();
					runpathShas.add(sha + ";name=\"" + file.getName() + "\"");
				} else {
					String newPath = "jar/" + file.getName();
					jar.putResource(newPath, new FileResource(file));
					classpath.add(newPath);
				}
			}
		}

		// Copy the bundles to the JAR

		List<String> runbundles = (List<String>) getRunBundles();
		List<String> actualPaths = new ArrayList<String>();

		for (String path : runbundles) {
			project.trace("embedding run bundles %s", path);
			File file = new File(path);
			if (!file.isFile())
				project.error("Invalid entry in -runbundles %s", file);
			else {
				if (useShas) {
					String sha = SHA1.digest(file).asHex();
					runbundleShas.add(sha + ";name=\"" + file.getName() + "\"");
					actualPaths.add("${JPMREPO}/" + sha);
				} else {
					String newPath = "jar/" + file.getName();
					jar.putResource(newPath, new FileResource(file));
					actualPaths.add(newPath);
				}
			}
		}

		LauncherConstants lc = getConstants(actualPaths, true);
		lc.embedded = !useShas;
		lc.storageDir = null; // cannot use local info

		final Properties p = lc.getProperties();

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		p.store(bout, "");
		jar.putResource(LauncherConstants.DEFAULT_LAUNCHER_PROPERTIES, new EmbeddedResource(bout.toByteArray(), 0L));

		Manifest m = new Manifest();
		Attributes main = m.getMainAttributes();

		for (Entry<Object,Object> e : project.getFlattenedProperties().entrySet()) {
			String key = (String) e.getKey();
			if (key.length() > 0 && Character.isUpperCase(key.charAt(0)))
				main.putValue(key, (String) e.getValue());
		}

		Instructions instructions = new Instructions(project.getProperty(Constants.REMOVEHEADERS));
		Collection<Object> result = instructions.select(main.keySet(), false);
		main.keySet().removeAll(result);

		if (useShas) {
			project.trace("Use JPM launcher");
			m.getMainAttributes().putValue("Main-Class", JPM_LAUNCHER_FQN);
			m.getMainAttributes().putValue("JPM-Classpath", Processor.join(runpathShas));
			m.getMainAttributes().putValue("JPM-Runbundles", Processor.join(runbundleShas));
			URLResource jpmLauncher = new URLResource(this.getClass().getResource("/" + JPM_LAUNCHER));
			jar.putResource(JPM_LAUNCHER, jpmLauncher);
		} else {
			project.trace("Use Embedded launcher");
			m.getMainAttributes().putValue("Main-Class", EMBEDDED_LAUNCHER_FQN);
			m.getMainAttributes().putValue(EmbeddedLauncher.EMBEDDED_RUNPATH, Processor.join(classpath));
			URLResource embeddedLauncher = new URLResource(this.getClass().getResource("/" + EMBEDDED_LAUNCHER));
			jar.putResource(EMBEDDED_LAUNCHER, embeddedLauncher);
		}
		if ( project.getProperty(Constants.DIGESTS) != null)
			jar.setDigestAlgorithms(project.getProperty(Constants.DIGESTS).trim().split("\\s*,\\s*"));
		else
			jar.setDigestAlgorithms(new String[]{"SHA-1", "MD-5"});
		jar.setManifest(m);
		return jar;
	}

}
