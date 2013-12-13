package aQute.bnd.deployer.repository.aether;

import static aQute.bnd.deployer.repository.RepoConstants.DEFAULT_CACHE_DIR;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.DefaultServiceLocator.ErrorHandler;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RemoteRepository.Builder;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferEvent.RequestType;
import org.eclipse.aether.transfer.TransferResource;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import aQute.bnd.deployer.repository.FixedIndexedRepo;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.service.IndexProvider;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.ResolutionPhase;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.service.reporter.Reporter;

public class AetherRepository implements Plugin, RegistryPlugin, RepositoryPlugin, IndexProvider {

	public static final String PROP_NAME = "name";
	public static final String PROP_MAIN_URL = "url";
	public static final String PROP_INDEX_URL = "indexUrl";
	public static final String PROP_USERNAME = "username";
	public static final String PROP_PASSWORD = "password";
	public static final String PROP_CACHE = "cache";

	private static final String	META_OBR	= ".meta/obr.xml";

	private Reporter reporter;
	private Registry registry;
	
	// Config Properties
	private String name = this.getClass().getSimpleName();
	private URI mainUri;
	private URI indexUri;
	private File cacheDir = new File(System.getProperty("user.home")+ File.separator + DEFAULT_CACHE_DIR);
	private String username = null;
	private String password = "";

	// Initialisation Fields
	private boolean initialised;
	private RepositorySystem repoSystem;
	private RemoteRepository remoteRepo;
	private LocalRepository localRepo;
	private FixedIndexedRepo indexedRepo;

