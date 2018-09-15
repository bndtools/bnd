package test.diff.inherit;

import java.io.Serializable;

public abstract class A<E extends Serializable> extends B<E> {

	@Override
	public A<E> h(E toElement) {
		return null;
	}
}
