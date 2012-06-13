package bndtools.bndplugins.repo.git;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;

import aQute.lib.deployer.repository.LocalIndexedRepo;
import aQute.lib.deployer.repository.api.IRepositoryContentProvider;
import aQute.lib.io.IO;
import aQute.lib.osgi.Jar;
import aQute.libg.glob.Glob;

public class GitOBRRepo extends LocalIndexedRepo {

	public static final String PROP_LOCAL_SUB_DIR = "sub";
	public static final String PROP_GIT_REPO_XML_URI = "git-repo-xml-uri";
	public static final String PROP_GIT_URI = "git-uri";
	public static final String PROP_GIT_PUSH_URI = "git-push-uri";
	public static final String PROP_GIT_BRANCH = "git-branch";

	private static final String CONFIG_FILE_LIST = "configs";
	private static final String PREFIX_PATTERN = "pattern.";
	private static final String PREFIX_USER = "uid.";
	private static final String PREFIX_PASSWORD = "pwd.";

	private boolean pretty = false;

	protected File gitRootDir;

	protected Repository repository;

	protected String gitUri;
	protected String gitPushUri;
	protected String gitBranch;
	protected String gitRepoXmlUri;

	protected String configFileList;

	private final AtomicBoolean configFileInited = new AtomicBoolean(false);
	private final List<Mapping> mappings = Collections.synchronizedList(new LinkedList<Mapping>());

	@Override
	public synchronized void setProperties(Map<String, String> map) {

		configFileList = map.get(CONFIG_FILE_LIST);

		gitUri = map.get(PROP_GIT_URI);
		if (gitUri == null)
			throw new IllegalArgumentException(String.format(
					"Attribute '%s' must be set on GitOBRRepo plugin.",
					PROP_GIT_URI));

		gitRepoXmlUri = map.get(PROP_GIT_REPO_XML_URI);

		gitPushUri = map.get(PROP_GIT_PUSH_URI);
		if (gitPushUri == null)
			throw new IllegalArgumentException(String.format(
					"Attribute '%s' must be set on GitOBRRepo plugin.",
					PROP_GIT_PUSH_URI));

		gitBranch = map.get(PROP_GIT_BRANCH);
		if (gitBranch == null)
			throw new IllegalArgumentException(String.format(
					"Attribute '%s' must be set on GitOBRRepo plugin.",
					PROP_GIT_BRANCH));

		String localDirPath = map.get(PROP_LOCAL_DIR);
		if (localDirPath == null) {
			localDirPath = System.getProperty("user.home") + "/.gitobrrepo/" + escape(gitUri);
			if (localDirPath.endsWith(Constants.DOT_GIT)) {
				localDirPath = localDirPath.substring(0, localDirPath.length() - Constants.DOT_GIT.length());
			}
			File file = new File(localDirPath);
			if (!file.exists()) {
				file.mkdirs();
			}
		}

		gitRootDir = new File(localDirPath);

		if (!gitRootDir.isDirectory())
			throw new IllegalArgumentException(String.format(
					"Local path '%s' does not exist or is not a directory.",
					localDirPath));

		CredentialsProvider.setDefault(new GitCredentialsProvider(this));

		try {
			repository = GitUtils.getRepository(gitRootDir, gitBranch, gitUri, gitPushUri);
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format(
					"Cannot setup GIT repository: '%s' - %s",
					gitRootDir.getAbsolutePath(), e.getMessage()), e);
		}

		String localSubDirPath = map.get(PROP_LOCAL_SUB_DIR);
		File dir;
		if (localSubDirPath != null) {
			dir = new File(gitRootDir, localSubDirPath);
			if (!dir.exists()) {
				dir.mkdirs();
			}
		} else {
			dir = gitRootDir;
		}
		map.put(PROP_LOCAL_DIR, dir.getAbsolutePath());

		super.setProperties(map);

