package aQute.lib.osgi;

import java.util.*;

import aQute.lib.osgi.Descriptors.PackageRef;
import aQute.libg.header.*;

public class Packages implements Map<PackageRef,Attrs> {
	private LinkedHashMap<PackageRef,Attrs>	map;
	static Map<PackageRef,Attrs>			EMPTY	= Collections.emptyMap();

	public Packages(Packages other) {
		if (other.map != null) {
			map = new LinkedHashMap<Descriptors.PackageRef,Attrs>(other.map);
		}
	}

	public Packages() {}

	public void clear() {
		if (map != null)
			map.clear();
	}

	public boolean containsKey(PackageRef name) {
		if (map == null)
			return false;

		return map.containsKey(name);
	}

	@Deprecated
	public boolean containsKey(Object name) {
		assert name instanceof PackageRef;
		if (map == null)
			return false;

		return map.containsKey((PackageRef) name);
	}

	public boolean containsValue(Attrs value) {
		if (map == null)
			return false;

		return map.containsValue(value);
	}

	@Deprecated
	public boolean containsValue(Object value) {
		assert value instanceof Attrs;
		if (map == null)
			return false;

		return map.containsValue((Attrs) value);
	}

	public Set<java.util.Map.Entry<PackageRef,Attrs>> entrySet() {
		if (map == null)
			return EMPTY.entrySet();

		return map.entrySet();
	}

	@Deprecated
	public Attrs get(Object key) {
		assert key instanceof PackageRef;
		if (map == null)
			return null;

		return map.get((PackageRef) key);
	}

	public Attrs get(PackageRef key) {
		if (map == null)
			return null;

		return map.get(key);
	}

	public boolean isEmpty() {
		return map == null || map.isEmpty();
	}

	public Set<PackageRef> keySet() {
		if (map == null)
			return EMPTY.keySet();

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

	public Attrs put(PackageRef key, Attrs value) {
		if (map == null)
			map = new LinkedHashMap<PackageRef,Attrs>();

		return map.put(key, value);
	}

	public void putAll(Map< ? extends PackageRef, ? extends Attrs> map) {
		if (this.map == null)
			if (map.isEmpty())
				return;
			else
				this.map = new LinkedHashMap<PackageRef,Attrs>();
		this.map.putAll(map);
	}

	public void putAllIfAbsent(Map<PackageRef, ? extends Attrs> map) {
		for (Map.Entry<PackageRef, ? extends Attrs> entry : map.entrySet()) {
			if (!containsKey(entry.getKey()))
				put(entry.getKey(), entry.getValue());
		}
	}

	@Deprecated
	public Attrs remove(Object var0) {
		assert var0 instanceof PackageRef;
		if (map == null)
			return null;

		return map.remove((PackageRef) var0);
	}

	public Attrs remove(PackageRef var0) {
		if (map == null)
			return null;
		return map.remove(var0);
	}

	public int size() {
		if (map == null)
			return 0;
		return map.size();
	}

	public Collection<Attrs> values() {
		if (map == null)
			return EMPTY.values();

		return map.values();
	}

	public Attrs getByFQN(String s) {
		if (map == null)
			return null;

		for (Map.Entry<PackageRef,Attrs> pr : map.entrySet()) {
			if (pr.getKey().getFQN().equals(s))
				return pr.getValue();
		}
		return null;
	}

	public Attrs getByBinaryName(String s) {
		if (map == null)
			return null;

		for (Map.Entry<PackageRef,Attrs> pr : map.entrySet()) {
			if (pr.getKey().getBinary().equals(s))
				pr.getValue();
		}
		return null;
	}

	public boolean containsFQN(String s) {
		return getByFQN(s) != null;
	}

	public boolean containsBinaryName(String s) {
		return getByFQN(s) != null;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		append(sb);
		return sb.toString();
	}

	public void append(StringBuilder sb) {
		String del = "";
		for (Map.Entry<PackageRef,Attrs> s : entrySet()) {
			sb.append(del);
			sb.append(s.getKey());
			if (!s.getValue().isEmpty()) {
				sb.append(';');
				s.getValue().append(sb);
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

	@Deprecated
	public boolean equals(Object other) {
		return super.equals(other);
	}

	@Deprecated
	public int hashCode() {
		return super.hashCode();
	}

}