	@Override
	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}
	
	@Override
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}
	
	@Override
	public void setProperties(Map<String, String> props) throws Exception {
		// Read name property
		if (props.containsKey(PROP_NAME))
			name = props.get(PROP_NAME);

		// Read main Nexus URL property
		String mainUrlStr = props.get(PROP_MAIN_URL);
		if (mainUrlStr == null)
			throw new IllegalArgumentException(String.format("Attribute '%s' must be set on '%s' plugin.", PROP_MAIN_URL, getClass().getName()));
		try {
			if (mainUrlStr.endsWith("/"))
				mainUrlStr = mainUrlStr.substring(0, mainUrlStr.length() - 1);
			mainUri = new URI(mainUrlStr);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(String.format("Invalid '%s' property in plugin %s", PROP_MAIN_URL), e);
		}

		// Read index URL property if present
		String indexUriStr = props.get(PROP_INDEX_URL);
		if (indexUriStr == null) {
			indexUri = findDefaultVirtualIndexUri(mainUri);
		} else {
			try {
				indexUri = new URI(indexUriStr);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(String.format("Invalid '%s' property in plugin %s", PROP_INDEX_URL), e);
			}
		}

		// Read username and password
		if (props.containsKey(PROP_USERNAME))
			username = props.get(PROP_USERNAME);
		if (props.containsKey(PROP_PASSWORD))
			password = props.get(PROP_PASSWORD);

		// Read cache path property
		String cachePath = props.get(PROP_CACHE);
		if (cachePath != null) {
			cacheDir = new File(cachePath);
			if (!cacheDir.isDirectory()) {
				String canonicalPath;
				try {
					canonicalPath = cacheDir.getCanonicalPath();
				} catch (IOException e) {
					throw new IllegalArgumentException(String.format("Could not canonical path for cacheDir '%s'.", cachePath), e);
				}
				throw new IllegalArgumentException(String.format("Cache path '%s' does not exist, or is not a directory", canonicalPath));
			}
		}
	}
	
	protected final synchronized void init() throws Exception {
		if (initialised)
			return;

		// Initialise Aether
		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);
		locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
		locator.addService(TransporterFactory.class, FileTransporterFactory.class);
		locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
		locator.setErrorHandler(new ErrorHandler() {
			@Override
			public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
				if (reporter != null)
					reporter.error("Service creation failed for type %s using impl %s: %s", type, impl, exception.getLocalizedMessage());
				exception.printStackTrace();
			}
		});
		repoSystem = locator.getService(RepositorySystem.class);
		if (repoSystem == null)
			throw new IllegalArgumentException("Failed to initialise Aether repository system");
		
		Builder builder = new RemoteRepository.Builder("remote", "default", mainUri.toString());
		if (username != null) {
			AuthenticationBuilder authBuilder = new AuthenticationBuilder().addUsername(username);
			if (password != null)
				authBuilder.addPassword(password);
			builder.setAuthentication(authBuilder.build());
		}
		remoteRepo = builder.build();
		localRepo = new LocalRepository(new File(cacheDir, "aether-local"));
		
		// Initialise Index
		if (indexUri == null) {
			indexedRepo = null;
		} else {
			// Test whether the index URI exists and is available.
			HttpURLConnection connection = (HttpURLConnection) indexUri.toURL().openConnection();
			try {
				connection.setRequestMethod("HEAD");
				int responseCode = connection.getResponseCode();
				if (responseCode >= 400) {
					indexedRepo = null;
				} else {
					indexedRepo = new FixedIndexedRepo();
					Map<String, String> config = new HashMap<String, String>();
					indexedRepo.setReporter(this.reporter);
					indexedRepo.setRegistry(registry);
					
					config.put(FixedIndexedRepo.PROP_CACHE, cacheDir.getAbsolutePath());
					config.put(FixedIndexedRepo.PROP_LOCATIONS, indexUri.toString());
					indexedRepo.setProperties(config);
				}
			} finally {
				connection.disconnect();
			}
		}

		initialised = true;
	}
	
	public String[] getGroupAndArtifactForBsn(String bsn) {
		int dotIndex = bsn.lastIndexOf('.');
		if (dotIndex < 0)
			throw new IllegalArgumentException("Cannot split bsn into group and artifact IDs: " + bsn);
		String groupId = bsn.substring(0, dotIndex);
		String artifactId = bsn.substring(dotIndex + 1);

		return new String[] { groupId, artifactId };
	}

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		init();

		DigestInputStream digestStream = new DigestInputStream(stream, MessageDigest.getInstance("SHA-1"));
		final File tmpFile = IO.createTempFile(cacheDir, "put", ".bnd");
		try {
			IO.copy(digestStream, tmpFile);
			byte[] digest = digestStream.getMessageDigest().digest();
			
			if (options.digest != null && !Arrays.equals(options.digest, digest))
				throw new IOException("Retrieved artifact digest doesn't match specified digest");
			
			// Get basic info about the bundle we're deploying
			Jar jar = new Jar(tmpFile);
			String bsn;
			String groupId;
			String artifactId;
			Version version;
			try {
				bsn = jar.getBsn();
				if (bsn == null || !Verifier.isBsn(bsn))
					throw new IllegalArgumentException("Jar does not have a valid Bundle-SymbolicName manifest header");
				String versionString = jar.getVersion();
				if (versionString == null)
					versionString = "0";
				else if (!Verifier.isVersion(versionString))
					throw new IllegalArgumentException("Invalid version " + versionString + " in file " + tmpFile);
				version = Version.parseVersion(versionString);
				String[] coords = getGroupAndArtifactForBsn(bsn);
				groupId = coords[0];
				artifactId = coords[1];
			} finally {
				jar.close();
			}
			MavenProjectVersion projectVersion = new MavenProjectVersion(version);
	
			// Setup the Aether repo session and create the deployment request
			DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
			session.setLocalRepositoryManager(repoSystem.newLocalRepositoryManager(session, localRepo));
			Artifact artifact = new DefaultArtifact(groupId, artifactId, "jar", projectVersion.toString()).setFile(tmpFile);
			final DeployRequest request = new DeployRequest();
			request.addArtifact(artifact);
			request.setRepository(remoteRepo);
			
			// Capture the result including remote resource URI
			final ResultHolder resultHolder = new ResultHolder();
			session.setTransferListener(new AbstractTransferListener() {
				@Override
				public void transferSucceeded(TransferEvent event) {
					TransferResource resource = event.getResource();
					if (event.getRequestType() == RequestType.PUT && tmpFile.equals(resource.getFile())) {
						PutResult result = new PutResult();
						result.artifact = URI.create(resource.getRepositoryUrl() + resource.getResourceName());
						resultHolder.result = result;
						System.out.println("UPLOADED to: " + URI.create(resource.getRepositoryUrl() + resource.getResourceName()));
					}
				}
				@Override
				public void transferFailed(TransferEvent event) {
					if (event.getRequestType() == RequestType.PUT && tmpFile.equals(event.getResource().getFile()))
						resultHolder.error = event.getException();
				}
				@Override
				public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
					if (event.getRequestType() == RequestType.PUT && tmpFile.equals(event.getResource().getFile()))
						resultHolder.error = event.getException();
				}
			});
			
			// Do the deploy and report results
			repoSystem.deploy(session, request);
			
			// Reload the index
			if (indexedRepo != null)
				indexedRepo.reset();
			
			if (resultHolder.result != null)
				return resultHolder.result;
			else if (resultHolder.error != null)
				throw new Exception("Error during artifact upload", resultHolder.error);
			else
				throw new Exception("Artifact was not uploaded");

		} finally {
			if (tmpFile != null && tmpFile.isFile())
				IO.delete(tmpFile);
		}
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		init();

		// only supported with a valid index
		return indexedRepo != null ? indexedRepo.list(pattern) : null;
	}
	
	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		init();

		// Use the index by preference
		if (indexedRepo != null)
			return indexedRepo.versions(bsn);

		// Setup the Aether repo session and create the range request
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		session.setLocalRepositoryManager(repoSystem.newLocalRepositoryManager(session, localRepo));
		String[] coords = getGroupAndArtifactForBsn(bsn);
		Artifact artifact = new DefaultArtifact(coords[0], coords[1], "jar", "[0,)");
		VersionRangeRequest rangeRequest = new VersionRangeRequest();
		rangeRequest.setArtifact(artifact);
		rangeRequest.setRepositories(Collections.singletonList(remoteRepo));
		
		// Resolve the range
		VersionRangeResult rangeResult = repoSystem.resolveVersionRange(session, rangeRequest);
		
		// Add to the result
		SortedSet<Version> versions = new TreeSet<Version>();
		for (org.eclipse.aether.version.Version version : rangeResult.getVersions()) {
			MavenProjectVersion parsed = MavenProjectVersion.parseString(version.toString());
			versions.add(parsed.getOSGiVersion());
		}
		return versions;
	}
	
	@Override
	public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners) throws Exception {
		init();
		
		// Use the index by preference
		if (indexedRepo != null)
			return indexedRepo.get(bsn, version, properties, listeners);
		
		File file = null;
		try {
			// Setup the Aether repo session and request
			DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
			session.setLocalRepositoryManager(repoSystem.newLocalRepositoryManager(session, localRepo));
			String[] coords = getGroupAndArtifactForBsn(bsn);
			
			MavenProjectVersion mvnVersion = new MavenProjectVersion(version);
			Artifact artifact = new DefaultArtifact(coords[0], coords[1], "jar", mvnVersion.toString());
			ArtifactRequest request = new ArtifactRequest();
			request.setArtifact(artifact);
			request.setRepositories(Collections.singletonList(remoteRepo));
			
			// Log the transfer
			session.setTransferListener(new AbstractTransferListener() {
				@Override
				public void transferStarted(TransferEvent event) throws TransferCancelledException {
					System.err.println(event);
				}
				@Override
				public void transferSucceeded(TransferEvent event) {
					System.err.println(event);
				}
				@Override
				public void transferFailed(TransferEvent event) {
					System.err.println(event);
				}
			});
			
			// Resolve the version
			ArtifactResult artifactResult = repoSystem.resolveArtifact(session, request);
			artifact = artifactResult.getArtifact();
			file = artifact.getFile();
			
			return file;
		} finally {
			for (DownloadListener dl : listeners) {
				if (file != null)
					dl.success(file);
				else
					dl.failure(null, "Download failed");
			}
		}
	}

	@Override
	public boolean canWrite() {
		return true;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getLocation() {
		return "http://localhost:8080/nexus/content/repositories/scratch1";
	}
	
	/**
	 * <p>
	 * Find the default URI of the OBR index for a hosted repository, assuming
	 * that the OBR plugin is used to generate a Virtual repository with the
	 * same name as the referenced repository with the addition of "-obr" to its
	 * name.
	 * </p>
	 * <p>
	 * For example suppose there is a hosted repository with the identity
	 * "releases"; it will have the URI
	 * {@code http://hostname/nexus/content/repositories/releases}. We assume
	 * there is a Virtual OBR repository with ID "releases-obr". It will have
	 * the URL {@code http://hostname/nexus/content/shadows/releases-obr}, and
	 * the OBR index will be at
	 * {@code http://hostname/nexus/content/shadows/releases-obr/.meta/obr.xml}.
	 * 
	 * @param hostedUri
	 *            The URI of the source Hosted repository.
	 * @return
	 */
	private static URI findDefaultVirtualIndexUri(URI hostedUri) {
		String repoName;
		
		String path = hostedUri.getPath();
		int slashPosition = path.lastIndexOf('/');
		if (slashPosition < 0)
			repoName = path;
		else
			repoName = path.substring(slashPosition + 1);
		
		return hostedUri.resolve("../shadows/" + repoName + "-obr/" + META_OBR);
	}
	
	@Override
	public List<URI> getIndexLocations() throws Exception {
		init();
		return indexedRepo != null ? indexedRepo.getIndexLocations() : Collections.<URI>emptyList();
	}
	
	@Override
	public Set<ResolutionPhase> getSupportedPhases() {
		return EnumSet.allOf(ResolutionPhase.class);
	}
	
	static class ResultHolder {
		RepositoryPlugin.PutResult result;
		Exception error;
	}

}
