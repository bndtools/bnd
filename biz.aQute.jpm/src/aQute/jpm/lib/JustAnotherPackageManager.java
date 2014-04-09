package aQute.jpm.lib;

import static aQute.lib.io.IO.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.*;
import java.util.regex.*;

import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.version.*;
import aQute.jpm.facade.repo.*;
import aQute.jpm.platform.*;
import aQute.jsonrpc.proxy.*;
import aQute.lib.base64.*;
import aQute.lib.collections.*;
import aQute.lib.converter.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.json.*;
import aQute.lib.justif.*;
import aQute.lib.settings.*;
import aQute.lib.strings.*;
import aQute.libg.cryptography.*;
import aQute.rest.urlclient.*;
import aQute.service.library.*;
import aQute.service.library.Library.Program;
import aQute.service.library.Library.Revision;
import aQute.service.library.Library.RevisionRef;
import aQute.service.reporter.*;
import aQute.struct.*;

/**
 * JPM is the Java package manager. It manages a local repository in the user
 * global directory and/or a global directory. This class is the main entry
 * point for the command line. This program maintains a repository, a list of
 * installed commands, and a list of installed service. It provides the commands
 * to changes these resources. All information is kept in a platform specific
 * area. However, the layout of this area is standardized.
 * 
 * <pre>
 * 	platform/
 *      check									check for write access
 *      repo/                              		repository
 *        &lt;bsn&gt;/                     		bsn directory
 *          &lt;bsn&gt;-&lt;version&gt;.jar		jar file
 *      service/                               All service
 *        &lt;service&gt;/                      A service
 *        	data								Service data (JSON)
 *          wdir/								Working dir
 *          lock								Lock file (if running, contains port)
 *      commands/
 *        &lt;command&gt;						Command data (JSON)
 * </pre>
 * 
 * For each service, the platform must also have a user writable directory used
 * for working dir, lock, and logging.
 * 
 * <pre>
 *   platform var/
 *       wdir/									Working dir
 *       lock									Lock file (exists only when running, contains UDP port)
 * </pre>
 */

public class JustAnotherPackageManager {
	private static final String	JPM_VMS_EXTRA		= "jpm.vms.extra";
	private static final String	SERVICE_JAR_FILE	= "service.jar";
	public static final String	SERVICE				= "service";
	public static final String	COMMANDS			= "commands";
	public static final String	LOCK				= "lock";
	private static final String	JPM_CACHE_LOCAL		= "jpm.cache.local";
	private static final String	JPM_CACHE_GLOBAL	= "jpm.cache.global";
	static final String			PERMISSION_ERROR	= "No write acces, might require administrator or root privileges (sudo in *nix)";
	static JSONCodec			codec				= new JSONCodec();
	static Pattern				BSN_P				= Pattern
															.compile(
																	"([-a-z0-9_]+(?:\\.[-a-z0-9_]+)+)(?:@([0-9]+(?:\\.[0-9]+(?:\\.[0-9]+(?:\\.[-_a-z0-9]+)?)?)?))?",
																	Pattern.CASE_INSENSITIVE);
	static Pattern				COORD_P				= Pattern
															.compile(
																	"([-a-z0-9_.]+):([-a-z0-9_.]+)(?::([-a-z0-9_.]+))?(?:@([-a-z0-9._]+))?",
																	Pattern.CASE_INSENSITIVE);
	static Pattern				URL_P				= Pattern.compile("([a-z]{3,6}:/.*)", Pattern.CASE_INSENSITIVE);
	static Pattern				CMD_P				= Pattern.compile("([a-z_][a-z\\d_]*)", Pattern.CASE_INSENSITIVE);
	static Pattern				SHA_P				= Pattern.compile("(?:sha:)?([a-fA-F0-9]{40,40})",
															Pattern.CASE_INSENSITIVE);
	static Executor				executor;

	final File					homeDir;
	final File					binDir;
	final File					repoDir;
	final File					commandDir;
	final File					serviceDir;
	final File					service;
	final Platform				platform;
	final Reporter				reporter;

	JpmRepo						library;
	final List<Service>			startedByDaemon		= new ArrayList<Service>();
	boolean						localInstall		= false;
	private URLClient			host;
	private boolean				underTest			= System.getProperty("jpm.intest") != null;
	Settings					settings;

	/**
	 * Constructor
	 * 
	 * @throws Exception
	 */
	public JustAnotherPackageManager(Reporter reporter, Platform platform, File homeDir, File binDir) throws Exception {

		if (platform == null)
			this.platform = Platform.getPlatform(reporter, null);
		else
			this.platform = platform;

		settings = new Settings(this.platform.getConfigFile());

		this.reporter = reporter;
		this.homeDir = homeDir;
		if (!homeDir.exists() && !homeDir.mkdirs())
			throw new IllegalArgumentException("Could not create directory " + homeDir);

		repoDir = IO.getFile(homeDir, "repo");
		if (!repoDir.exists() && !repoDir.mkdirs())
			throw new IllegalArgumentException("Could not create directory " + repoDir);

		commandDir = new File(homeDir, COMMANDS);
		serviceDir = new File(homeDir, SERVICE);
		commandDir.mkdir();
		serviceDir.mkdir();
		service = new File(repoDir, SERVICE_JAR_FILE);
		if (!service.isFile())
			init();

		this.binDir = binDir;
		if (!binDir.exists() && !binDir.mkdirs())
			throw new IllegalArgumentException("Could not create bin directory " + binDir);
	}

