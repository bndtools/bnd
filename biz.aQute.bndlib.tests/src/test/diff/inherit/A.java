package test.diff.inherit;

import java.io.*;

public abstract class A<E extends String> extends B<E> {

	public A<E> h(E toElement) {
		return null;
	}
}