		pretty = "true".equalsIgnoreCase(map.get(PROP_PRETTY));

	}

	
	
	private static String escape(String url) {
		String urli = url.replace('/', '-');
		urli = urli.replace(':', '-');
		urli = urli.replace('@', '-');
		urli = urli.replace('\\', '-');
		return urli;
	}

	@Override
	public synchronized File put(Jar jar) throws Exception {
		init();

		File newFile = null;

		try {
			repository.incrementOpen();

			Git git = Git.wrap(repository);

			// Pull remote repository
			PullResult pullResult = git.pull().call();

			// Check result
			if (pullResult.getMergeResult().getMergeStatus() == MergeStatus.CONFLICTING || 
					pullResult.getMergeResult().getMergeStatus() ==  MergeStatus.FAILED) {

				//TODO: How to report failure
					throw new RuntimeException(String.format("Failed to merge changes from %s", gitUri));
			}

			//TODO: Check if jar already exists, is it ok to overwrite in all repositories?

			newFile = super.put(jar);

			// Add, Commit and Push
			for (IRepositoryContentProvider provider : generatingProviders) {
				if (!provider.supportsGeneration())
					continue;
				git.add().addFilepattern(getRelativePath(gitRootDir, newFile)).addFilepattern(getRelativePath(gitRootDir, new File(provider.getDefaultIndexName(pretty)))).call();
			}
			git.commit().setMessage("bndtools added bundle : " + getRelativePath(gitRootDir, newFile)).call();
			git.push().setCredentialsProvider(CredentialsProvider.getDefault()).call();

			// Re-read the index
			reset();
			init();
		} finally {
			if (repository != null) {
				repository.close();
			}
		}
		return newFile;
	}

	public String getLocation() {
		return gitUri;
	}

	public synchronized File getCacheDirectory() {
		return null;
	}

	private static String getRelativePath(File base, File file) throws IOException {
		return base.toURI().relativize(file.toURI()).getPath();
	}
	
	@Override
	protected synchronized List<URI> loadIndexes() throws Exception {
		List<URI> indexes = super.loadIndexes();
		if (gitRepoXmlUri != null) {
			URI gitRepo = new URI(gitRepoXmlUri);
			indexes.add(gitRepo);
		}
		return indexes;
	}

	protected void configFileInit() {
		if (configFileList == null) {
			return;
		}
		if (configFileInited.compareAndSet(false, true)) {
			mappings.clear();

			StringTokenizer tokenizer = new StringTokenizer(configFileList, ",");
			while (tokenizer.hasMoreTokens()) {
				String configFileName = tokenizer.nextToken().trim();

				File file = new File(configFileName);
				if (file.exists()) {
					Properties props = new Properties();
					InputStream stream = null;
					try {
						stream = new FileInputStream(file);
						props.load(stream);

						for (Object key : props.keySet()) {
							String name = (String) key;

							if (name.startsWith(PREFIX_PATTERN)) {
								String id = name.substring(PREFIX_PATTERN.length());

								Glob glob = new Glob(props.getProperty(name));
								String uid = props.getProperty(PREFIX_USER + id);
								String pwd = props.getProperty(PREFIX_PASSWORD + id);

								mappings.add(new Mapping(glob, uid, pwd));
							}
						}
					} catch (IOException e) {
						reporter.error("Failed to load %s", configFileName);
					} finally {
						if (stream != null) IO.close(stream);
					}
				}
			}
		}
	}

	public boolean containsMappings() {
		configFileInit();
		return mappings.size() > 0;
	}

	public Mapping findMapping(String url) {
		configFileInit();
		for (Mapping mapping : mappings) {
			Matcher matcher = mapping.urlPattern.matcher(url);
			if (matcher.find())
				return mapping;
		}
		return null;
	}

	void addMapping(Mapping mapping) {
		mappings.add(mapping);
	}

	static class Mapping {
		Glob urlPattern;
		String user;
		char[] pass;
		Mapping(Glob urlPattern, String user, String pass) {
			this.urlPattern = urlPattern;
			this.user = user;
			if (pass != null) {
				this.pass = pass.toCharArray();
			} else {
				this.pass = new char[0];
			}
		}
	}
}