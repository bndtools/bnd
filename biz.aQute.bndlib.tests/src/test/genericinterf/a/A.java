package test.genericinterf.a;

import test.genericinterf.b.*;
import test.genericinterf.c.*;

public class A<T> implements B<C<T>> {
	public T	field;

	public <X> X foo(@SuppressWarnings("unused") X x) {
		return null;
	}
}
