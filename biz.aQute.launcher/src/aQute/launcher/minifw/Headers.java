package aQute.launcher.minifw;

import static aQute.launcher.minifw.Enumerations.enumeration;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.jar.Manifest;
import java.util.stream.Stream;

class Headers extends Dictionary<String, String> {
	private final Manifest manifest;

	Headers(Manifest manifest) {
		this.manifest = manifest;
	}

	@Override
	public Enumeration<String> elements() {
		@SuppressWarnings({
			"unchecked", "rawtypes"
		})
		Collection<String> elements = (Collection) manifest.getMainAttributes()
			.values();
		return enumeration(elements);
	}

	@Override
	public String get(Object key) {
		String o = manifest.getMainAttributes()
			.getValue((String) key);
		return o;
	}

	@Override
	public boolean isEmpty() {
		return manifest.getMainAttributes()
			.isEmpty();
	}

	@Override
	public Enumeration<String> keys() {
		Stream<String> keys = manifest.getMainAttributes()
			.keySet()
			.stream()
			.map(Object::toString);
		return enumeration(keys);
	}

	@Override
	public String put(String key, String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return manifest.getMainAttributes()
			.size();
	}
}
