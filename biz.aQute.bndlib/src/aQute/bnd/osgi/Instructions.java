package aQute.bnd.osgi;

import java.io.*;
import java.util.*;

import aQute.bnd.header.*;
import aQute.lib.io.*;

public class Instructions implements Map<Instruction,Attrs> {
	private LinkedHashMap<Instruction,Attrs>	map;
	public static Instructions					ALWAYS	= new Instructions();
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

		return map.containsKey(name);
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

		return map.containsValue(value);
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

		return map.get(key);
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
		if (this.map == null) {
			if (map.isEmpty())
				return;
			this.map = new LinkedHashMap<Instruction,Attrs>();
		}
		this.map.putAll(map);
	}

	@Deprecated
	public Attrs remove(Object var0) {
		assert var0 instanceof Instruction;
		if (map == null)
			return null;

		return map.remove(var0);
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

	@Override
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

	public Instruction matcher(String value) {
		for (Instruction i : keySet()) {
			if (i.matches(value)) {
				return i;
			}
		}
		return null;
	}

	public Instruction finder(String value) {
		for (Instruction i : keySet()) {
			if (i.finds(value)) {
				return i;
			}
		}
		return null;
	}

	public boolean matches(String value) {
		if (size() == 0)
			return true;

		Instruction instr = matcher(value);
		if (instr == null || instr.isNegated())
			return false; // we deny this one explicitly
		return true;
	}

	/**
	 * Turn this Instructions into a map of File -> Attrs. You can specify a
	 * base directory, which will match all files in that directory against the
	 * specification or you can use literal instructions to get files from
	 * anywhere.
	 * 
	 * @param base
	 *            The directory to list files from.
	 * @return The map that links files to attributes
	 */
	public Map<File,Attrs> select(File base) {

		Map<File,Attrs> result = new HashMap<File,Attrs>();

		//
		// We allow literals to be specified so that we can actually include
		// files from anywhere in the file system
		//

		for (java.util.Map.Entry<Instruction,Attrs> instr : entrySet()) {
			if (instr.getKey().isLiteral() && !instr.getKey().isNegated()) {
				File f = IO.getFile(base, instr.getKey().getLiteral());
				if (f.isFile())
					result.put(f, instr.getValue());
			}
		}

		//
		// Iterator over the found files and match them against this
		//

		if (base != null) {
			nextFile: for (File f : base.listFiles()) {
				for (Entry<Instruction,Attrs> instr : entrySet()) {
					String name = f.getName();
					if (instr.getKey().matches(name)) {
						if (!instr.getKey().isNegated())
							result.put(f, instr.getValue());
						continue nextFile;
					}
				}
			}
		}
		return result;
	}

}
