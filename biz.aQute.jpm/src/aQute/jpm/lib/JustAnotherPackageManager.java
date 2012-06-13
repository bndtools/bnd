package aQute.jpm.lib;

import static aQute.lib.io.IO.*;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;

import aQute.jpm.platform.*;
import aQute.lib.base64.*;
import aQute.lib.data.*;
import aQute.lib.io.*;
import aQute.lib.json.*;
import aQute.libg.filerepo.*;
import aQute.libg.reporter.*;
import aQute.libg.version.*;

/**
 * JPM is the Java package manager. It manages a local repository in the user
 * home directory and/or a global directory. This class is the main entry point
 * for the command line. This program maintains a repository, a list of
 * installed commands, and a list of installed services. It provides the
 * commands to changes these resources. All information is kept in a platform
 * specific area. However, the layout of this area is standardized.
 * 
 * <pre>
 * 	platform/
 *      check									check for write access
 *      repo/                              		repository
 *        &lt;bsn&gt;/                     		bsn directory
 *          &lt;bsn&gt;-&lt;version&gt;.jar		jar file
 *      services/                               All services
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
	FileRepo					repo;
	Reporter					reporter;
	final JSONCodec				codec				= new JSONCodec();

	public JustAnotherPackageManager(Reporter reporter) {
		this.reporter = reporter;
		platform = Platform.getPlatform(reporter);
		homeDir = platform.getGlobal();
		homeDir.mkdirs();

		repoDir = IO.getFile(homeDir, "repo");
		repoDir.mkdirs();
		repo = new FileRepo(repoDir);

		commandDir = new File(homeDir, "commands");
		serviceDir = new File(homeDir, "services");
		commandDir.mkdir();
		serviceDir.mkdir();

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

	public List<ArtifactData> getArtifacts() throws Exception {
		List<String> list = repo.list(null);
		List<ArtifactData> result = new ArrayList<ArtifactData>();
		for (String bsn : list) {
			ArtifactData ad = new ArtifactData();
			ad.bsn = bsn;
			ad.version = repo.versions(bsn);
			result.add(ad);
		}
		return result;
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

	public void uninstall(String bsn, VersionRange range) throws Exception {
		File[] files = repo.get(bsn, range);
		if (files == null || files.length == 0) {
			reporter.error("No artifact found for %s:%s", bsn, range);
		} else {
			for (File file : files) {
				file.delete();
				file.getParentFile().delete();
			}
		}
	}

	/**
	 * Garbage collect any services and commands.
	 * 
	 * @throws Exception
	 */
	public void gc() throws Exception {
		for (File cmd : commandDir.listFiles()) {
			CommandData data = getData(CommandData.class, cmd);

			if (!data.repoFile.isFile()) {
				platform.remove(data);
				cmd.delete();
			}
		}

		for (File service : serviceDir.listFiles()) {
			File dataFile = new File(service, "data");

			ServiceData data = getData(ServiceData.class, dataFile);

			if (!data.repoFile.isFile()) {
				Service s = getService(service.getName());
				s.stop();
				delete(data.work);
				delete(data.sdir);
				delete(data.log);
				platform.remove(data);
				delete(service);
			}
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

		delete(repoDir);
		gc();

		delete(platform.getGlobal());
		return null;
	}

	public File install(File source, String bsn, Version version) throws IOException {
		File target = repo.put(bsn, version);
		copy(source, target);
		return target;
	}

	/**
	 * @param data
	 * @param target
	 * @throws Exception
	 * @throws IOException
	 */
	public String createService(ServiceData data) throws Exception, IOException {

		data.sdir = new File(serviceDir, data.name);
		data.sdir.mkdirs();
		data.lock = new File(data.sdir, "lock");
		if (data.work == null)
			data.work = new File(data.sdir, "work");
		data.work.mkdir();
		if (data.log == null)
			data.log = new File(data.sdir, "log");

		data.log.mkdir();

		if (Data.validate(data) != null)
			return "Invalid service data: " + Data.validate(data);

		File service = repo.get("biz.aQute.jpm.service", null, 1);
		if (service == null)
			throw new RuntimeException(
					"Missing biz.aQute.jpm.service in repo, should have been installed by init, try reiniting");

		data.path = data.repoFile.getAbsolutePath() + File.pathSeparator + service.getAbsolutePath();

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
	public String verify(JarFile jar, String[] algorithms) throws IOException {
		if (algorithms == null)
			algorithms = new String[] {
					"MD5", "SHA1"
			};
		else if (algorithms.length == 1 && algorithms[0].equals("-"))
			return null;

		try {
			Manifest m = jar.getManifest();
			if (m.getEntries().isEmpty())
				return "No name sections";

			for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
				JarEntry je = e.nextElement();
				if (je.getName().equals("META-INF/MANIFEST.MF") || je.getName().endsWith(".SF")
						|| je.getName().endsWith("/"))
					continue;

				Attributes nameSection = m.getAttributes(je.getName());
				if (nameSection == null)
					return "No name section for " + je.getName();

				for (String algorithm : algorithms) {
					try {
						MessageDigest md = MessageDigest.getInstance(algorithm);
						String expected = nameSection.getValue(algorithm + "-Digest");
						byte digest[] = Base64.decodeBase64(expected);
						copy(jar.getInputStream(je), md);
						if (!Arrays.equals(digest, md.digest()))
							return "Invalid digest for " + je.getName() + ", " + expected + " != "
									+ Base64.encodeBase64(md.digest());

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
		return codec.dec().from(dataFile).get(clazz);
	}

	private void storeData(File dataFile, Object o) throws Exception {
		codec.enc().to(dataFile).put(o);
	}

	public void setPlatform(Platform plf) {
		this.platform = plf;
	}

	public File getArtifact(String bsn, Version version) throws Exception {
		return repo.get(bsn, new VersionRange(version.toString()), 1);
	}

}
