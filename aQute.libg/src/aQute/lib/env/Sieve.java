package aQute.lib.env;

import java.util.*;


public class Sieve implements Map<Selector,Props> {
	private LinkedHashMap<Selector,Props>	map;
	public static Sieve					ALWAYS	= new Sieve();
	static Map<Selector,Props>				EMPTY	= Collections.emptyMap();

	public Sieve(Sieve other) {
		if (other.map != null && !other.map.isEmpty()) {
			map = new LinkedHashMap<Selector,Props>(other.map);
		}
	}

	public Sieve(Collection<String> other) {
		if (other != null)
			for (String s : other) {
				put(new Selector(s), null);
			}
	}

	public Sieve() {}

	public Sieve(Header contained) {
		append(contained);
	}

	public Sieve(String h) {
		this(new Header(h));
	}

	public void clear() {
		map.clear();
	}

	public boolean containsKey(Selector name) {
		if (map == null)
			return false;

		return map.containsKey(name);
	}

	@Deprecated
	public boolean containsKey(Object name) {
		assert name instanceof Selector;
		if (map == null)
			return false;

		return map.containsKey(name);
	}

	public boolean containsValue(Props value) {
		if (map == null)
			return false;

		return map.containsValue(value);
	}

	@Deprecated
	public boolean containsValue(Object value) {
		assert value instanceof Props;
		if (map == null)
			return false;

		return map.containsValue(value);
	}

	public Set<java.util.Map.Entry<Selector,Props>> entrySet() {
		if (map == null)
			return EMPTY.entrySet();

		return map.entrySet();
	}

	@Deprecated
	public Props get(Object key) {
		assert key instanceof Selector;
		if (map == null)
			return null;

		return map.get(key);
	}

	public Props get(Selector key) {
		if (map == null)
			return null;

		return map.get(key);
	}

	public boolean isEmpty() {
		return map == null || map.isEmpty();
	}

	public Set<Selector> keySet() {
		if (map == null)
			return EMPTY.keySet();

		return map.keySet();
	}

	public Props put(Selector key, Props value) {
		if (map == null)
			map = new LinkedHashMap<Selector,Props>();

		return map.put(key, value);
	}

	public void putAll(Map< ? extends Selector, ? extends Props> map) {
		if (this.map == null) {
			if (map.isEmpty())
				return;
			this.map = new LinkedHashMap<Selector,Props>();
		}
		this.map.putAll(map);
	}

	@Deprecated
	public Props remove(Object var0) {
		assert var0 instanceof Selector;
		if (map == null)
			return null;

		return map.remove(var0);
	}

	public Props remove(Selector var0) {
		if (map == null)
			return null;
		return map.remove(var0);
	}

	public int size() {
		if (map == null)
			return 0;
		return map.size();
	}

	public Collection<Props> values() {
		if (map == null)
			return EMPTY.values();

		return map.values();
	}

	@Override
	public String toString() {
		return map == null ? "{}" : map.toString();
	}

	public void append(Header other) {
		for (Map.Entry<String,Props> e : other.entrySet()) {
			put(new Selector(e.getKey()), e.getValue());
		}
	}

	public <T> Collection<T> select(Collection<T> set, boolean emptyIsAll) {
		return select(set, null, emptyIsAll);
	}

	public <T> Collection<T> select(Collection<T> set, Set<Selector> unused, boolean emptyIsAll) {
		List<T> input = new ArrayList<T>(set);
		if (emptyIsAll && isEmpty())
			return input;

		List<T> result = new ArrayList<T>();

		for (Selector instruction : keySet()) {
			boolean used = false;
			for (Iterator<T> o = input.iterator(); o.hasNext();) {
				T oo = o.next();
				String s = oo.toString();
				if (instruction.matches(s)) {
					if (!instruction.isNegated())
						result.add(oo);
					o.remove();
					used = true;
				}
			}
			if (!used && unused != null)
				unused.add(instruction);
		}
		return result;
	}

	public <T> Collection<T> reject(Collection<T> set) {
		List<T> input = new ArrayList<T>(set);
		List<T> result = new ArrayList<T>();

		for (Selector instruction : keySet()) {
			for (Iterator<T> o = input.iterator(); o.hasNext();) {
				T oo = o.next();
				String s = oo.toString();
				if (instruction.matches(s)) {
					if (instruction.isNegated())
						result.add(oo);
					o.remove();
				} else
					result.add(oo);

			}
		}
		return result;
	}

	public Selector matcher(String value) {
		for (Selector i : keySet()) {
			if (i.matches(value)) {
				return i;
			}
		}
		return null;
	}
	
	public Selector finder(String value) {
		for (Selector i : keySet()) {
			if (i.finds(value)) {
				return i;
			}
		}
		return null;
	}
	
	public boolean matches(String value) {
		if (size() == 0)
			return true;

		Selector instr = matcher(value);
		if (instr==null || instr.isNegated())
			return false; // we deny this one explicitly
		return true;
	}

}
