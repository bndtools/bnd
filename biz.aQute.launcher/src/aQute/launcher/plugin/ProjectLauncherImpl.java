package aQute.launcher.plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.launcher.constants.LauncherConstants;
import aQute.launcher.pre.EmbeddedLauncher;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.cryptography.SHA1;

public class ProjectLauncherImpl extends ProjectLauncher {
	private final static Logger	logger					= LoggerFactory.getLogger(ProjectLauncherImpl.class);
	private static final String	EMBEDDED_LAUNCHER_FQN	= "aQute.launcher.pre.EmbeddedLauncher";
	private static final String	EMBEDDED_LAUNCHER		= "aQute/launcher/pre/EmbeddedLauncher.class";
	private static final String	JPM_LAUNCHER			= "aQute/launcher/pre/JpmLauncher.class";
	private static final String	JPM_LAUNCHER_FQN		= "aQute.launcher.pre.JpmLauncher";

	final private File			launchPropertiesFile;
	boolean						prepared;

	DatagramSocket				listenerComms;

	public ProjectLauncherImpl(Project project) throws Exception {
		super(project);
		logger.debug("created a aQute launcher plugin");
		launchPropertiesFile = File.createTempFile("launch", ".properties", project.getTarget());
		logger.debug("launcher plugin using temp launch file {}", launchPropertiesFile.getAbsolutePath());
		addRunVM("-D" + LauncherConstants.LAUNCHER_PROPERTIES + "=\"" + launchPropertiesFile.getAbsolutePath() + "\"");

		if (project.getRunProperties().get("noframework") != null) {
			setRunFramework(NONE);
			project.warning(
					"The noframework property in -runproperties is replaced by a project setting: '-runframework: none'");
		}

		super.addDefault(Constants.DEFAULT_LAUNCHER_BSN);
	}

	//
	// Initialize the main class for a local launch start
	//

	protected int invoke(Class< ? > main, String args[]) throws Exception {
		LauncherConstants lc = getConstants(getRunBundles(), false);

		Method mainMethod = main.getMethod("main", args.getClass(), Properties.class);
		Object o = mainMethod.invoke(null, args, lc.getProperties(new UTF8Properties()));
		if (o == null)
			return 0;

		return (Integer) o;
	}

	/**
	 * Cleanup the properties file. Is called after the process terminates.
	 */

	@Override
	public void cleanup() {
		launchPropertiesFile.delete();
		if (listenerComms != null) {
			listenerComms.close();
			listenerComms = null;
		}
		prepared = false;
		logger.debug("Deleted {}", launchPropertiesFile.getAbsolutePath());
		super.cleanup();
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
		try (OutputStream out = Files.newOutputStream(launchPropertiesFile.toPath())) {
			lc.getProperties(new UTF8Properties()).store(out, "Launching " + getProject());
		}
	}

