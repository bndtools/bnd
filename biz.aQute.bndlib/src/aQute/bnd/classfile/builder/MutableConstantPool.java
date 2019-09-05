package aQute.bnd.classfile.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import aQute.bnd.classfile.ConstantPool;

public class MutableConstantPool extends ConstantPool {
	private final List<Object> pool;

	public MutableConstantPool() {
		super(new Object[1]);
		pool = new ArrayList<>();
		pool.add(null); // index 0
	}

	public MutableConstantPool(ConstantPool constantPool) {
		super(new Object[1]);
		int constant_pool_count = constantPool.size();
		pool = new ArrayList<>(constant_pool_count);
		pool.add(null); // index 0
		for (int index = 1; index < constant_pool_count; index++) {
			pool.add(constantPool.entry(index));
		}
	}

	@Override
	public int size() {
		return pool.size();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T entry(int index) {
		return (T) pool.get(index);
	}

	@SuppressWarnings("unchecked")
	public <T> T entry(int index, Object entry) {
		return (T) pool.set(index, entry);
	}

	@Override
	public String toString() {
		return pool.toString();
	}

	@Override
	protected <I> int add(Class<I> infoType, Supplier<I> supplier) {
		I entry = supplier.get();
		int index = size(); // supplier may have added to pool
		pool.add(entry);
		if ((infoType == Long.class) || (infoType == Double.class)) {
			// For some insane optimization reason, the Long(5) and
			// Double(6) entries take two slots in the constant pool.
			// See 4.4.5
			pool.add(null);
		}
		return index;
	}
}
