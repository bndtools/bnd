package aQute.bnd.repository.fileset;

import static aQute.lib.exceptions.FunctionWithException.asFunctionOrElse;
import static aQute.lib.promise.PromiseCollectors.toPromise;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.osgi.repository.BridgeRepository;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.util.repository.DownloadListenerPromise;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.strings.Strings;
import aQute.libg.cryptography.SHA256;
import aQute.maven.api.Revision;
import aQute.maven.provider.POM;
import aQute.service.reporter.Reporter;

public class FileSetRepository extends BaseRepository implements Plugin, RepositoryPlugin, Refreshable {
	private final static Logger					logger	= LoggerFactory.getLogger(FileSetRepository.class);
	private final String						name;
	private final Collection<File>				files;
	private volatile Deferred<BridgeRepository>	repository;
	private Reporter							reporter;
	private final PromiseFactory				promiseFactory;

	public FileSetRepository(String name, Collection<File> files) throws Exception {
		this.name = name;
		this.files = files;
		promiseFactory = new PromiseFactory(PromiseFactory.inlineExecutor());
		repository = promiseFactory.deferred();
	}

	private Collection<File> files() {
		return files;
	}

	public Collection<File> getFiles() {
		return Collections.unmodifiableCollection(files());
	}

	private BridgeRepository getBridge() throws Exception {
		Deferred<BridgeRepository> deferred = repository;
		Promise<BridgeRepository> promise = deferred.getPromise();
		if (!promise.isDone()) {
			deferred.resolveWith(readFiles());
		}
		return promise.getValue();
	}

	private Promise<BridgeRepository> readFiles() {
		Promise<List<Resource>> resources = files().stream()
			.map(this::parseFile)
			.collect(toPromise(promiseFactory));
		if (logger.isDebugEnabled()) {
			resources.onSuccess(l -> l.stream()
				.filter(Objects::nonNull)
				.forEachOrdered(r -> logger.debug("{}: adding resource {}", getName(), r)));
		}
		Promise<BridgeRepository> bridge = resources.map(ResourcesRepository::new)
			.map(BridgeRepository::new);
		return bridge;
	}

	private Promise<Resource> parseFile(File file) {
		Promise<Resource> resource = promiseFactory.submit(() -> {
			if (!file.isFile()) {
				return null;
			}
			ResourceBuilder rb = new ResourceBuilder();
			try (Jar jar = new Jar(file)) {
				Domain manifest = Domain.domain(jar.getManifest());
				boolean hasIdentity = rb.addManifest(manifest);
				if (!hasIdentity) {
					Optional<Revision> revision = jar.getPomXmlResources()
						.findFirst()
						.map(asFunctionOrElse(pomResource -> new POM(null, pomResource.openInputStream(), true), null))
						.map(POM::getRevision);

					String name = jar.getModuleName();
					if (name == null) {
						name = revision.map(r -> r.program.toString())
							.orElse(null);
						if (name == null) {
							return null;
						}
					}

					Version version = revision.map(r -> r.version.getOSGiVersion())
						.orElse(null);
					if (version == null) {
						version = new MavenVersion(jar.getModuleVersion()).getOSGiVersion();
					}

					CapReqBuilder identity = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
						.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, name)
						.addAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, version)
						.addAttribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_UNKNOWN);
					rb.addCapability(identity);
				}
			} catch (Exception f) {
				return null;
			}
			logger.debug("{}: parsing {}", getName(), file);
			CapReqBuilder content = new CapReqBuilder(ContentNamespace.CONTENT_NAMESPACE)
				.addAttribute(ContentNamespace.CONTENT_NAMESPACE, SHA256.digest(file)
					.asHex())
				.addAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE, file.toURI()
					.toString())
				.addAttribute(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE, Long.valueOf(file.length()));
			rb.addCapability(content);
			return rb.build();
		});
		if (logger.isDebugEnabled()) {
			resource.onFailure(failure -> logger.debug("{}: failed to parse {}", getName(), file, failure));
		}
		return resource;
	}

	@Override
	public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
		throws Exception {
		Promise<File> promise = get(bsn, version);
		if (promise == null) {
			return null;
		}
		if (listeners.length != 0) {
			new DownloadListenerPromise(reporter, "Get " + bsn + "-" + version + " for " + getName(), promise,
				listeners);
		}
		return promise.getValue();
	}

	private Promise<File> get(final String bsn, final Version version) throws Exception {
		logger.debug("{}: get {} {}", getName(), bsn, version);
		Resource resource = getBridge().get(bsn, version);
		if (resource == null) {
			logger.debug("{}: resource not found {} {}", getName(), bsn, version);
			return null;
		}
		ContentCapability content = ResourceUtils.getContentCapability(resource);
		if (content == null) {
			logger.warn("{}: No content capability for {}", getName(), resource);
			return null;
		}
		URI uri = content.url();
		if (uri == null) {
			logger.warn("{}: No content URI for {}", getName(), resource);
			return null;
		}
		logger.debug("{}: get returning {}", getName(), uri);
		return promiseFactory.resolved(new File(uri));
	}

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		throw new UnsupportedOperationException("Read only");
	}

	@Override
	public List<String> list(String pattern) throws Exception {
		List<String> bsns = getBridge().list(pattern);
		logger.debug("{}: list({}) {}", getName(), pattern, bsns);
		return bsns;
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		SortedSet<Version> versions = getBridge().versions(bsn);
		logger.debug("{}: versions({}) {}", getName(), bsn, versions);
		return versions;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getLocation() {
		try {
			return Strings.join(list(null));
		} catch (Exception e) {
			return Strings.join(files());
		}
	}

	@Override
	public String toString() {
		return String.format("%s [%-40s r/w=%s]", getName(), getLocation(), canWrite());
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		try {
			return getBridge().getRepository()
				.findProviders(requirements);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@Override
	public void setProperties(Map<String, String> map) throws Exception {
		// ignored
	}

	@Override
	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	@Override
	public boolean refresh() {
		repository = promiseFactory.deferred();
		return true;
	}

	@Override
	public File getRoot() {
		return new File(getName());
	}
}
