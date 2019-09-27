package test.api;

import java.util.Collection;
import java.util.List;

import aQute.bnd.annotation.baseline.BaselineIgnore;

@BaselineIgnore("1.1.0")
public interface Interf {
	/**
	 * Test if a change in generic type is detected. The original has a String
	 */

	Collection<Integer> foo();

	int fooInt();

	String fooString();

	<X extends List<String>, Y> X foo(Y bla);

	void foo(List<Integer> l);
}
