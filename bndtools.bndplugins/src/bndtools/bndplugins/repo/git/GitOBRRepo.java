package bndtools.bndplugins.repo.git;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;

import javax.xml.transform.stream.StreamResult;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.osgi.service.bindex.BundleIndexer;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import aQute.lib.deployer.FileRepo;
import aQute.lib.deployer.obr.AbstractBaseOBR;
import aQute.lib.deployer.obr.UniqueResourceFilter;
import aQute.lib.io.IO;
import aQute.lib.osgi.Jar;
import aQute.libg.glob.Glob;
import aQute.libg.reporter.Reporter;
import aQute.libg.sax.SAXUtil;
import aQute.libg.sax.filters.MergeContentFilter;

public class GitOBRRepo extends AbstractBaseOBR {

	public static final String PROP_LOCAL_DIR = "local";
	public static final String PROP_LOCAL_SUB_DIR = "sub";
	public static final String PROP_READONLY = "readonly";
	public static final String PROP_GIT_REPO_XML_URI = "git-repo-xml-uri";
	public static final String PROP_GIT_URI = "git-uri";
	public static final String PROP_GIT_PUSH_URI = "git-push-uri";
	public static final String PROP_GIT_BRANCH = "git-branch";
	
	private static final String CONFIG_FILE_LIST = "configs";
	private static final String PREFIX_PATTERN = "pattern.";
	private static final String PREFIX_USER = "uid.";
	private static final String PREFIX_PASSWORD = "pwd.";

	protected final FileRepo storageRepo = new FileRepo();

	protected File gitRootDir;
	protected File storageDir;
	protected File localIndex;

	protected List<URL> indexUrls;

	protected Repository repository;
	
	protected String gitUri;
	protected String gitPushUri;
	protected String gitBranch;
	protected String gitRepoXmlUri;

	protected String configFileList;
	
	private final AtomicBoolean configFileInited = new AtomicBoolean(false);
	private final List<Mapping> mappings = Collections.synchronizedList(new LinkedList<Mapping>());
	
	@Override
	public void setReporter(Reporter reporter) {
		super.setReporter(reporter);
		storageRepo.setReporter(reporter);
	}

	@Override
	public synchronized void setProperties(Map<String, String> map) {
		super.setProperties(map);

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
		
		// Load essential properties
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
		if (localSubDirPath != null) {
			storageDir = new File(gitRootDir, localSubDirPath);
			if (!storageDir.exists()) {
				storageDir.mkdirs();
			}
		} else {
			storageDir = gitRootDir;
		}
		
		
		// Configure the storage repository
		Map<String, String> storageRepoConfig = new HashMap<String, String>(2);
		storageRepoConfig.put(FileRepo.LOCATION, storageDir.toString());
		storageRepoConfig.put(FileRepo.READONLY, map.get(PROP_READONLY));
		storageRepo.setProperties(storageRepoConfig);

		// Set the local index location
		localIndex = new File(storageDir, REPOSITORY_FILE_NAME);
		if (localIndex.exists() && !localIndex.isFile())
			throw new IllegalArgumentException(
					String.format(
							"Cannot build local repository index: '%s' already exists but is not a plain file.",
							localIndex.getAbsolutePath()));

	}

	private static String escape(String url) {
		url = url.replace('/', '-');
		url = url.replace(':', '-');
		url = url.replace('@', '-');
		url = url.replace('\\', '-');
		return url;
	}

	@Override
	public synchronized File put(Jar jar) throws Exception {
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
			
			newFile = storageRepo.put(jar);
			
			// Index the new file
			BundleIndexer indexer = registry.getPlugin(BundleIndexer.class);
			if (indexer == null)
				throw new IllegalStateException(
						"Cannot index repository: no Bundle Indexer service or plugin found.");
			ByteArrayOutputStream newIndexBuffer = new ByteArrayOutputStream();
			Map<String,String> config = new HashMap<String, String>();
			config.put(BundleIndexer.ROOT_URL, gitRootDir.getCanonicalFile().toURI().toURL().toString());
			indexer.index(Collections.singleton(newFile), newIndexBuffer, config);
	
			// Merge into main index
			File tempIndex = File.createTempFile("repository", ".xml");
			FileOutputStream tempIndexOutput = new FileOutputStream(tempIndex);
			MergeContentFilter merger = new MergeContentFilter();
			XMLReader reader = SAXUtil.buildPipeline(new StreamResult(
					tempIndexOutput), new UniqueResourceFilter(), merger);
	
			try {
				// Parse the newly generated index
				reader.parse(new InputSource(new ByteArrayInputStream(
						newIndexBuffer.toByteArray())));
	
				// Parse the existing index (which may be empty/missing)
				try {
					reader.parse(new InputSource(new FileInputStream(localIndex)));
				} catch (Exception e) {
					reporter.warning(
							"Existing local index is invalid or missing, overwriting (%s).",
							localIndex.getAbsolutePath());
				}
	
				merger.closeRootAndDocument();
			} finally {
				tempIndexOutput.flush();
				tempIndexOutput.close();
			}
			IO.copy(tempIndex, localIndex);
	
			// Add, Commit and Push
			git.add().addFilepattern(getRelativePath(gitRootDir, newFile)).addFilepattern(getRelativePath(gitRootDir, localIndex)).call();
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

	public File getCacheDirectory() {
		return null;
	}

	@Override
	public boolean canWrite() {
		return storageRepo.canWrite();
	}
	
	public String getLocation() {
		return gitUri;
	}

	private static String getRelativePath(File base, File file) throws IOException {
		return base.toURI().relativize(file.toURI()).getPath();
	}

	public Collection<URL> getOBRIndexes() throws IOException {
		try {
			indexUrls = new ArrayList<URL>();
			if (localIndex.exists()) {
				indexUrls.add(localIndex.toURI().toURL());
			}
			if (gitRepoXmlUri != null) {
				URL gitRepo = new URL(gitRepoXmlUri);
				indexUrls.add(gitRepo);
			}
			return indexUrls;
		} catch (IOException e) {
			throw new IllegalArgumentException("Error initialising local index URL", e);
		}
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
		String pass;
		Mapping(Glob urlPattern, String user, String pass) {
			this.urlPattern = urlPattern; this.user = user; this.pass = pass;
		}

	}

}