	/**
	 * @throws Exception
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private LauncherConstants getConstants(Collection<String> runbundles, boolean exported)
			throws Exception, FileNotFoundException, IOException {
		logger.debug("preparing the aQute launcher plugin");

		LauncherConstants lc = new LauncherConstants();
		lc.noreferences = getProject().is(Constants.RUNNOREFERENCES);
		lc.runProperties = getRunProperties();
		lc.storageDir = getStorageDir();
		lc.keep = isKeep();
		lc.runbundles.addAll(runbundles);
		lc.trace = getTrace();
		lc.timeout = getTimeout();
		lc.services = super.getRunFramework() == SERVICES ? true : false;
		lc.activators.addAll(getActivators());
		lc.name = getProject().getName();

		if (!exported && !getNotificationListeners().isEmpty()) {
			if (listenerComms == null) {
				listenerComms = new DatagramSocket(new InetSocketAddress(InetAddress.getByName(null), 0));
				new Thread(new Runnable() {
					public void run() {
						DatagramSocket socket = listenerComms;
						DatagramPacket packet = new DatagramPacket(new byte[65536], 65536);
						while (!socket.isClosed()) {
							try {
								socket.receive(packet);
								DataInputStream dai = new DataInputStream(new ByteArrayInputStream(packet.getData(),
										packet.getOffset(), packet.getLength()));
								NotificationType type = NotificationType.values()[dai.readInt()];
								String message = dai.readUTF();
								for (NotificationListener listener : getNotificationListeners()) {
									listener.notify(type, message);
								}
							} catch (IOException e) {}
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
		} catch (Throwable e) {}

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
		} catch (Throwable e) {}
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
		Parameters packageHeader = OSGiHeader.parseHeader(getProject().getProperty("-package"));
		boolean useShas = packageHeader.containsKey("jpm");
		logger.debug("useShas {} {}", useShas, packageHeader);

		Jar jar = new Jar(getProject().getName());

		Builder b = new Builder();
		getProject().addClose(b);

		if (!getProject().getIncludeResource().isEmpty()) {
			b.setIncludeResource(getProject().getIncludeResource().toString());
			b.setProperty(Constants.RESOURCEONLY, "true");
			b.build();
			if (b.isOk()) {
				jar.addAll(b.getJar());
			}
			getProject().getInfo(b);
		}

		List<String> runpath = getRunpath();

		Set<String> runpathShas = new LinkedHashSet<String>();
		Set<String> runbundleShas = new LinkedHashSet<String>();
		List<String> classpath = new ArrayList<String>();

		for (String path : runpath) {
			logger.debug("embedding runpath {}", path);
			File file = new File(path);
			if (file.isFile()) {
				if (useShas) {
					String sha = SHA1.digest(file).asHex();
					runpathShas.add(sha + ";name=\"" + file.getName() + "\"");
				} else {
					String newPath = nonCollidingPath(file, jar);
					jar.putResource(newPath, new FileResource(file));
					classpath.add(newPath);
				}
			}
		}

		// Copy the bundles to the JAR

		List<String> runbundles = (List<String>) getRunBundles();
		List<String> actualPaths = new ArrayList<String>();

		for (String path : runbundles) {
			logger.debug("embedding run bundles {}", path);
			File file = new File(path);
			if (!file.isFile())
				getProject().error("Invalid entry in -runbundles %s", file);
			else {
				if (useShas) {
					String sha = SHA1.digest(file).asHex();
					runbundleShas.add(sha + ";name=\"" + file.getName() + "\"");
					actualPaths.add("${JPMREPO}/" + sha);
				} else {
					String newPath = nonCollidingPath(file, jar);
					jar.putResource(newPath, new FileResource(file));
					actualPaths.add(newPath);
				}
			}
		}

		LauncherConstants lc = getConstants(actualPaths, true);
		lc.embedded = !useShas;

		final Properties p = lc.getProperties(new UTF8Properties());

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		p.store(bout, "");
		jar.putResource(LauncherConstants.DEFAULT_LAUNCHER_PROPERTIES, new EmbeddedResource(bout.toByteArray(), 0L));

		Manifest m = new Manifest();
		Attributes main = m.getMainAttributes();

		for (Entry<Object,Object> e : getProject().getFlattenedProperties().entrySet()) {
			String key = (String) e.getKey();
			if (key.length() > 0 && Character.isUpperCase(key.charAt(0)))
				main.putValue(key, (String) e.getValue());
		}

		Instructions instructions = new Instructions(getProject().getProperty(Constants.REMOVEHEADERS));
		Collection<Object> result = instructions.select(main.keySet(), false);
		main.keySet().removeAll(result);

		if (useShas) {
			logger.debug("Use JPM launcher");
			m.getMainAttributes().putValue("Main-Class", JPM_LAUNCHER_FQN);
			m.getMainAttributes().putValue("JPM-Classpath", Processor.join(runpathShas));
			m.getMainAttributes().putValue("JPM-Runbundles", Processor.join(runbundleShas));
			Resource jpmLauncher = Resource.fromURL(this.getClass().getResource("/" + JPM_LAUNCHER));
			jar.putResource(JPM_LAUNCHER, jpmLauncher);
			doStart(jar, JPM_LAUNCHER_FQN);
		} else {
			logger.debug("Use Embedded launcher");
			m.getMainAttributes().putValue("Main-Class", EMBEDDED_LAUNCHER_FQN);
			m.getMainAttributes().putValue(EmbeddedLauncher.EMBEDDED_RUNPATH, Processor.join(classpath));
			Resource embeddedLauncher = Resource.fromURL(this.getClass().getResource("/" + EMBEDDED_LAUNCHER));
			jar.putResource(EMBEDDED_LAUNCHER, embeddedLauncher);
			doStart(jar, EMBEDDED_LAUNCHER_FQN);
		}
		if (getProject().getProperty(Constants.DIGESTS) != null)
			jar.setDigestAlgorithms(getProject().getProperty(Constants.DIGESTS).trim().split("\\s*,\\s*"));
		else
			jar.setDigestAlgorithms(new String[] {
					"SHA-1", "MD-5"
			});
		jar.setManifest(m);
		cleanup();
		return jar;
	}

	String nonCollidingPath(File file, Jar jar) {
		String fileName = file.getName();
		String path = "jar/" + fileName;
		String[] parts = Strings.extension(fileName);
		if (parts == null) {
			parts = new String[] {
					fileName, ""
			};
		}
		int i = 1;
		while (jar.exists(path)) {
			path = String.format("jar/%s[%d].%s", parts[0], i++, parts[1]);
		}
		return path;
	}

	/*
	 * Useful for when exported as folder or unzipped
	 */
	void doStart(Jar jar, String fqn) throws UnsupportedEncodingException {
		String nix = "#!/bin/sh\njava -cp . " + fqn + "\n";
		String pc = "java -cp . " + fqn + "\r\n";
		jar.putResource("start", new EmbeddedResource(nix, 0));
		jar.putResource("start.bat", new EmbeddedResource(pc, 0));
	}

}
