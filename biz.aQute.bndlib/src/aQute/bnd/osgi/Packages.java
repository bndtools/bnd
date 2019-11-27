package aQute.bnd.osgi;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.stream.MapStream;

public class Packages implements Map<PackageRef, Attrs> {
	private final Map<PackageRef, Attrs> map;

	public enum QUERY {
		ANY,
		ANNOTATED,
		NAMED,
		VERSIONED,
		CONDITIONAL;
	}

	public Packages(Packages other) {
		map = new LinkedHashMap<>(other.map);
	}

	public Packages() {
		map = new LinkedHashMap<>();
	}

	@Override
	public void clear() {
		map.clear();
	}

	public boolean containsKey(PackageRef name) {
		return map.containsKey(name);
	}

	@Override
	@Deprecated
	public boolean containsKey(Object name) {
		assert name instanceof PackageRef;
		return map.containsKey(name);
	}

	public boolean containsValue(Attrs value) {
		return map.containsValue(value);
	}

	@Override
	@Deprecated
	public boolean containsValue(Object value) {
		assert value instanceof Attrs;
		return map.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<PackageRef, Attrs>> entrySet() {
		return map.entrySet();
	}

	public MapStream<PackageRef, Attrs> stream() {
		return MapStream.of(this);
	}

	@Override
	@Deprecated
	public Attrs get(Object key) {
		assert key instanceof PackageRef;
		return map.get(key);
	}

	public Attrs get(PackageRef key) {
		return map.get(key);
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public Set<PackageRef> keySet() {
		return map.keySet();
	}

	public Attrs put(PackageRef ref) {
		Attrs attrs = get(ref);
		if (attrs != null)
			return attrs;

		attrs = new Attrs();
		put(ref, attrs);
		return attrs;
	}

	@Override
	public Attrs put(PackageRef key, Attrs value) {
		return map.put(key, value);
	}

	@Override
	public void putAll(Map<? extends PackageRef, ? extends Attrs> map) {
		this.map.putAll(map);
	}

	public void putAllIfAbsent(Map<PackageRef, ? extends Attrs> map) {
		MapStream.of(map)
			.filterKey(key -> !containsKey(key))
			.forEachOrdered(this::put);
	}

	@Override
	@Deprecated
	public Attrs remove(Object var0) {
		assert var0 instanceof PackageRef;
		return map.remove(var0);
	}

	public Attrs remove(PackageRef var0) {
		return map.remove(var0);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public Collection<Attrs> values() {
		return map.values();
	}

	public Attrs getByFQN(String s) {
		return stream().filterKey(key -> key.getFQN()
			.equals(s))
			.values()
			.findFirst()
			.orElse(null);
	}

	public Attrs getByBinaryName(String s) {
		return stream().filterKey(key -> key.getBinary()
			.equals(s))
			.values()
			.findFirst()
			.orElse(null);
	}

	public boolean containsFQN(String s) {
		return getByFQN(s) != null;
	}

	public boolean containsBinaryName(String s) {
		return getByBinaryName(s) != null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		append(sb);
		return sb.toString();
	}

	public void append(StringBuilder sb) {
		String del = "";
		for (Map.Entry<PackageRef, Attrs> s : entrySet()) {
			sb.append(del);
			sb.append(s.getKey());
			if (!s.getValue()
				.isEmpty()) {
				sb.append(';');
				s.getValue()
					.append(sb);
			}
			del = ",";
		}
	}

	public void merge(PackageRef ref, boolean unique, Attrs... attrs) {
		if (unique) {
			while (containsKey(ref))
				ref = ref.getDuplicate();
		}

		Attrs org = put(ref);
		for (Attrs a : attrs) {
			if (a != null)
				org.putAll(a);
		}
	}

	public Attrs get(PackageRef packageRef, Attrs deflt) {
		Attrs mine = get(packageRef);
		if (mine != null)
			return mine;

		return deflt;
	}

	@Override
	@Deprecated
	public boolean equals(Object other) {
		return super.equals(other);
	}

	@Override
	@Deprecated
	public int hashCode() {
		return super.hashCode();
	}

}
