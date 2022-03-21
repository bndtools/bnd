package aQute.launcher.plugin;

import java.io.DataInput;
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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.annotation.bundle.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.header.Attrs;
import aQute.bnd.help.instructions.LauncherInstructions.Executable;
import aQute.bnd.help.instructions.LauncherInstructions.RunOption;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Jar.Compression;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.PropertiesResource;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.Verifier;
import aQute.launcher.constants.LauncherConstants;
import aQute.lib.collections.MultiMap;
import aQute.lib.io.ByteBufferDataInput;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.cryptography.SHA1;
import aQute.libg.glob.Glob;

@Header(name = Constants.LAUNCHER_PLUGIN, value = "${@class}")
public class ProjectLauncherImpl extends ProjectLauncher {
	private final static Logger	logger				= LoggerFactory.getLogger(ProjectLauncherImpl.class);
	private static final String	EMBEDDED_RUNPATH	= "Embedded-Runpath";
	private static final String	LAUNCHER_PATH		= "launcher.runpath";
	private static final String	EMBEDDED_LAUNCHER	= "aQute.launcher.pre.EmbeddedLauncher";
	static final String			PRE_JAR				= "biz.aQute.launcher.pre.jar";
	private final Container		container;
	private final List<String>	launcherpath		= new ArrayList<>();

	private File				preTemp;

	final private File			launchPropertiesFile;
	boolean						prepared;
	DatagramSocket				listenerComms;

	public ProjectLauncherImpl(Project project, Container container) throws Exception {
		super(project);
		this.container = container;

		logger.debug("created a aQute launcher plugin");
		launchPropertiesFile = File.createTempFile("launch", ".properties", project.getTarget());
		logger.debug("launcher plugin using temp launch file {}", launchPropertiesFile.getAbsolutePath());
		addRunVM("-D" + LauncherConstants.LAUNCHER_PROPERTIES + "=" + launchPropertiesFile.getAbsolutePath());

		if (project.getRunProperties()
			.get("noframework") != null) {
			setRunFramework(NONE);
			project.warning(
				"The noframework property in -runproperties is replaced by a project setting: '-runframework: none'");
		}
	}

	@Override
	protected void updateFromProject() throws Exception {
		super.updateFromProject();

		File launcher = container.getFile();

		// Pre file will likely be next to launcher in embedded repo
		File pre = new File(launcher.getParentFile(), PRE_JAR);

		if (!pre.isFile()) {
			if (preTemp != null) {
				IO.delete(preTemp);
			}
			preTemp = pre = File.createTempFile("pre", ".jar");
			try (Jar jar = new Jar(launcher)) {
				Resource embeddedPre = jar.getResource(PRE_JAR);
				try (OutputStream out = IO.outputStream(pre)) {
					embeddedPre.write(out);
				}
			}
		}
		launcherpath.clear();
		addClasspath(new Container(getProject(), pre), launcherpath);

		// Make sure the launcher is on the runpath
		addClasspath(container);

	}

	//
	// Initialize the main class for a local launch start
	//

	@Override
	protected int invoke(Class<?> main, String args[]) throws Exception {
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
		IO.delete(launchPropertiesFile);
		if (listenerComms != null) {
			listenerComms.close();
			listenerComms = null;
		}
		if (preTemp != null) {
			IO.delete(preTemp);
			preTemp = null;
		}
		prepared = false;
		logger.debug("Deleted {}", launchPropertiesFile.getAbsolutePath());
		super.cleanup();
	}

	@Override
	public String getMainTypeName() {
		Instructions instructions = new Instructions(getProject().getProperty(Constants.REMOVEHEADERS));
		if (!instructions.isEmpty() && instructions.matches(MAIN_CLASS)) {
			return EMBEDDED_LAUNCHER;
		}
		return getProject().getProperty(MAIN_CLASS, EMBEDDED_LAUNCHER);
	}

	@Override
	public void update() throws Exception {
		super.update();
		writeProperties();
	}

