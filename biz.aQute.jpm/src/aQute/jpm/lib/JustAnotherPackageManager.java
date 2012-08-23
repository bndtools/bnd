package aQute.jpm.lib;

import static aQute.lib.io.IO.*;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;

import aQute.bnd.build.*;
import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.version.*;
import aQute.jpm.platform.*;
import aQute.lib.base64.*;
import aQute.lib.data.*;
import aQute.lib.deployer.*;
import aQute.lib.io.*;
import aQute.lib.json.*;
import aQute.library.remote.*;
import aQute.service.library.Library.Revision;
import aQute.service.reporter.*;

/**
 * JPM is the Java package manager. It manages a local repository in the user
 * home directory and/or a global directory. This class is the main entry point
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

public class JustAnotherPackageManager extends FileRepo {
	public final static String	VERSION_PATTERN		= "[0-9]+(\\.[0-9]+(\\.[0-9]+(\\.[0-9A-Za-z_-]+)?)?)?";
	public final static String	BSN_PATTERN			= "[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*";
	public final static String	COMMAND_PATTERN		= "[\\w\\d]*";
	static Pattern				DIGEST_PATTERN		= Pattern.compile("([\\w\\d]+)-Digest", Pattern.CASE_INSENSITIVE);
	public final static String	MAINCLASS_PATTERN	= "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*";

	File						repoDir;
	File						homeDir;
	File						commandDir;
	File						serviceDir;
	Platform					platform;
	RemoteLibrary				library				= new RemoteLibrary(null);
	Reporter					reporter;
	final JSONCodec				codec				= new JSONCodec();

	public JustAnotherPackageManager(Reporter reporter) {
		super.setReporter(reporter);
		this.reporter = reporter;
		platform = Platform.getPlatform(reporter);
		homeDir = platform.getGlobal();
		if (!homeDir.exists() && !homeDir.mkdirs()) {
			throw new ExceptionInInitializerError("Could not create directory " + homeDir);
		}

		repoDir = IO.getFile(homeDir, "repo");
		if (!repoDir.exists() && !repoDir.mkdirs()) {
			throw new ExceptionInInitializerError("Could not create directory " + repoDir);
		}
		super.setDir(repoDir);

		commandDir = new File(homeDir, "commands");
		serviceDir = new File(homeDir, "service");
		commandDir.mkdir();
		serviceDir.mkdir();
	}

	public void setLibrary(URI url) {
		library = new RemoteLibrary(url.toString());
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
			result.add(data);
		}
		return result;
	}

	/**
	 * Garbage collect any service and commands.
	 * 
	 * @throws Exception
	 */
	public void gc() throws Exception {
		for (File cmd : commandDir.listFiles()) {
			CommandData data = getData(CommandData.class, cmd);
//			File repoFile = new File(data.repoFile);
//			if (!repoFile.isFile()) {
//				platform.remove(data);
//				cmd.delete();
//			}
		}

		for (File service : serviceDir.listFiles()) {
			File dataFile = new File(service, "data");

			ServiceData data = getData(ServiceData.class, dataFile);

//			if (!repoFile.isFile()) {
//				Service s = getService(service.getName());
//				s.stop();
//				if (data.work != null)
//					IO.delete(new File(data.work));
//				if (data.sdir != null)
//					IO.delete(new File(data.sdir));
//				if (data.log != null)
//					IO.delete(new File(data.log));
//				platform.remove(data);
//				IO.delete(service);
//			}
		}
	}

	/**
	 * Remove the JPM area.
	 * 
	 * @return
	 * @throws Exception
	 */

	public String deinit(boolean force) throws Exception {

		if (!repoDir.delete() && !force)
			return "Not empty";

		IO.delete(repoDir);
		gc();

		IO.delete(platform.getGlobal());
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
		
		new File(data.work).mkdir();
		
		if (data.log == null)
			data.log = new File(data.sdir, "log").getAbsolutePath();

		new File(data.log).mkdir();

		if (Data.validate(data) != null)
			return "Invalid service data: " + Data.validate(data);

		File service = get("biz.aQute.jpm.service", null, null); // get the
																	// latest
																	// version
		if (service == null)
			throw new RuntimeException(
					"Missing biz.aQute.jpm.service in repo, should have been installed by init, try reiniting");

		data.dependencies.add(service.getAbsolutePath());

		String s = platform.createService(data);
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
		if (Data.validate(data) != null)
			return "Invalid command data: " + Data.validate(data);
		String s = platform.createCommand(data);
		if (s == null)
			storeData(new File(commandDir, data.name), data);
		return s;
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
			System.out.println(IO.collect(dataFile));
			return null;
		}
	}

	private void storeData(File dataFile, Object o) throws Exception {
		codec.enc().to(dataFile).put(o);
	}

	public void setPlatform(Platform plf) {
		this.platform = plf;
	}

	public File get(String bsn, Version version, Map<String,String> properties, DownloadListener... listeners)
			throws Exception {
		if (version == null) {
			SortedSet<Version> versions = versions(bsn);
			if (versions.isEmpty())
				return null;
			version = versions.last();
		}
		File file = super.get(bsn, version, properties, listeners);
		if (file == null) {
			Revision revision = library.getRevision(bsn, version.toString());
			if (revision == null)
				return null;

			file = getLocal(bsn, version, properties);

			File tmp = IO.createTempFile(root, bsn, ".jar");
			URL url = revision.url.toURL();
			IO.copy(url, tmp);
			file.getParentFile().mkdirs();
			tmp.renameTo(file);
			if (!file.isFile()) {
				reporter.error("Could not rename %s to %s", tmp, file);
			}
			for (DownloadListener dl : listeners) {
				try {
					dl.success(file);
				}
				catch (Exception e) {
					reporter.exception(e, "Notifying download listener %s", dl);
				}
			}
		}
		return file;
	}

	public ArtifactData artifact(File source) throws Exception {
		assert source.isFile();

		JarFile jar = new JarFile(source);
		try {
			ArtifactData artifact = new ArtifactData();
			Manifest m = jar.getManifest();
			Attributes main = m.getMainAttributes();
			String bsn = main.getValue("Bundle-SymbolicName");
			if (bsn == null)
				reporter.error("The JAR does not have a name (Bundle-SymbolicName header)");
			artifact.bsn = bsn;
			
			String v = main.getValue("Bundle-Version");
			Version version = null;

			if (v == null)
				reporter.error("The JAR does not have a version (Bundle-Version header)");
			else if (!v.matches(JustAnotherPackageManager.VERSION_PATTERN))
				reporter.error("Not a valid version: %s", v);
			else
				version = new Version(v);
			artifact.version = version;
			
			String mainClass = main.getValue("Main-Class");

			reporter.trace("analyzing %s %s %s", bsn, version, mainClass);
			List<String> dependencies = new ArrayList<String>();

			{
				Parameters requires = OSGiHeader.parseHeader(main.getValue("JPM-Classpath"));
				List<DownloadBlocker> blockers = new ArrayList<DownloadBlocker>();

				for (Map.Entry<String,Attrs> e : requires.entrySet()) {
					String rbsn = e.getKey();
					String rv = e.getValue().get("version");
					if (Verifier.isVersion(rv)) {
						DownloadBlocker blocker = new DownloadBlocker(reporter);

						File f = get(rbsn, new Version(rv), null, blocker);
						if (f == null) {
							reporter.error("Cannot find class path dependency %s-%s", rbsn, rv);
						} else {
							blockers.add(blocker);
							dependencies.add(f.getAbsolutePath());
						}
					}
				}
				for (DownloadBlocker blocker : blockers) {
					artifact.reason = blocker.getReason();
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
					data.bsn = bsn;
					data.version = version;
					if (attrs.containsKey("args"))
						data.args = attrs.get("args");
					if (attrs.containsKey("jvmargs"))
						data.jvmArgs = attrs.get("jvmargs");

					data.main = mainClass;
					data.dependencies.add(source.getAbsolutePath());
					data.dependencies.addAll(dependencies);
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
					data.bsn = bsn;
					data.version = version;
					data.name = e.getKey();
					data.jvmArgs = attrs.get("vmargs");
					data.main = mainClass;
					data.dependencies.add(source.getAbsolutePath());
					data.dependencies.addAll(dependencies);
					artifact.command = data;
				}
				reporter.trace("commands %s", artifact.command);
			}

			artifact.verify = verify(jar);
			reporter.trace("returning " + artifact);
			return artifact;
		}
		finally {
			jar.close();
		}

	}

	public ArtifactData artifact(String bsn, Version version) throws Exception {
		File f = get(bsn, version, null);
		if (f == null)
			return null;

		return artifact(f);

	}
}
