package aQute.launchpad;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import aQute.bnd.service.specifications.BuilderSpecification;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

/**
 * Implementation class for building bundles on the remote workspace server.
 */
public class BundleBuilder implements BundleSpecBuilder, AutoCloseable {

	final Launchpad				ws;
	final BuilderSpecification	spec		= new BuilderSpecification();
	final List<Closeable>		closeables	= new ArrayList<>();
	static final Pattern		NONTOKEN	= Pattern.compile("[^-\\w]+");
	static final Pattern		DOT			= Pattern.compile("\\.");

	BundleBuilder(Launchpad ws) {
		this.ws = ws;
		spec.classpath.add(ws.runspec.bin_test);

		final String baseName = DOT.splitAsStream(ws.getClassName() + '.' + ws.getName())
			.map(x -> NONTOKEN.matcher(x)
				.replaceAll(""))
			.filter(x -> !x.isEmpty())
			.collect(Collectors.joining("."));
		bundleSymbolicName(baseName + '-' + ws.counter.incrementAndGet());
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
		if (entry == null)
			throw new IllegalArgumentException("Requires " + s + " to be set before adding " + key + "=" + value);

		String put = entry.getValue()
			.put(key, value);
		if (put != null)
			throw new IllegalArgumentException(
				"Value already set " + s + " : " + key + "=" + value + " old value was: " + put);
		return entry;
	}

	void add(String header, Map<String, Map<String, String>> s, String outerkey, String innerKey, String value) {

		Map<String, String> map = s.get(outerkey);
		if (map == null) {
			map = new LinkedHashMap<>();
			s.put(outerkey, map);
		}
		map.put(innerKey, value);
	}

	String join(String... packageNames) {
		return Stream.of(packageNames)
			.collect(Strings.joining());
	}

	String prepare(String name, Map<String, Map<String, String>> domain) {
		while (domain.containsKey(name)) {
			name += "~";
		}
		domain.put(name, new LinkedHashMap<>());
		return name;
	}

	void addClose(Closeable closeable) {
		closeables.add(closeable);
	}

	@Override
	public void close() {
		closeables.forEach(IO::close);
	}

}
