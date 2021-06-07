package aQute.libg.map;

import java.util.HashMap;
import java.util.Map;

/**
 * A map that is to set inline like a builder. I.e. you can do `new
 * LiteralMap<>().set("a", a).set("b",b)`.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class BuilderMap<K, V> extends HashMap<K, V> {
	private static final long serialVersionUID = 1L;

	public BuilderMap<K, V> set(K key, V value) {
		put(key, value);
		return this;
	}

	public BuilderMap<K, V> setAll(Map<K, V> src) {
		putAll(src);
		return this;
	}

	public BuilderMap<K, V> setIfAbsent(K key, V value) {
		putIfAbsent(key, value);
		return this;
	}

	public static <K, V> BuilderMap<K, V> map(K key, V value) {
		return new BuilderMap<K, V>().set(key, value);
	}

}
