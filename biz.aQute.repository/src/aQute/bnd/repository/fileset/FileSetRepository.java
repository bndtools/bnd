package aQute.bnd.repository.fileset;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.jar.JarFile;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.osgi.repository.BridgeRepository;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.util.repository.DownloadListenerPromise;
import aQute.bnd.version.Version;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.promise.PromiseExecutor;
import aQute.lib.strings.Strings;
import aQute.libg.cryptography.SHA256;
import aQute.service.reporter.Reporter;

public class FileSetRepository extends BaseRepository implements Plugin, RepositoryPlugin {
	private final static Logger					logger	= LoggerFactory.getLogger(FileSetRepository.class);
	private final String						name;
	private final Collection<File>				files;
	private final Deferred<BridgeRepository>	repository;
	private Reporter							reporter;
	private final PromiseExecutor				executor;

	public FileSetRepository(String name, Collection<File> files) throws Exception {
		this.name = name;
		this.files = files;
		executor = new PromiseExecutor();
		repository = executor.deferred();
	}

	private Collection<File> getFiles() {
		return files;
	}

	private BridgeRepository getBridge() throws Exception {
		Promise<BridgeRepository> promise = repository.getPromise();
		if (!promise.isDone()) {
			repository.resolveWith(readFiles());
		}
		return promise.getValue();
	}

	private Promise<BridgeRepository> readFiles() {
		Promise<List<Resource>> resources = getFiles().stream()
				.map(this::parseFile)
				.collect(executor.toPromise());
		if (logger.isDebugEnabled()) {
			resources.onSuccess(l -> l.stream().filter(Objects::nonNull).forEachOrdered(
					r -> logger.debug("{}: adding resource {}", getName(), r)));
		}
		Promise<BridgeRepository> bridge = resources.map(ResourcesRepository::new).map(BridgeRepository::new);
		return bridge;
	}

	private Promise<Resource> parseFile(File file) {
		Promise<Resource> resource = executor.submit(() -> {
			if (!file.isFile()) {
				return null;
			}
			ResourceBuilder rb = new ResourceBuilder();
			try (JarFile jar = new JarFile(file)) {
				Domain manifest = Domain.domain(jar.getManifest());
				boolean hasIdentity = rb.addManifest(manifest);
				if (!hasIdentity) {
					return null;
				}
			} catch (Exception f) {
				return null;
			}
			logger.debug("{}: parsing {}", getName(), file);
			Attrs attrs = new Attrs();
			attrs.put(ContentNamespace.CAPABILITY_URL_ATTRIBUTE, file.toURI().toString());
			attrs.putTyped(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE, file.length());
			attrs.put(ContentNamespace.CONTENT_NAMESPACE, SHA256.digest(file).asHex());
			rb.addCapability(CapabilityBuilder.createCapReqBuilder(ContentNamespace.CONTENT_NAMESPACE, attrs));
			return rb.build();
		});
		if (logger.isDebugEnabled()) {
			resource.onFailure(failure -> logger.debug("{}: failed to parse {}", getName(), file, failure));
		}
		return resource;
	}

	@Override
	public File get(String bsn, Version version, Map<String,String> properties, DownloadListener... listeners)
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
		return executor.resolved(new File(uri));
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
			return Strings.join(getFiles());
		}
	}

	@Override
	public String toString() {
		return String.format("%s [%-40s r/w=%s]", getName(), getLocation(), canWrite());
	}

	@Override
	public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
		try {
			return getBridge().getRepository().findProviders(requirements);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@Override
	public void setProperties(Map<String,String> map) throws Exception {
		// ignored
	}

	@Override
	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}
}
