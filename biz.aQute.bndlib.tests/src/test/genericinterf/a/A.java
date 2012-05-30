package test.genericinterf.a;

import test.genericinterf.b.*;
import test.genericinterf.c.*;

public class A<T extends A> implements B<C<T>> {
	public T	field;

	public <X> X foo(X x) {
		return null;
	}
}
