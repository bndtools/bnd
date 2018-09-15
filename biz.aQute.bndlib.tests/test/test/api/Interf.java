package test.api;

import java.util.Collection;
import java.util.List;

public interface Interf {
	/**
	 * Test if a change in generic type is detected. The original has a String
	 */

	public Collection<Integer> foo();

	public int fooInt();

	public String fooString();

	<X extends List<String>, Y> X foo(Y bla);

	void foo(List<Integer> l);
}
