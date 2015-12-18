package test.genericinterf.a;

import test.genericinterf.b.B;
import test.genericinterf.c.C;

public class A<T> implements B<C<T>> {
	public T field;

	public static <X> X foo(@SuppressWarnings("unused") X x) {
		return null;
	}
}