	/**
	 * We override getClasspath to use it just for the embedded launcher.
	 */
	@Override
	public Collection<String> getClasspath() {
		return launcherpath;
	}

	/**
	 * We override getRunVM to use it to add the runpath to the JVM execution as
	 * a system property to be processed by the embedded launcher.
	 */
	@Override
	public Collection<String> getRunVM() {
		List<String> list = new ArrayList<>(super.getRunVM());
		list.add(getRunpath().stream()
			.collect(Strings.joining(",", "-D" + LAUNCHER_PATH + "=", "", "")));
		return list;
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
		super.prepare();

		writeProperties();
	}

	void writeProperties() throws Exception {
		LauncherConstants lc = getConstants(getRunBundles(), false);
		try (OutputStream out = IO.outputStream(launchPropertiesFile)) {
			lc.getProperties(new UTF8Properties())
				.store(out, "Launching " + getProject());
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
		lc.services = getRunFramework() == SERVICES ? true : false;
		lc.activators.addAll(getActivators());
		lc.name = getProject().getName();
		lc.activationEager = launcherInstrs.runoptions()
			.contains(RunOption.eager);
		lc.frameworkRestart = isRunFrameworkRestart();

		if (!exported && !getNotificationListeners().isEmpty()) {
			if (listenerComms == null) {
				listenerComms = new DatagramSocket(new InetSocketAddress(InetAddress.getByName(null), 0));
				new Thread(() -> {
					DatagramSocket socket = listenerComms;
					DatagramPacket packet = new DatagramPacket(new byte[65536], 65536);
					while (!socket.isClosed()) {
						try {
							socket.receive(packet);
							ByteBuffer bb = ByteBuffer.wrap(packet.getData(), packet.getOffset(), packet.getLength());
							DataInput dai = ByteBufferDataInput.wrap(bb);
							NotificationType type = NotificationType.values()[dai.readInt()];
							String message = dai.readUTF();
							if ((type == NotificationType.ERROR) && bb.hasRemaining()) {
								message += "\n" + dai.readUTF();
							}
							for (NotificationListener listener : getNotificationListeners()) {
								listener.notify(type, message);
							}
						} catch (IOException e) {}
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
			Map<String, ? extends Map<String, String>> systemPkgs = getSystemPackages();
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
	 * detect that it should load the bundles from inside. This is driven by the
	 * launcher.embedded flag.
	 */

	@Override
	public Jar executable() throws Exception {
		Executable instrs = launcherInstrs.executable();
		Optional<Compression> rejar = instrs.rejar();
		logger.debug("rejar {}", rejar);
		Map<Glob, List<Glob>> strip = extractStripMapping(instrs.strip());
		logger.debug("strip {}", strip);
		try (Builder builder = new Builder()) {
			Project project = getProject();
			builder.setBase(project.getBase());

			Jar jar = new Jar(project.getName());
			builder.setJar(jar);
			builder.addClasspath(jar);

			// Use properties from the project
			copyProperties(project::getProperty, builder::setProperty,
				// jar properties
				Constants.COMPRESSION, Constants.REPRODUCIBLE, Constants.DIGESTS,
				// jpms properties
				Constants.JPMS_MODULE_INFO, Constants.AUTOMATIC_MODULE_NAME);
			copyProperties(project::mergeProperties, builder::setProperty,
				// include resource properties
				Constants.INCLUDERESOURCE, Constants.INCLUDE_RESOURCE);

			List<String> classpath = new ArrayList<>();

			List<String> runpath = getRunpath();
			for (String path : runpath) {
				logger.debug("embedding runpath {}", path);
				File file = getOriginalFile(path);
				if (file.isFile()) {
					String newPath = nonCollidingPath(file, jar, null);
					jar.putResource(newPath, getJarFileResource(file, rejar, strip));
					classpath.add(newPath);
				}
			}

			// Copy the bundles to the JAR

			List<String> actualPaths = new ArrayList<>();

			Collection<String> runbundles = getRunBundles();
			for (String path : runbundles) {
				logger.debug("embedding run bundles {}", path);
				File file = getOriginalFile(path);
				if (!file.isFile())
					project.error("Invalid entry in -runbundles %s", file);
				else {
					String newPath = nonCollidingPath(file, jar, instrs.location());
					jar.putResource(newPath, getJarFileResource(file, rejar, strip));
					actualPaths.add(newPath);
				}
			}

			LauncherConstants lc = getConstants(actualPaths, true);
			lc.embedded = true;

			jar.putResource(LauncherConstants.DEFAULT_LAUNCHER_PROPERTIES,
				new PropertiesResource(lc.getProperties(new UTF8Properties())));

			Properties flattenedProperties = project.getFlattenedProperties();
			Instructions instructions = new Instructions(project.getProperty(Constants.REMOVEHEADERS));
			Collection<Object> result = instructions.select(flattenedProperties.keySet(), false);
			flattenedProperties.keySet()
				.removeAll(result);

			Manifest manifest = new Manifest();
			Attributes main = manifest.getMainAttributes();
			main.putValue(Constants.MAIN_CLASS, EMBEDDED_LAUNCHER);
			main.putValue(EMBEDDED_RUNPATH, join(classpath));
			for (Entry<Object, Object> e : flattenedProperties.entrySet()) {
				String key = (String) e.getKey();
				String value = (String) e.getValue();
				if (Strings.nonNullOrEmpty(key) && Character.isUpperCase(key.charAt(0))
					&& Strings.nonNullOrEmpty(value)) {
					main.putValue(key, value.trim());
				}
			}

			Resource preJar = Resource.fromURL(this.getClass()
				.getResource("/" + PRE_JAR));
			try (Jar pre = Jar.fromResource("pre", preJar)) {
				jar.addAll(pre, new Instruction("!{META-INF,OSGI-OPT}/*"));
			}

			String embeddedLauncherName = main.getValue(Constants.MAIN_CLASS);
			logger.debug("Use '{}' launcher class", embeddedLauncherName);
			// jpms properties
			builder.setProperty(Constants.MAIN_CLASS, embeddedLauncherName);
			doStart(jar, embeddedLauncherName);

			jar = builder.build();

			// overwrite calculated information
			jar.setName(project.getName());
			jar.setManifest(manifest);

			project.getInfo(builder);

			cleanup();
			builder.removeClose(jar); // detach jar from builder
			return jar;
		}
	}

	private void copyProperties(Function<String, String> getProperty, BiConsumer<String, String> setProperty,
		String... keys) {
		for (String key : keys) {
			String value = getProperty.apply(key);
			if (Strings.nonNullOrEmpty(value)) {
				setProperty.accept(key, value);
			}
		}
	}

	private Map<Glob, List<Glob>> extractStripMapping(List<String> strip) {
		MultiMap<Glob, Glob> map = new MultiMap<>();

		for (String s : strip) {
			int n = s.indexOf(':');
			Glob key = Glob.ALL;
			if (n > 0) {
				key = new Glob(s.substring(0, n));
			}
			Glob value = new Glob(s.substring(n + 1));
			map.add(key, value);
		}
		return map;
	}

	private Resource getJarFileResource(File file, Optional<Compression> compression, Map<Glob, List<Glob>> strip)
		throws IOException {
		if (strip.isEmpty() && !compression.isPresent()) {
			return new FileResource(file);
		}

		Jar jar = new Jar(file);
		jar.setDoNotTouchManifest();

		compression.ifPresent(jar::setCompression);
		logger.debug("compression {}", compression);

		stripContent(strip, jar);

		JarResource resource = new JarResource(jar, true);
		return resource;
	}

	private void stripContent(Map<Glob, List<Glob>> strip, Jar jar) {
		Set<String> remove = new HashSet<>();

		for (Map.Entry<Glob, List<Glob>> e : strip.entrySet()) {
			Glob fileMatch = e.getKey();
			if (!fileMatch.matcher(jar.getName())
				.matches()) {
				continue;
			}

			logger.debug("strip {}", e.getValue());
			List<Glob> value = e.getValue();

			for (String path : jar.getResources()
				.keySet()) {
				if (Glob.in(value, path)) {
					logger.debug("strip {}", path);
					remove.add(path);
				}
			}
		}
		remove.forEach(jar::remove);
		logger.debug("resources {}", Strings.join("\n", jar.getResources()
			.keySet()));
	}

	File getOriginalFile(String path) {
		File file = new File(path);
		if (file.getName()
			.startsWith("+") && file.exists()) { // file has source attached
			File originalFile = new File(file.getParentFile(), file.getName()
				.substring(1));
			if (originalFile.exists()) {
				return originalFile;
			}
		}
		return file;
	}

	String nonCollidingPath(File file, Jar jar, String locationFormat) throws Exception {
		if (locationFormat == null) {
			String fileName = file.getName();
			String path = "jar/" + fileName;
			Resource resource = jar.getResource(path);
			if (resource != null) {
				if ((file.length() == resource.size()) && (SHA1.digest(file)
					.equals(SHA1.digest(resource.openInputStream())))) {
					return path; // same resource
				}
				String[] parts = Strings.extension(fileName);
				if (parts == null) {
					parts = new String[] {
						fileName, ""
					};
				}
				for (int i = 1; jar.exists(path); i++) {
					path = String.format("jar/%s[%d].%s", parts[0], i, parts[1]);
				}
			}
			return path;
		}

		try {
			Domain domain = Domain.domain(file);
			Entry<String, Attrs> bundleSymbolicName = domain.getBundleSymbolicName();
			if (bundleSymbolicName == null) {
				warning("Cannot find bsn in %s, required because it is on the -runbundles", file);
				return nonCollidingPath(file, jar, null);
			}
			String bundleVersion = domain.getBundleVersion();
			if (bundleVersion == null)
				bundleVersion = "0";
			else {
				if (!Verifier.isVersion(bundleVersion)) {
					error("Invalid bundle version in %s", file);
					return nonCollidingPath(file, jar, null);
				}
			}
			try (Processor p = new Processor(this)) {

				p.setProperty("@bsn", bundleSymbolicName.getKey());
				p.setProperty("@version", bundleVersion);
				p.setProperty("@name", file.getName());

				String fileName = p.getReplacer()
					.process(locationFormat);

				if (fileName.contains("/")) {
					error("Invalid bundle version in %s", file);
					return nonCollidingPath(file, jar, null);
				}

				String filePath = "jar/" + fileName;
				if (jar.getResources()
					.containsKey(filePath)) {
					error("Duplicate locations for %s for file %s", filePath, file);
				}
				return filePath;
			}
		} catch (Exception e) {
			exception(e, "failed to use location pattern %s for location for file %", locationFormat, file);
			return nonCollidingPath(file, jar, null);
		}
	}

	/*
	 * Useful for when exported as folder or unzipped
	 */
	void doStart(Jar jar, String fqn) throws UnsupportedEncodingException {
		Collection<String> arguments = getProject().getMergedParameters(RUNVM)
			.keyList();
		Collection<String> progArgs = getProject().getMergedParameters(RUNPROGRAMARGS)
			.keyList();
		String nix = "#!/bin/sh\njava -cp . " + renderArguments(arguments, false) + " " + fqn
			+ (progArgs.isEmpty() ? "" : " " + renderArguments(progArgs, false)) + "\n";
		String pc = "java -cp . " + renderArguments(arguments, true) + " " + fqn
			+ (progArgs.isEmpty() ? "" : " " + renderArguments(progArgs, true)) + "\r\n";
		jar.putResource("start", new EmbeddedResource(nix, 0L));
		jar.putResource("start.bat", new EmbeddedResource(pc, 0L));
	}

}
