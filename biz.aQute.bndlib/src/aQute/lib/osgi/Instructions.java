package aQute.lib.osgi;

import java.util.*;

import aQute.libg.header.*;

public class Instructions implements Map<Instruction,Attrs> {
	private LinkedHashMap<Instruction,Attrs>	map;
	static Map<Instruction,Attrs>				EMPTY	= Collections.emptyMap();

	public Instructions(Instructions other) {
		if (other.map != null && !other.map.isEmpty()) {
			map = new LinkedHashMap<Instruction,Attrs>(other.map);
		}
	}

	public Instructions(Collection<String> other) {
		if (other != null)
			for (String s : other) {
				put(new Instruction(s), null);
			}
	}

	public Instructions() {}

	public Instructions(Parameters contained) {
		append(contained);
	}

	public Instructions(String h) {
		this(new Parameters(h));
	}

	public void clear() {
		map.clear();
	}

	public boolean containsKey(Instruction name) {
		if (map == null)
			return false;

		return map.containsKey(name);
	}

	@Deprecated
	public boolean containsKey(Object name) {
		assert name instanceof Instruction;
		if (map == null)
			return false;

		return map.containsKey((Instruction) name);
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

	public Set<java.util.Map.Entry<Instruction,Attrs>> entrySet() {
		if (map == null)
			return EMPTY.entrySet();

		return map.entrySet();
	}

	@Deprecated
	public Attrs get(Object key) {
		assert key instanceof Instruction;
		if (map == null)
			return null;

		return map.get((Instruction) key);
	}

	public Attrs get(Instruction key) {
		if (map == null)
			return null;

		return map.get(key);
	}

	public boolean isEmpty() {
		return map == null || map.isEmpty();
	}

	public Set<Instruction> keySet() {
		if (map == null)
			return EMPTY.keySet();

		return map.keySet();
	}

	public Attrs put(Instruction key, Attrs value) {
		if (map == null)
			map = new LinkedHashMap<Instruction,Attrs>();

		return map.put(key, value);
	}

	public void putAll(Map< ? extends Instruction, ? extends Attrs> map) {
		if (this.map == null)
			if (map.isEmpty())
				return;
			else
				this.map = new LinkedHashMap<Instruction,Attrs>();
		this.map.putAll(map);
	}

	@Deprecated
	public Attrs remove(Object var0) {
		assert var0 instanceof Instruction;
		if (map == null)
			return null;

		return map.remove((Instruction) var0);
	}

	public Attrs remove(Instruction var0) {
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

	public String toString() {
		return map == null ? "{}" : map.toString();
	}

	public void append(Parameters other) {
		for (Map.Entry<String,Attrs> e : other.entrySet()) {
			put(new Instruction(e.getKey()), e.getValue());
		}
	}

	public <T> Collection<T> select(Collection<T> set, boolean emptyIsAll) {
		return select(set, null, emptyIsAll);
	}

	public <T> Collection<T> select(Collection<T> set, Set<Instruction> unused, boolean emptyIsAll) {
		List<T> input = new ArrayList<T>(set);
		if (emptyIsAll && isEmpty())
			return input;

		List<T> result = new ArrayList<T>();

		for (Instruction instruction : keySet()) {
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

		for (Instruction instruction : keySet()) {
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

	public boolean matches(String value) {
		if (size() == 0)
			return true;

		for (Instruction i : keySet()) {
			if (i.matches(value)) {
				if (i.isNegated())
					return false; // we deny this one explicitly
				else
					return true; // we allow it explicitly
			}
		}
		return false;
	}

}
