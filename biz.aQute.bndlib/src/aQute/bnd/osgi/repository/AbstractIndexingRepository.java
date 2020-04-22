package aQute.bnd.osgi.repository;

import static aQute.lib.exceptions.FunctionWithException.asFunction;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.stream.MapStream;
import aQute.lib.memoize.Memoize;

public abstract class AbstractIndexingRepository<KEY> extends BaseRepository {
	private final Map<KEY, Supplier<? extends Collection<Resource>>>	resources;
	private volatile Supplier<ResourcesRepository>							repository;

	protected AbstractIndexingRepository() {
		resources = new ConcurrentHashMap<>();
		repository = Memoize.supplier(this::aggregate);
	}

	protected abstract boolean isValid(KEY key);

	protected abstract Consumer<? super ResourceBuilder> customizer(KEY key);

	public void index(KEY key, Collection<File> files) {
		index(key, () -> files);
	}

	public void index(KEY key, Supplier<? extends Collection<File>> files) {
		resources.keySet()
			.removeIf(p -> !isValid(p));
		resources.put(key, Memoize.supplier(indexer(files, customizer(key))));
		repository = Memoize.supplier(this::aggregate);
	}

	private Supplier<List<Resource>> indexer(
		Supplier<? extends Collection<File>> files, Consumer<? super ResourceBuilder> customizer) {
		requireNonNull(files);
		requireNonNull(customizer);
		return () -> files.get()
			.stream()
			.filter(File::isFile)
			.map(asFunction(file -> {
				ResourceBuilder rb = new ResourceBuilder();
				rb.addFile(file, file.toURI());
				customizer.accept(rb);
				return rb.build();
			}))
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