	public String getArtifactIdFromCoord(String coord) {
		Matcher m = COORD_P.matcher(coord);
		if (m.matches()) {
			return m.group(2);
		} else {
			return null;
		}
	}

	public boolean hasAccess() {
		assert (binDir != null);
		assert (homeDir != null);

		return binDir.canWrite() && homeDir.canWrite();
	}

	public File getHomeDir() {
		return homeDir;
	}

	public File getRepoDir() {
		return repoDir;
	}

	public File getBinDir() {
		return binDir;
	}

	public List<ServiceData> getServices() throws Exception {
		return getServices(serviceDir);
	}

	public List<ServiceData> getServices(File serviceDir) throws Exception {
		List<ServiceData> result = new ArrayList<ServiceData>();

		if (!serviceDir.exists()) {
			return result;
		}

		for (File sdir : serviceDir.listFiles()) {
			File dataFile = new File(sdir, "data");
			ServiceData data = getData(ServiceData.class, dataFile);
			result.add(data);
		}
		return result;
	}

	public List<CommandData> getCommands() throws Exception {
		return getCommands(commandDir);
	}

	public List<CommandData> getCommands(File commandDir) throws Exception {
		List<CommandData> result = new ArrayList<CommandData>();

		if (!commandDir.exists()) {
			return result;
		}

		for (File f : commandDir.listFiles()) {
			CommandData data = getData(CommandData.class, f);
			if (data != null)
				result.add(data);
		}
		return result;
	}

	public CommandData getCommand(String name) throws Exception {
		File f = new File(commandDir, name);
		if (!f.isFile())
			return null;

		return getData(CommandData.class, f);
	}

	/**
	 * Garbage collect repository
	 * 
	 * @throws Exception
	 */
	public void gc() throws Exception {
		HashSet<byte[]> deps = new HashSet<byte[]>();

		// deps.add(SERVICE_JAR_FILE);

		for (File cmd : commandDir.listFiles()) {
			CommandData data = getData(CommandData.class, cmd);
			addDependencies(deps, data);
		}

		for (File service : serviceDir.listFiles()) {
			File dataFile = new File(service, "data");
			ServiceData data = getData(ServiceData.class, dataFile);
			addDependencies(deps, data);
		}

		int count = 0;
		for (File f : repoDir.listFiles()) {
			String name = f.getName();
			if (!deps.contains(name.getBytes())) {
				if (!name.endsWith(".json") || !deps.contains(name.substring(0, name.length() - ".json".length()).getBytes())) { // Remove
																														// json
																														// files
																														// only
																														// if
																														// the
																														// bin
																														// is
																														// going
																														// as
																														// well
					f.delete();
					count++;
				} else {

				}
			}
		}
		System.out.format("Garbage collection done (%d file(s) removed)%n", count);
	}

	private void addDependencies(HashSet<byte[]> deps, CommandData data) {
		for (byte[] dep : data.dependencies) {
			deps.add(dep);
		}
		for (byte[] dep : data.runbundles) {
			deps.add(dep);
		}
	}

	public void deinit(Appendable out, boolean force) throws Exception {
		Settings settings = new Settings(platform.getConfigFile());

		if (!force) {
			Justif justify = new Justif(80, 40);
			StringBuilder sb = new StringBuilder();
			Formatter f = new Formatter(sb);

			try {
				String list = listFiles(platform.getGlobal());
				if (list != null) {
					f.format("In global default environment:%n");
					f.format(list);
				}

				list = listFiles(platform.getLocal());
				if (list != null) {
					f.format("In local default environment:%n");
					f.format(list);
				}

				if (settings.containsKey(JPM_CACHE_GLOBAL)) {
					list = listFiles(IO.getFile(settings.get(JPM_CACHE_GLOBAL)));
					if (list != null) {
						f.format("In global configured environment:%n");
						f.format(list);
					}
				}

				if (settings.containsKey(JPM_CACHE_LOCAL)) {
					list = listFiles(IO.getFile(settings.get(JPM_CACHE_LOCAL)));
					if (list != null) {
						f.format("In local configured environment:%n");
						f.format(list);
					}
				}

				list = listSupportFiles();
				if (list != null) {
					f.format("jpm support files:%n");
					f.format(list);
				}

				f.format("%n%n");

				f.format("All files listed above will be deleted if deinit is run with the force flag set"
						+ " (\"jpm deinit -f\" or \"jpm deinit --force\"%n%n");
				f.flush();

				justify.wrap(sb);
				out.append(sb.toString());
			}
			finally {
				f.close();
			}
		} else { // i.e. if(force)
			int count = 0;
			File[] caches = {
					platform.getGlobal(), platform.getLocal(), null, null
			};
			if (settings.containsKey(JPM_CACHE_LOCAL)) {
				caches[2] = IO.getFile(settings.get(JPM_CACHE_LOCAL));
			}
			if (settings.containsKey(JPM_CACHE_GLOBAL)) {
				caches[3] = IO.getFile(settings.get(JPM_CACHE_GLOBAL));
			}
			ArrayList<File> toDelete = new ArrayList<File>();

			for (File cache : caches) {
				if (cache == null || !cache.exists()) {
					continue;
				}
				listFiles(cache, toDelete);
				if (toDelete.size() > count) {
					count = toDelete.size();
					if (!cache.canWrite()) {
						reporter.error(PERMISSION_ERROR + " (" + cache + ")");
						return;
					}
					toDelete.add(cache);
				}
			}
			listSupportFiles(toDelete);

			for (File f : toDelete) {
				if (f.exists() && !f.canWrite()) {
					reporter.error(PERMISSION_ERROR + " (" + f + ")");
				}
			}
			if (reporter.getErrors().size() > 0) {
				return;
			}

			for (File f : toDelete) {
				if (f.exists()) {
					IO.deleteWithException(f);
				}
			}
		}

	}

