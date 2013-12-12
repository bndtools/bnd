package aQute.bnd.deployer.repository.aether;

import static aQute.bnd.deployer.repository.RepoConstants.DEFAULT_CACHE_DIR;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

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

import aQute.bnd.deployer.repository.nexus.MavenProjectVersion;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.service.reporter.Reporter;

public class AetherRepository implements Plugin, RegistryPlugin, RepositoryPlugin {

	public static final String PROP_NAME = "name";
	public static final String PROP_CACHE = "cache";

	private Reporter reporter;
	private Registry registry;
	
	// Config Properties
	private String name = this.getClass().getSimpleName();
	private File cacheDir = new File(System.getProperty("user.home")+ File.separator + DEFAULT_CACHE_DIR);

	// Initialisation Fields
	private boolean initialised;
	private RepositorySystem repoSystem;
	private RemoteRepository repo;

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
		if (props.containsKey(PROP_NAME))
			name = props.get(PROP_NAME);

		// Read cache path property
		String cachePath = props.get(PROP_CACHE);
		if (cachePath != null) {
			cacheDir = new File(cachePath);
			if (!cacheDir.isDirectory()) {
				String canonicalPath;
				try {
					canonicalPath = cacheDir.getCanonicalPath();
				}
				catch (IOException e) {
					throw new IllegalArgumentException(String.format("Could not canonical path for cacheDir '%s'.", cachePath), e);
				}
				throw new IllegalArgumentException(String.format("Cache path '%s' does not exist, or is not a directory", canonicalPath));
			}
		}
	}
	
	protected final synchronized void init() {
		if (initialised)
			return;
		
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
		
		Builder builder = new RemoteRepository.Builder("nexus", "default", "http://localhost:8080/nexus/content/repositories/scratch1");
		builder.setAuthentication(new AuthenticationBuilder().addUsername("admin").addPassword("admin123").build());
		repo = builder.build();
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
				
				int dotIndex = bsn.lastIndexOf('.');
				if (dotIndex < 0)
					throw new IllegalArgumentException("Cannot split bsn into group and artifact IDs: " + bsn);
				groupId = bsn.substring(0, dotIndex);
				artifactId = bsn.substring(dotIndex + 1);
			} finally {
				jar.close();
			}
			MavenProjectVersion projectVersion = new MavenProjectVersion(version, true);
			
			DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
			LocalRepository localRepo = new LocalRepository("generated/local-repo");
			
			session.setLocalRepositoryManager(repoSystem.newLocalRepositoryManager(session, localRepo));
			
			Artifact artifact = new DefaultArtifact(groupId, artifactId, "jar", projectVersion.toString()).setFile(tmpFile);
			final DeployRequest request = new DeployRequest();
			request.addArtifact(artifact);
			request.setRepository(repo);
			
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
			repoSystem.deploy(session, request);
			
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
	public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canWrite() {
		return true;
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		// TODO Auto-generated method stub
		return Collections.emptyList();
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getLocation() {
		return "http://localhost:8080/nexus/content/repositories/scratch1";
	}
	
	static class ResultHolder {
		RepositoryPlugin.PutResult result;
		Exception error;
	}

}
