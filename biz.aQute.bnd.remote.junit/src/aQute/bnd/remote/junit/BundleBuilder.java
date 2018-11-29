package aQute.bnd.remote.junit;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import aQute.bnd.service.specifications.BuilderSpecification;
import aQute.lib.io.IO;

/**
 * Implementation class for building bundles on the remote workspace server.
 */
public class BundleBuilder implements BundleSpecBuilder {

	final JUnitFramework		ws;
	final BuilderSpecification	spec		= new BuilderSpecification();
	final List<Closeable>		closeables	= new ArrayList<>();

	BundleBuilder(JUnitFramework ws) {
		requireNonNull(ws, "'ws' cannot be null");
		this.ws = ws;
		bundleSymbolicName("test-" + JUnitFrameworkBuilder.counter.incrementAndGet());
	}

	/**
	 * Provides access to underlying builder
	 */
	@Override
	public BundleBuilder x() {
		return this;
	}

	Entry<String, Map<String, String>> add(String s, Entry<String, Map<String, String>> entry, String key,
			String value) {
		requireNonNull(s, "'s' cannot be null");
		requireNonNull(entry, "Requires " + s + " to be set before adding " + key + "=" + value);
		requireNonNull(key, "Key cannot be null");
		requireNonNull(value, "Value cannot be null");

		String put = entry.getValue().put(key, value);
		if (put != null) {
			throw new IllegalArgumentException(
				"Value already set " + s + " : " + key + "=" + value + " old value was: " + put);
		}
		return entry;
	}

	void add(String header, Map<String, Map<String, String>> s, String outerkey,
			String innerKey, String value) {
		Map<String, String> map = s.computeIfAbsent(outerkey, k -> new LinkedHashMap<>());
		s.put(outerkey, map);
		map.put(innerKey, value);
	}

	String join(String... packageNames) {
		requireNonNull(packageNames, "'packageNames' cannot be null");
		return Stream.of(packageNames).collect(Collectors.joining(","));
	}

	String prepare(String name, Map<String, Map<String, String>> domain) {
		StringBuilder builder = new StringBuilder(name);
		while (domain.containsKey(name)) {
			builder.append("~");
		}
		String val = builder.toString();
		domain.put(val, new LinkedHashMap<>());
		return val;
	}

	void addClose(Closeable closeable) {
		closeables.add(closeable);
	}

	void close() {
		closeables.forEach(IO::close);
	}

}