	// Adapter to list without planning to delete
	private String listFiles(final File cache) throws Exception {
		return listFiles(cache, null);
	}

	private String listFiles(final File cache, List<File> toDelete) throws Exception {
		boolean stopServices = false;
		if (toDelete == null) {
			toDelete = new ArrayList<File>();
		} else {
			stopServices = true;
		}
		int count = 0;
		Formatter f = new Formatter();

		f.format(" - Cache:%n    * %s%n", cache.getCanonicalPath());
		f.format(" - Commands:%n");
		for (CommandData cdata : getCommands(new File(cache, COMMANDS))) {
			f.format("    * %s \t0 handle for \"%s\"%n", cdata.bin, cdata.name);
			toDelete.add(new File(cdata.bin));
			count++;
		}
		f.format(" - Services:%n");
		for (ServiceData sdata : getServices(new File(cache, SERVICE))) {
			if (sdata != null) {
				f.format("    * %s \t0 service directory for \"%s\"%n", sdata.sdir, sdata.name);
				toDelete.add(new File(sdata.sdir));
				File initd = platform.getInitd(sdata);
				if (initd != null && initd.exists()) {
					f.format("    * %s \t0 init.d file for \"%s\"%n", initd.getCanonicalPath(), sdata.name);
					toDelete.add(initd);
				}
				if (stopServices) {
					Service s = getService(sdata);
					try {
						s.stop();
					}
					catch (Exception e) {}
				}
				count++;
			}
		}
		f.format("%n");

		String result = (count > 0) ? f.toString() : null;
		f.close();
		return result;
	}

	private String listSupportFiles() throws Exception { // Adapter to list
															// without planning
															// to delete
		return listSupportFiles(null);
	}

	private String listSupportFiles(List<File> toDelete) throws Exception {
		Formatter f = new Formatter();
		try {
			if (toDelete == null) {
				toDelete = new ArrayList<File>();
			}
			int precount = toDelete.size();
			File confFile = IO.getFile(platform.getConfigFile()).getCanonicalFile();
			if (confFile.exists()) {
				f.format("    * %s \t0 Config file%n", confFile);
				toDelete.add(confFile);
			}

			String result = (toDelete.size() > precount) ? f.toString() : null;
			return result;
		}
		finally {
			f.close();
		}

	}

	/**
	 * @param data
	 * @param target
	 * @throws Exception
	 * @throws IOException
	 */
	public String createService(ServiceData data) throws Exception, IOException {

		File sdir = new File(serviceDir, data.name);
		if (!sdir.exists() && !sdir.mkdirs()) {
			throw new IOException("Could not create directory " + data.sdir);
		}
		data.sdir = sdir.getAbsolutePath();

		File lock = new File(data.sdir, LOCK);
		data.lock = lock.getAbsolutePath();

		if (data.work == null)
			data.work = new File(data.sdir, "work").getAbsolutePath();
		if (data.user == null)
			data.user = platform.user();

		if (data.user == null)
			data.user = "root";

		new File(data.work).mkdir();

		if (data.log == null)
			data.log = new File(data.sdir, "log").getAbsolutePath();

		// TODO
		// if (Data.validate(data) != null)
		// return "Invalid service data: " + Data.validate(data);

		if (service == null)
			throw new RuntimeException(
					"Missing biz.aQute.jpm.service in repo, should have been installed by init, try reiniting");

		data.serviceLib = service.getAbsolutePath();

		platform.chown(data.user, true, new File(data.sdir));

		String s = platform.createService(data, null, false);
		if (s == null)
			storeData(new File(data.sdir, "data"), data);
		return s;
	}

	/**
	 * @param data
	 * @param target
	 * @throws Exception
	 * @throws IOException
	 */
	public String createCommand(CommandData data, boolean force) throws Exception, IOException {

		// TODO
		// if (Data.validate(data) != null)
		// return "Invalid command data: " + Data.validate(data);

		Map<String,String> map = null;
		if (data.trace) {
			map = new HashMap<String,String>();
			map.put("java.security.manager", "aQute.jpm.service.TraceSecurityManager");
			reporter.trace("tracing");
		}
		String s = platform.createCommand(data, map, force, service.getAbsolutePath());
		if (s == null)
			storeData(new File(commandDir, data.name), data);
		return s;
	}

