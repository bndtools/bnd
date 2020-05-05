package aQute.bnd.osgi.repository;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.stream.MapStream;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.memoize.Memoize;

public abstract class AbstractIndexingRepository<KEY> extends BaseRepository {
	private final Map<KEY, Supplier<? extends Collection<? extends Resource>>>	resources;
	private volatile Supplier<ResourcesRepository>								repository;

	protected AbstractIndexingRepository() {
		resources = new ConcurrentHashMap<>();
		repository = memoize(this::aggregate);
	}

	protected <S> Supplier<S> memoize(Supplier<S> supplier) {
		return Memoize.referenceSupplier(supplier, SoftReference::new);
	}

	protected abstract boolean isValid(KEY key);

	protected BiFunction<ResourceBuilder, File, ? extends ResourceBuilder> fileIndexer(KEY key) {
		return (rb, file) -> {
			try {
				rb.addFile(file, file.toURI());
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
			return rb;
		};
	}

	public void index(KEY key, Collection<File> files) {
		index(key, () -> files);
	}

	public void index(KEY key, Supplier<? extends Collection<File>> files) {
		add(key, indexer(files, fileIndexer(key)));
	}

	protected void add(KEY key, Supplier<? extends Collection<? extends Resource>> supplier) {
		resources.put(key, memoize(supplier));
		repository = memoize(this::aggregate);
	}

	protected boolean remove(KEY key) {
		// Using | operator as we do not want to short-circuit
		if ((resources.remove(key) != null) | resources.keySet()
			.removeIf(p -> !isValid(p))) {
			repository = memoize(this::aggregate);
			return true;
		}
		return false;
	}

	private Supplier<List<Resource>> indexer(Supplier<? extends Collection<File>> files,
		BiFunction<? super ResourceBuilder, File, ? extends ResourceBuilder> fileIndexer) {
		requireNonNull(files);
		requireNonNull(fileIndexer);
		return () -> files.get()
			.stream()
			.filter(File::isFile)
			.map(file -> fileIndexer.apply(new ResourceBuilder(), file))
			.map(ResourceBuilder::build)
			.collect(toList());
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		Map<Requirement, Collection<Capability>> result = repository.get()
			.findProviders(requirements);
		return result;
	}

	private ResourcesRepository aggregate() {
		ResourcesRepository aggregate = MapStream.of(resources)
			.filterKey(this::isValid)
			.values()
			.map(Supplier::get)
			.flatMap(Collection::stream)
			.collect(ResourcesRepository.toResourcesRepository());
		return aggregate;
	}
}
