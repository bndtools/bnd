package aQute.jpm.lib;

import static aQute.lib.io.IO.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.*;
import java.util.regex.*;

import aQute.bnd.build.*;
import aQute.bnd.header.*;
import aQute.bnd.version.*;
import aQute.jpm.platform.*;
import aQute.lib.base64.*;
import aQute.lib.converter.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.json.*;
import aQute.libg.cryptography.*;
import aQute.library.remote.*;
import aQute.service.library.*;
import aQute.service.library.Library.*;
import aQute.service.reporter.*;
import aQute.struct.*;

/**
 * JPM is the Java package manager. It manages a local repository in the user
 * global directory and/or a global directory. This class is the main entry point
 * for the command line. This program maintains a repository, a list of
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
	static JSONCodec	codec			= new JSONCodec();
	static Pattern		BSN_P			= Pattern
												.compile(
														"([-a-z0-9_]+(?:\\.[-a-z0-9_]+)+)(?:@([0-9]+(?:\\.[0-9]+(?:\\.[0-9]+(?:\\.[-_a-z0-9]+)?)?)?))?",
														Pattern.CASE_INSENSITIVE);
	static Pattern		COORD_P			= Pattern
												.compile(
														"([-a-z0-9_.]+):([-a-z0-9_.]+)(?::([-a-z0-9_.]+))?(?:@([-a-z0-9._]+))?",
														Pattern.CASE_INSENSITIVE);
	static Pattern		URL_P			= Pattern.compile("([a-z]{3,6}:/.*)", Pattern.CASE_INSENSITIVE);
	static Pattern		CMD_P			= Pattern.compile("([a-z_][a-z\\d_]*)", Pattern.CASE_INSENSITIVE);
	static Pattern		SHA_P			= Pattern.compile("(?:sha:)?([a-f0-9]{40,40})", Pattern.CASE_INSENSITIVE);
	static Executor		executor;

	File				homeDir;
	File				binDir;
	File				repoDir;
	File				commandDir;
	File				serviceDir;
	File				service;
	Platform			platform;
	RemoteLibrary		library			= new RemoteLibrary(null);
	Reporter			reporter;
	final List<Service>	startedByDaemon	= new ArrayList<Service>();

	/**
	 * Constructor
	 * 
	 * @throws IOException
	 */
	public JustAnotherPackageManager(Reporter reporter) throws IOException {
		this.reporter = reporter;
		setPlatform(Platform.getPlatform(reporter));
	}

	public boolean hasAccess() {
		File file = new File(homeDir, "check");
		try {
			store("", file);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	public List<ServiceData> getServices() throws Exception {
		List<ServiceData> result = new ArrayList<ServiceData>();
		for (File sdir : serviceDir.listFiles()) {
			File dataFile = new File(sdir, "data");
			ServiceData data = getData(ServiceData.class, dataFile);
			result.add(data);
		}
		return result;
	}

	public List<CommandData> getCommands() throws Exception {
		List<CommandData> result = new ArrayList<CommandData>();
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
	 * Garbage collect any service and commands.
	 * 
	 * @throws Exception
	 */
	public void gc() throws Exception {
		for (File cmd : commandDir.listFiles()) {
			CommandData data = getData(CommandData.class, cmd);
			// File repoFile = new File(data.repoFile);
			// if (!repoFile.isFile()) {
			// platform.remove(data);
			// cmd.delete();
			// }
		}

		for (File service : serviceDir.listFiles()) {
			File dataFile = new File(service, "data");

			ServiceData data = getData(ServiceData.class, dataFile);

			// if (!repoFile.isFile()) {
			// Service s = getService(service.getName());
			// s.stop();
			// if (data.work != null)
			// IO.delete(new File(data.work));
			// if (data.sdir != null)
			// IO.delete(new File(data.sdir));
			// if (data.log != null)
			// IO.delete(new File(data.log));
			// platform.remove(data);
			// IO.delete(service);
			// }
		}
	}

	/**
	 * Remove the JPM area.
	 * 
	 * @return
	 * @throws Exception
	 */

	public String deinit(boolean force) throws Exception {
		for ( CommandData d : getCommands())  {
			reporter.trace("delete cmd %s", d.name);
			deleteCommand(d.name);
		}
		
		for ( ServiceData sd : getServices()) {
			reporter.trace("delete service %s", sd.name);
			Service s = getService(sd.name);
			if ( s != null)
				s.remove();
		}
		
		
		reporter.trace("delete repo");
		IO.delete(repoDir);
		gc(); 
		reporter.trace("delete repo, command, and service dir");
		IO.delete(repoDir);
		IO.delete(commandDir);
		IO.delete(serviceDir);
		return null;
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

		File lock = new File(data.sdir, "lock");
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

		String s = platform.createService(data, null);
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
	public String createCommand(CommandData data) throws Exception, IOException {

		// TODO
		// if (Data.validate(data) != null)
		// return "Invalid command data: " + Data.validate(data);

		if (binDir != null)
			data.bin = new File(binDir, data.name).getAbsolutePath();

		Map<String,String> map = null;
		if ( data.trace ) {
			map = new HashMap<String,String>();
			map.put("java.security.manager", "aQute.jpm.service.TraceSecurityManager");
			reporter.trace("tracing");
		}
		String s = platform.createCommand(data, map, service.getAbsolutePath());
		if (s == null)
			storeData(new File(commandDir, data.name), data);
		return s;
	}

	public void deleteCommand(String name) throws Exception { //pl: no removal of no longer needed dependancies ?
		CommandData cmd = getCommand(name);
		if (cmd == null)
			throw new IllegalArgumentException("No such command " + name);

		platform.deleteCommand(cmd);
		File tobedel = new File(commandDir, name);
		IO.deleteWithException(tobedel);
	}

	public Service getService(String service) throws Exception {
		File base = new File(serviceDir, service);
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
							System.out.println("could not find digest for " + algorithm + "-Digest");
					}
					catch (NoSuchAlgorithmException nsae) {
						return "Missing digest algorithm " + algorithm;
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
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
			e.printStackTrace();
			System.out.println("Cannot read data file "+dataFile+": " + IO.collect(dataFile));
			return null;
		}
	}

	private void storeData(File dataFile, Object o) throws Exception {
		codec.enc().to(dataFile).put(o);
	}

	public void setPlatform(Platform plf) throws IOException {
		this.platform = plf;
		if (homeDir == null)
			homeDir = platform.getGlobal();

		initDirs();
	}

	void initDirs() throws IOException {
		if (!homeDir.exists() && !homeDir.mkdirs()) {
			throw new ExceptionInInitializerError("Could not create directory " + homeDir);
		}

		repoDir = IO.getFile(homeDir, "repo");
		if (!repoDir.exists() && !repoDir.mkdirs()) {
			throw new ExceptionInInitializerError("Could not create directory " + repoDir);
		}

		commandDir = new File(homeDir, "commands");
		serviceDir = new File(homeDir, "service");
		commandDir.mkdir();
		serviceDir.mkdir();
		service = new File(repoDir, "service.jar");
		if (!service.isFile()) {
			init();
		}
	}

	public ArtifactData parse(File source) throws Exception {
		assert source.isFile();

		JarFile jar = new JarFile(source);
		try {
			ArtifactData artifact = new ArtifactData();
			artifact.sha = SHA1.digest(source).digest();

			Manifest m = jar.getManifest();
			Attributes main = m.getMainAttributes();
			String name = main.getValue("Bundle-SymbolicName");
			String version = main.getValue("Bundle-Version");
			if (version != null)
				name += "-" + version;

			artifact.name = name;

			artifact.mainClass = main.getValue("Main-Class");
			artifact.description = main.getValue("Bundle-Description");
			artifact.title = main.getValue("JPM-Name");

			List<ArtifactData> dependencies = new ArrayList<ArtifactData>();
			{
				if (main.getValue("JPM-Classpath") != null) {
					Parameters requires = OSGiHeader.parseHeader(main.getValue("JPM-Classpath"));
					List<DownloadBlocker> blockers = new ArrayList<DownloadBlocker>();

					for (Map.Entry<String,Attrs> e : requires.entrySet()) {
						String key = e.getKey();
						String v = e.getValue().get("version");
						if (aQute.bnd.osgi.Verifier.isBsn(e.getKey()) && aQute.bnd.osgi.Verifier.isVersion(v)) {
							key = Library.OSGI_GROUP + ":" + key + ":" + v;
						}
						reporter.trace("searching %s", key);
						ArtifactData candidate = getCandidate(key, false);

						if (candidate == null) {
							reporter.error("Missing dependency: %s", key);
						} else {
							reporter.trace("found %s", candidate);
							dependencies.add(candidate);
						}
					}
				}
				/*else {
					// TODO pierre this is the (pseudo code)
					// do maven
					Revision revision = library.getRevision(artifact.sha);
					for ( Requirement req : revision.requirements ) {
						if ( req.ns.equals("x-maven")) {
							String coordinate = (String) req.ps.get("name:");
						}
					}
				}*/

				for (ArtifactData data : dependencies) {
					data.sync();
					if (data.error != null) {
						reporter.error("Download of %s failed: %s", data.name, data.error);
					} else {
						reporter.trace("adding dependency %s", data.file);
						artifact.dependencies.add(data.file);
					}
				}
			}

			{
				Parameters service = OSGiHeader.parseHeader(main.getValue("JPM-Service"));
				if (service.size() > 1)
					reporter.error("Only one service can be specified");
				for (Map.Entry<String,Attrs> e : service.entrySet()) {
					Attrs attrs = e.getValue();
					ServiceData data = new ServiceData();
					data.name = e.getKey();
					doService(attrs, data, artifact);
					data.name = e.getKey();
					artifact.service = data;
				}
				reporter.trace("service %s", artifact.service);
			}
			{
				Parameters command = OSGiHeader.parseHeader(main.getValue("JPM-Command"));
				if (command.size() > 1)
					reporter.error("Only one command can be specified");
				for (Map.Entry<String,Attrs> e : command.entrySet()) {
					Attrs attrs = e.getValue();
					CommandData data = new CommandData();
					doCommand(attrs, data, artifact);
					data.name = e.getKey();
					artifact.command = data;
				}
				reporter.trace("commands %s", artifact.command);
			}

			reporter.trace("returning " + artifact);
			return artifact;
		}
		finally {
			jar.close();
		}

	}

	private void doCommand(Attrs attrs, CommandData data, ArtifactData artifact) {
		data.sha = artifact.sha;
		data.description = artifact.description;
		if (attrs.containsKey("jvmargs"))
			data.jvmArgs = attrs.get("jvmargs");
		data.main = artifact.mainClass;
		data.title = attrs.get("title");

		if (data.title != null)
			data.title = artifact.title;

		if (data.title != null)
			data.title = data.name;

		data.dependencies = artifact.dependencies;
	}

	private void doService(Attrs attrs, ServiceData data, ArtifactData artifact) throws Exception {
		doCommand(attrs, data, artifact);
		data.user = platform.user();
		if (attrs.containsKey("args"))
			data.args = attrs.get("args");
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
						System.err.println("Stopping " + service);
						service.stop();
						System.err.println("Stopped " + service);
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
			System.out.println("No services to start");

		for (ServiceData sd : start) {
			try {
				Service service = getService(sd.name);
				System.err.println("Starting " + service);
				String result = service.start();
				if (result != null)
					System.err.println("Started error " + result);
				else
					startedByDaemon.add(service);
				System.err.println("Started " + service);
			}
			catch (Exception e) {
				System.err.println("Cannot start daemon " + sd.name);
			}
		}

		while (true) {
			for (Service sd : startedByDaemon) {
				try {
					if (!sd.isRunning()) {
						System.err.println("Starting due to failure " + sd);
						String result = sd.start();
						if (result != null)
							System.err.println("Started error " + result);
					}
				}
				catch (Exception e) {
					System.err.println("Cannot start daemon " + sd);
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
			System.err.println("Cyclic dependency for " + sd.name);
			return;
		}

		cyclic.add(sd);

		for (String dependsOn : sd.after) {
			if (dependsOn.equals("boot"))
				continue;

			ServiceData deps = map.get(dependsOn);
			if (deps == null) {
				System.err.println("No such service " + dependsOn + " but " + sd.name + " depends on it");
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
					data.error = e.toString();
				}
				finally {
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
		File tmp = createTempFile(repoDir, "mtp", ".whatever");
		tmp.deleteOnExit();
		try {
			copy(uri.toURL(), tmp);
			byte[] sha = SHA1.digest(tmp).digest();
			reporter.trace("SHA %s %s", uri, Hex.toHexString(sha));
			ArtifactData existing = get(sha);
			if (existing == null) {
				File meta = new File(repoDir, Hex.toHexString(sha) + ".json");
				File file = new File(repoDir, Hex.toHexString(sha));
				rename(tmp, file);
				existing = parse(file);
				existing.file = file.getAbsolutePath();
				existing.sha = sha;
				codec.enc().to(meta).put(existing);
			}
			xcopy(existing, data);
			reporter.trace("TD = " + data);
		}
		finally {
			tmp.delete();
		}
	}

	public ArtifactData get(byte[] sha) throws Exception {
		String name = Hex.toHexString(sha);
		File data = new File(repoDir, name + ".json");
		if (data.isFile()) {
			ArtifactData artifact = codec.dec().from(data).get(ArtifactData.class);
			artifact.file = new File(repoDir, name).getAbsolutePath();
			return artifact;
		} else
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
		StringBuilder sb = new StringBuilder(r.groupId).append(":").append(r.artifactId).append(":");
		if (r.classifier != null)
			sb.append(r.classifier).append("@");
		sb.append(r.version);

		return sb.toString();
	}

	public ArtifactData getCandidate(String key, boolean staged) throws Exception {
		reporter.trace("getCandidate " + key);
		// Short cut, see if we alread have it
		Matcher m = SHA_P.matcher(key);
		if (m.matches()) {
			byte[] sha = Hex.toByteArray(m.group(1));
			reporter.trace("sha " + key);
			ArtifactData art = get(sha);
			if (art != null)
				return art;

			reporter.trace("sha not in cache");
			Revision r = library.getRevision(sha);
			if (r != null) {
				reporter.trace("downloading sha");
				ArtifactData target = put(r.url);
				target.coordinates = key;
				return target;
			}
			reporter.trace("no sha found");
			// fall through
		}

		File f = new File(key);
		if (f.isFile()) {
			reporter.trace("is file");
			ArtifactData target = put(f.toURI());
			target.coordinates = f.getAbsolutePath();
			return target;
		}

		m = URL_P.matcher(key);
		if (m.matches()) {
			reporter.trace("looks like a url");
			try {
				ArtifactData target = put(new URI(key));
				target.coordinates = key;
				return target;
			}
			catch (Exception e) {
				// Ignore
				reporter.trace("hmm, not a valid url");
			}
			// fall through
		}

		reporter.trace("get the programs for %s", key);
		Iterable< ? extends Program> ps = library.getPrograms(key);
		if (ps == null)
			return null;

		Matcher matcher = COORD_P.matcher(key);
		String classifier = matcher.matches() ? matcher.group(3) : null;
		reporter.trace("filter for valid versions, classifier %s", classifier);
		List<RevisionRef> refs = new ArrayList<RevisionRef>();
		for (Program p : ps) {
			RevisionRef selected = selectBest(p.revisions, staged, classifier);
			if (selected != null)
				refs.add(selected);
		}
		if (refs.isEmpty()) {
			reporter.trace("no valid versions found");
			return null;
		}

		if (refs.size() == 1) {
			reporter.trace("found 1 program %s:%s", refs.get(0).groupId, refs.get(0).artifactId);
			RevisionRef r = refs.get(0);

			ArtifactData target = get(r.revision);
			if (target == null)
				target = put(r.url);

			target.coordinates = key;
			return target;
		}

		System.out.printf("Multiple candidates for this name, select with its sha\n");
		for (Program p : ps) {
			RevisionRef selected = selectBest(p.revisions, staged, classifier);
			if (selected != null) {
				String desc = selected.description;
				if (desc == null) {
					desc = p.wiki.text;
					if (desc == null)
						desc = "-";
				}

				System.out.printf("%-20s:%20s %-10s %s %s\n", selected.groupId, selected.artifactId, selected.version,
						Hex.toHexString(selected.revision), desc);
			}
		}
		return null;
	}

	private RevisionRef selectBest(List<RevisionRef> revisions, boolean staged, String classifier) {
		long date = 0;
		RevisionRef selected = null;
		Version selectedVersion = null;
		for (RevisionRef r : revisions) {
			reporter.trace("%s:%s:%s@%s %s", r.groupId, r.artifactId, r.classifier, r.version, classifier);
			if (r.classifier == null && classifier != null || r.classifier != null && classifier == null) {
				continue;
			}

			if (r.classifier == null || classifier == null || classifier.equals(r.classifier)) {
				if (r.phase == Phase.MASTER || (r.phase == Phase.STAGING && staged)) {
					Version v = toVersion(r.baseline,r.qualifier);
					if (selected == null || v.compareTo(selectedVersion) > 0) {
						selected = r;
						selectedVersion = v;
					}
						
				}
			}
		}
		if (selected == null) {
			reporter.trace("no candidate found");
		} else {
			reporter.trace("selected %s:%s:%s@%s", selected.groupId, selected.artifactId, selected.classifier,
					selected.version);
		}
		return selected;
	}

	private Version toVersion(String baseline, String qualifier) {
		if (qualifier == null || qualifier.trim().length() == 0) {
			return new Version(baseline);
		} else {
			return new Version(baseline+"."+qualifier);
		}
	}

	public static Executor getExecutor() {
		if (executor == null)
			executor = Executors.newFixedThreadPool(4);
		return executor;
	}

	public static void setExecutor(Executor executor) {
		JustAnotherPackageManager.executor = executor;
	}

	public void setLibrary(URI url) {
		library = new RemoteLibrary(url.toString());
	}

	public void close() {
		if (executor != null && executor instanceof ExecutorService)
			((ExecutorService) executor).shutdown();
	}

	public void init() throws IOException {
		URL s = getClass().getClassLoader().getResource("service.jar");
		IO.copy(s, service);
	}

	public List<Revision> getCandidates(String key) throws Exception {
		Iterable<Revision> revisions = library.getRevisions(key);
		List<Revision> revs = new ArrayList<Revision>();
		for (Revision r : revisions) {
			revs.add(r);
		}
		return revs;
	}

	public Iterable< ? extends Program> find(String q) throws Exception {
		return library.findProgram().query(q);
	}

	public void setHomeDir(File homeDir) throws IOException {
		this.homeDir = homeDir;
		initDirs();
	}

	public void setBinDir(File binDir) throws IOException {
		this.binDir = binDir;
		this.binDir.mkdirs();
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

	public RemoteLibrary getLibrary() {
		return library;
	}
	
}