	public void deleteCommand(String name) throws Exception {
		CommandData cmd = getCommand(name);
		if (cmd == null)
			throw new IllegalArgumentException("No such command " + name);

		platform.deleteCommand(cmd);
		File tobedel = new File(commandDir, name);
		IO.deleteWithException(tobedel);
	}

	public Service getService(String serviceName) throws Exception {
		File base = new File(serviceDir, serviceName);
		return getService(base);
	}

	public Service getService(ServiceData sdata) throws Exception {
		return getService(new File(sdata.sdir));
	}

	private Service getService(File base) throws Exception {
		File dataFile = new File(base, "data");
		if (!dataFile.isFile())
			return null;

		ServiceData data = getData(ServiceData.class, dataFile);
		return new Service(this, data);
	}

	/**
	 * Verify that the jar file is correct. This also verifies ok when there are
	 * no checksums or.
	 * 
	 * @throws IOException
	 */
	static Pattern	MANIFEST_ENTRY	= Pattern.compile("(META-INF/[^/]+)|(.*/)");

	public String verify(JarFile jar, String... algorithms) throws IOException {
		if (algorithms == null || algorithms.length == 0)
			algorithms = new String[] {
					"MD5", "SHA"
			};
		else if (algorithms.length == 1 && algorithms[0].equals("-"))
			return null;

		try {
			Manifest m = jar.getManifest();
			if (m.getEntries().isEmpty())
				return "No name sections";

			for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
				JarEntry je = e.nextElement();
				if (MANIFEST_ENTRY.matcher(je.getName()).matches())
					continue;

				Attributes nameSection = m.getAttributes(je.getName());
				if (nameSection == null)
					return "No name section for " + je.getName();

				for (String algorithm : algorithms) {
					try {
						MessageDigest md = MessageDigest.getInstance(algorithm);
						String expected = nameSection.getValue(algorithm + "-Digest");
						if (expected != null) {
							byte digest[] = Base64.decodeBase64(expected);
							copy(jar.getInputStream(je), md);
							if (!Arrays.equals(digest, md.digest()))
								return "Invalid digest for " + je.getName() + ", " + expected + " != "
										+ Base64.encodeBase64(md.digest());
						} else
							reporter.error("could not find digest for " + algorithm + "-Digest");
					}
					catch (NoSuchAlgorithmException nsae) {
						return "Missing digest algorithm " + algorithm;
					}
				}
			}
		}
		catch (Exception e) {
			return "Failed to verify due to exception: " + e.getMessage();
		}
		return null;
	}

	/**
	 * @param clazz
	 * @param dataFile
	 * @return
	 * @throws Exception
	 */

	private <T> T getData(Class<T> clazz, File dataFile) throws Exception {
		try {
			return codec.dec().from(dataFile).get(clazz);
		}
		catch (Exception e) {
			// e.printStackTrace();
			// System.out.println("Cannot read data file "+dataFile+": " +
			// IO.collect(dataFile));
			return null;
		}
	}

	private void storeData(File dataFile, Object o) throws Exception {
		codec.enc().to(dataFile).put(o);
	}

	/**
	 * This is called when JPM runs in the background to start jobs
	 * 
	 * @throws Exception
	 */
	public void daemon() throws Exception {
		Runtime.getRuntime().addShutdownHook(new Thread("Daemon shutdown") {
			public void run() {

				for (Service service : startedByDaemon) {
					try {
						reporter.error("Stopping " + service);
						service.stop();
						reporter.error("Stopped " + service);
					}
					catch (Exception e) {
						// Ignore
					}
				}
			}
		});
		List<ServiceData> services = getServices();
		Map<String,ServiceData> map = new HashMap<String,ServiceData>();
		for (ServiceData d : services) {
			map.put(d.name, d);
		}
		List<ServiceData> start = new ArrayList<ServiceData>();
		Set<ServiceData> set = new HashSet<ServiceData>();
		for (ServiceData sd : services) {
			checkStartup(map, start, sd, set);
		}

		if (start.isEmpty())
			reporter.warning("No services to start");

		for (ServiceData sd : start) {
			try {
				Service service = getService(sd.name);
				reporter.trace("Starting " + service);
				String result = service.start();
				if (result != null)
					reporter.error("Started error " + result);
				else
					startedByDaemon.add(service);
				reporter.trace("Started " + service);
			}
			catch (Exception e) {
				reporter.error("Cannot start daemon %s, due to %s", sd.name, e);
			}
		}

		while (true) {
			for (Service sd : startedByDaemon) {
				try {
					if (!sd.isRunning()) {
						reporter.error("Starting due to failure " + sd);
						String result = sd.start();
						if (result != null)
							reporter.error("Started error " + result);
					}
				}
				catch (Exception e) {
					reporter.error("Cannot start daemon %s, due to %s", sd, e);
				}
			}
			Thread.sleep(10000);
		}

	}

	private void checkStartup(Map<String,ServiceData> map, List<ServiceData> start, ServiceData sd,
			Set<ServiceData> cyclic) {
		if (sd.after.isEmpty() || start.contains(sd))
			return;

		if (cyclic.contains(sd)) {
			reporter.error("Cyclic dependency for " + sd.name);
			return;
		}

		cyclic.add(sd);

		for (String dependsOn : sd.after) {
			if (dependsOn.equals("boot"))
				continue;

			ServiceData deps = map.get(dependsOn);
			if (deps == null) {
				reporter.error("No such service " + dependsOn + " but " + sd.name + " depends on it");
			} else {
				checkStartup(map, start, deps, cyclic);
			}
		}
		start.add(sd);
	}

	public void register(boolean user) throws Exception {
		platform.installDaemon(user);
	}

	public ArtifactData putAsync(final URI uri) {
		final ArtifactData data = new ArtifactData();
		data.busy = true;
		Runnable r = new Runnable() {

			public void run() {
				try {
					put(uri, data);
				}
				catch (Throwable e) {
					e.printStackTrace();
					data.error = e.toString();
				}
				finally {
					reporter.trace("done downloading %s", uri);
					data.done();
				}
			}

		};
		getExecutor().execute(r);
		return data;
	}

	public ArtifactData put(final URI uri) throws Exception {
		final ArtifactData data = new ArtifactData();
		put(uri, data);
		return data;
	}

	void put(final URI uri, ArtifactData data) throws Exception {
		reporter.trace("put %s %s", uri, data);
		File tmp = createTempFile(repoDir, "mtp", ".whatever");
		tmp.deleteOnExit();
		try {
			copy(uri.toURL(), tmp);
			byte[] sha = SHA1.digest(tmp).digest();
			reporter.trace("SHA %s %s", uri, Hex.toHexString(sha));
			ArtifactData existing = get(sha);
			if (existing != null) {
				reporter.trace("existing");
				xcopy(existing, data);
				return;
			}
			File meta = new File(repoDir, Hex.toHexString(sha) + ".json");
			File file = new File(repoDir, Hex.toHexString(sha));
			rename(tmp, file);
			reporter.trace("file %s", file);
			data.file = file.getAbsolutePath();
			data.sha = sha;
			data.busy = false;
			CommandData cmddata = parseCommandData(data);
			if (cmddata.bsn != null) {
				data.name = cmddata.bsn + "-" + cmddata.version;
			} else
				data.name = Strings.display(cmddata.title, cmddata.bsn, cmddata.name, uri);
			codec.enc().to(meta).put(data);
			reporter.trace("TD = " + data);
		}
		finally {
			tmp.delete();
			reporter.trace("puted %s %s", uri, data);
		}
	}

	public ArtifactData get(byte[] sha) throws Exception {
		String name = Hex.toHexString(sha);
		File data = IO.getFile(repoDir, name + ".json");
		reporter.trace("artifact data file %s", data);
		if (data.isFile()) { // Bin + metadata
			ArtifactData artifact = codec.dec().from(data).get(ArtifactData.class);
			artifact.file = IO.getFile(repoDir, name).getAbsolutePath();
			return artifact;
		}
		File bin = IO.getFile(repoDir, name);
		if (bin.exists()) { // Only bin
			ArtifactData artifact = new ArtifactData();
			artifact.file = bin.getAbsolutePath();
			artifact.sha = sha;
			return artifact;
		}

		return null;
	}

	public List<Revision> filter(Collection<Revision> list, EnumSet<Library.Phase> phases) {
		List<Revision> filtered = new ArrayList<Library.Revision>();
		for (Revision r : list)
			if (phases.contains(r.phase))
				filtered.add(r);

		return filtered;
	}

	public Map<String,Revision> latest(Collection<Revision> list) {
		Map<String,Revision> programs = new HashMap<String,Library.Revision>();

		for (Revision r : list) {
			String coordinates = r.groupId + ":" + r.artifactId;
			if (r.classifier != null)
				coordinates += ":" + r.classifier;

			if (r.groupId.equals(Library.SHA_GROUP))
				continue;

			Revision current = programs.get(coordinates);
			if (current == null)
				programs.put(coordinates, r);
			else {
				// who is better?
				if (compare(r, current) >= 0)
					programs.put(coordinates, r);
			}
		}
		return programs;
	}

	private int compare(Revision a, Revision b) {
		if (Arrays.equals(a._id, b._id))
			return 0;

		Version va = getVersion(a);
		Version vb = getVersion(b);
		int n = va.compareTo(vb);
		if (n != 0)
			return n;

		if (a.created != b.created)
			return a.created > b.created ? 1 : -1;

		for (int i = 0; i < a._id.length; i++)
			if (a._id[i] != b._id[i])
				return a._id[i] > b._id[i] ? 1 : -1;

		return 0;
	}

	private Version getVersion(Revision a) {
		if (a.qualifier != null)
			return new Version(a.baseline + "." + a.qualifier);
		return new Version(a.baseline);
	}

	public String getCoordinates(Revision r) {
		StringBuilder sb = new StringBuilder(r.groupId).append(":").append(r.artifactId);
		if (r.classifier != null)
			sb.append(":").append(r.classifier);
		sb.append("@").append(r.version);

		return sb.toString();
	}

	public String getCoordinates(RevisionRef r) {
		StringBuilder sb = new StringBuilder(r.groupId).append(":").append(r.artifactId).append(":");
		if (r.classifier != null)
			sb.append(r.classifier).append("@");
		sb.append(r.version);

		return sb.toString();
	}

	public ArtifactData getCandidate(String key) throws Exception {
		ArtifactData data = getCandidateAsync(key);
		if (data != null) {
			data.sync();
		}
		return data;
	}

	public ArtifactData getCandidateAsync(String arg) throws Exception {
		reporter.trace("coordinate %s", arg);
		if (isUrl(arg))
			try {
				ArtifactData data = putAsync(new URI(arg));
				data.local = true;
				return data;
			}
			catch (Exception e) {
				reporter.trace("hmm, not a valid url %s, will try the server", arg);
			}

		Coordinate c = new Coordinate(arg);

		if (c.isSha()) {
			ArtifactData r = get(c.getSha());
			if (r != null)
				return r;
		}

		Revision revision = library.getRevisionByCoordinate(c);
		if (revision == null)
			return null;

		reporter.trace("revision %s", Hex.toHexString(revision._id));

		ArtifactData ad = get(revision._id);
		if (ad != null) {
			reporter.trace("found in cache");
			return ad;
		}

		URI url = revision.urls.iterator().next();
		ArtifactData artifactData = putAsync(url);
		artifactData.coordinate = c;
		return artifactData;
	}

	public static Executor getExecutor() {
		if (executor == null)
			executor = Executors.newFixedThreadPool(4);
		return executor;
	}

	public static void setExecutor(Executor executor) {
		JustAnotherPackageManager.executor = executor;
	}

	public void setLibrary(URI url) throws Exception {
		if (url == null)
			url = new URI("http://repo.jpm4j.org/");

		this.host = new URLClient(url.toString());
		host.setReporter(reporter);
		library = JSONRPCProxy.createRPC(JpmRepo.class, host, "jpm");
	}

	public void close() {
		if (executor != null && executor instanceof ExecutorService)
			((ExecutorService) executor).shutdown();
	}

	public void init() throws IOException {
		URL s = getClass().getClassLoader().getResource(SERVICE_JAR_FILE);
		if (s == null)
			if (underTest)
				return;
			else
				throw new Error("No " + SERVICE_JAR_FILE + " resource in jar");
		service.getParentFile().mkdirs();
		IO.copy(s, service);
	}

	public Platform getPlatform() {
		return platform;
	}

	/**
	 * Copy from the copy method in StructUtil. Did not want to drag that code
	 * in. maybe this actually should go to struct.
	 * 
	 * @param from
	 * @param to
	 * @param excludes
	 * @return
	 * @throws Exception
	 */
	static public <T extends struct> T xcopy(struct from, T to, String... excludes) throws Exception {
		Arrays.sort(excludes);
		for (Field f : from.fields()) {
			if (Arrays.binarySearch(excludes, f.getName()) >= 0)
				continue;

			Object o = f.get(from);
			if (o == null)
				continue;

			Field tof = to.getField(f.getName());
			if (tof != null)
				try {
					tof.set(to, Converter.cnv(tof.getGenericType(), o));
				}
				catch (Exception e) {
					System.out.println("Failed to convert " + f.getName() + " from " + from.getClass() + " to "
							+ to.getClass() + " value " + o + " exception " + e);
				}
		}

		return to;
	}

	public JpmRepo getLibrary() {
		return library;
	}

	public void setLocalInstall(boolean b) {
		localInstall = b;
	}

	public String what(String key, boolean oneliner) throws Exception {
		byte[] sha;

		Matcher m = SHA_P.matcher(key);
		if (m.matches()) {
			sha = Hex.toByteArray(key);
		} else {
			m = URL_P.matcher(key);
			if (m.matches()) {
				URL url = new URL(key);
				sha = SHA1.digest(url.openStream()).digest();
			} else {
				File jarfile = new File(key);
				if (!jarfile.exists()) {
					reporter.error("File does not exist: %s", jarfile.getCanonicalPath());
				}
				sha = SHA1.digest(jarfile).digest();
			}
		}
		reporter.trace("sha %s", Hex.toHexString(sha));
		Revision revision = library.getRevision(sha);
		if (revision == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		Formatter f = new Formatter(sb);
		Justif justif = new Justif(120, 20, 70, 20, 75);
		DateFormat dateFormat = DateFormat.getDateInstance();

		try {
			if (oneliner) {
				f.format("%20s %s%n", Hex.toHexString(revision._id), createCoord(revision));
			} else {
				f.format("Artifact: %s%n", revision.artifactId);
				if (revision.organization != null && revision.organization.name != null) {
					f.format(" (%s)", revision.organization.name);
				}
				f.format("%n");
				f.format("Coordinates\t0: %s%n", createCoord(revision));
				f.format("Created\t0: %s%n", dateFormat.format(new Date(revision.created)));
				f.format("Size\t0: %d%n", revision.size);
				f.format("Sha\t0: %s%n", Hex.toHexString(revision._id));
				f.format("URL\t0: %s%n", createJpmLink(revision));
				f.format("%n");
				f.format("%s%n", revision.description);
				f.format("%n");
				f.format("Dependencies\t0:%n");
				boolean flag = false;
				Iterable<RevisionRef> closure = library.getClosure(revision._id, true);
				for (RevisionRef dep : closure) {
					f.format(" - %s \t2- %s \t3- %s%n", dep.name, createCoord(dep),
							dateFormat.format(new Date(dep.created)));
					flag = true;
				}
				if (!flag) {
					f.format("     None%n");
				}
				f.format("%n");
			}
			f.flush();
			justif.wrap(sb);
			return sb.toString();
		}
		finally {
			f.close();
		}

	}

	private String createCoord(Revision rev) {
		return String.format("%s:%s@%s [%s]", rev.groupId, rev.artifactId, rev.version, rev.phase);
	}

	private String createCoord(RevisionRef rev) {
		return String.format("%s:%s@%s [%s]", rev.groupId, rev.artifactId, rev.version, rev.phase);
	}

	private String createJpmLink(Revision rev) {
		return String.format("http://jpm4j.org/#!/p/sha/%s//%s", Hex.toHexString(rev._id), rev.baseline);
	}

	public class UpdateMemo {
		public CommandData	current;	// Works for commandData and
										// ServiceData, as ServiceData --|>
										// CommandData
		public RevisionRef	best;
	}

	public void listUpdates(List<UpdateMemo> notFound, List<UpdateMemo> upToDate, List<UpdateMemo> toUpdate,
			CommandData data, boolean staged) throws Exception {

		// UpdateMemo memo = new UpdateMemo();
		// memo.current = data;
		//
		// Matcher m = data.coordinates == null ? null :
		// COORD_P.matcher(data.coordinates);
		//
		// if (data.version == null || m == null || !m.matches()) {
		// Revision revision = library.getRevision(data.sha);
		// if (revision == null) {
		// notFound.add(memo);
		// return;
		// }
		// data.version = new Version(revision.version);
		// data.coordinates = getCoordinates(revision);
		// if (data instanceof ServiceData) {
		// storeData(IO.getFile(new File(serviceDir, data.name), "data"), data);
		// } else {
		// storeData(IO.getFile(commandDir, data.name), data);
		// }
		// }
		//
		// Iterable< ? extends Program> programs =
		// library.getQueryPrograms(data.coordinates, 0, 0);
		// int count = 0;
		// RevisionRef best = null;
		// for (Program p : programs) {
		// // best = selectBest(p.revisions, staged, null);
		// count++;
		// }
		// if (count != 1 || best == null) {
		// notFound.add(memo);
		// return;
		// }
		// Version bestVersion = new Version(best.version);
		//
		// if (data.version.compareTo(bestVersion) < 0) { // Update available
		// memo.best = best;
		// toUpdate.add(memo);
		// } else { // up to date
		// upToDate.add(memo);
		// }
	}

	public void update(UpdateMemo memo) throws Exception {

		ArtifactData target = put(memo.best.urls.iterator().next());

		memo.current.version = new Version(memo.best.version);
		target.sync();
		memo.current.sha = target.sha;
		// memo.current.dependencies = target.dependencies;
		// memo.current.dependencies.add((new File(repoDir,
		// Hex.toHexString(target.sha))).getCanonicalPath());
		// memo.current.runbundles = target.runbundles;
		// memo.current.description = target.description;
		memo.current.time = target.time;

		if (memo.current instanceof ServiceData) {
			Service service = getService((ServiceData) memo.current);
			service.remove();
			createService((ServiceData) memo.current);
			IO.delete(new File(IO.getFile(serviceDir, memo.current.name), "data"));
			storeData(new File(IO.getFile(serviceDir, memo.current.name), "data"), memo.current);
		} else {
			platform.deleteCommand(memo.current);
			createCommand(memo.current, false);
			IO.delete(IO.getFile(commandDir, memo.current.name));
			storeData(IO.getFile(commandDir, memo.current.name), memo.current);
		}

	}

	private boolean isUrl(String coordinate) {
		return URL_P.matcher(coordinate).matches();
	}

	/**
	 * Find programs
	 * 
	 * @throws Exception
	 */

	public List<Program> find(String query, int skip, int limit) throws Exception {
		return library.getQueryPrograms(query, skip, limit);
	}

	public boolean isWildcard(String coordinate) {
		return coordinate != null && coordinate.endsWith("@*");
	}

	public CommandData parseCommandData(ArtifactData artifact) throws Exception {
		File source = new File(artifact.file);
		if (!source.isFile())
			throw new FileNotFoundException();

		CommandData data = new CommandData();
		data.sha = artifact.sha;
		data.jpmRepoDir = repoDir.getCanonicalPath();
		JarFile jar = new JarFile(source);
		try {
			reporter.trace("Parsing %s", source);
			Manifest m = jar.getManifest();
			Attributes main = m.getMainAttributes();
			data.name = data.bsn = main.getValue(Constants.BUNDLE_SYMBOLICNAME);
			String version = main.getValue(Constants.BUNDLE_VERSION);
			if (version == null)
				data.version = Version.LOWEST;
			else
				data.version = new Version(version);

			data.main = main.getValue("Main-Class");
			data.description = main.getValue(Constants.BUNDLE_DESCRIPTION);
			data.title = main.getValue("JPM-Name");

			if (main.getValue("Class-Path") != null) {
				File parent = source.getParentFile();
				for (String entry : main.getValue("Class-Path").split("\\s+")) {
					File child = new File(parent, entry);
					if (!child.isFile()) {
						reporter.error("Target specifies Class-Path in JAR but the indicated file %s is not found",
								child);
					} else {
						ArtifactData x = put(child.toURI());
						data.dependencies.add(x.sha);
					}
				}
			}

			reporter.trace("name " + data.name + " " + data.main + " " + data.title);
			DependencyCollector path = new DependencyCollector(this);
			path.add(artifact);
			DependencyCollector bundles = new DependencyCollector(this);
			if (main.getValue("JPM-Classpath") != null) {
				Parameters requires = OSGiHeader.parseHeader(main.getValue("JPM-Classpath"));

				for (Map.Entry<String,Attrs> e : requires.entrySet()) {
					path.add(e.getKey(), e.getValue().get("name")); // coordinate
				}
			} else if (!artifact.local) { // No JPM-Classpath, falling back to
											// server's revision
											// Iterable<RevisionRef> closure =
											// library.getClosure(artifact.sha,
											// false);
				// System.out.println("getting closure " + artifact.url + " " +
				// Strings.join("\n",closure));

				// if (closure != null) {
				// for (RevisionRef ref : closure) {
				// path.add(Hex.toHexString(ref.revision));
				// }
				// }
			}

			if (main.getValue("JPM-Runbundles") != null) {
				Parameters jpmrunbundles = OSGiHeader.parseHeader(main.getValue("JPM-Runbundles"));

				for (Map.Entry<String,Attrs> e : jpmrunbundles.entrySet()) {
					bundles.add(e.getKey(), e.getValue().get("name"));
				}
			}

			reporter.trace("collect digests runpath");
			data.dependencies.addAll(path.getDigests());
			reporter.trace("collect digests bundles");
			data.runbundles.addAll(bundles.getDigests());

			Parameters command = OSGiHeader.parseHeader(main.getValue("JPM-Command"));
			if (command.size() > 1)
				reporter.error("Only one command can be specified");

			for (Map.Entry<String,Attrs> e : command.entrySet()) {
				data.name = e.getKey();

				Attrs attrs = e.getValue();

				if (attrs.containsKey("jvmargs"))
					data.jvmArgs = attrs.get("jvmargs");

				if (attrs.containsKey("title"))
					data.title = attrs.get("title");

				if (data.title != null)
					data.title = data.name;

			}
			return data;
		}
		finally {
			jar.close();
		}
	}

	public void setUnderTest() {
		underTest = true;
	}

	/**
	 * Turn the shas into a readable form
	 * 
	 * @param dependencies
	 * @return
	 * @throws Exception
	 */

	public List< ? > toString(List<byte[]> dependencies) throws Exception {
		List<String> out = new ArrayList<String>();
		for (byte[] dependency : dependencies) {
			ArtifactData data = get(dependency);
			if (data == null)
				out.add(Hex.toHexString(dependency));
			else {
				out.add(Strings.display(data.name, Hex.toHexString(dependency)));
			}
		}
		return out;
	}

	/**
	 * Get a list of candidates from a coordinate
	 * 
	 * @param c
	 * @throws Exception
	 */
	public Iterable<Revision> getCandidates(Coordinate c) throws Exception {
		return library.getRevisionsByCoordinate(c);
	}

	/**
	 * Post install
	 */
	public void doPostInstall() {
		getPlatform().doPostInstall();
	}

	public SortedSet<JVM> getVMs() throws Exception {
		TreeSet<JVM> set = new TreeSet<JVM>(JVM.comparator);
		String list = settings.get(JPM_VMS_EXTRA);
		if ( list != null) {
			ExtList<String> elist = new ExtList<String>(list.split("\\s*,\\s*"));
			for ( String dir : elist) {
				File f  = new File(dir);
				JVM jvm = getPlatform().getJVM(f);
				if ( jvm == null) {
					jvm = new JVM();
					jvm.path = f.getCanonicalPath();
					jvm.name = "Not a valid VM";
					jvm.platformVersion = jvm.vendor=jvm.version = "";
				}
				set.add(jvm);
			}
		}
		getPlatform().getVMs(set);
		return set;
	}

	public JVM addVm(File platformRoot) throws Exception {
		if (!platformRoot.isDirectory()) {
			reporter.error("No such directory %s for a VM", platformRoot);
			return null;
		}

		JVM jvm = getPlatform().getJVM(platformRoot);
		if (jvm == null) {
			return null;
		}
		
		String list = settings.get(JPM_VMS_EXTRA);
		if (list == null)
			list = platformRoot.getCanonicalPath();
		else {
			ExtList<String> elist = new ExtList<String>(list.split("\\s*,\\s*"));
			elist.remove(platformRoot.getCanonicalPath());
			elist.add(0, platformRoot.getCanonicalPath());
			list = Strings.join(",", elist);
					
		}
		settings.put(JPM_VMS_EXTRA, list);
		settings.save();
		return jvm;
	}
}