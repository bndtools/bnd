package test.api;

import java.util.*;

public interface Interf {
	/**
	 * Test if a change in generic type is detected
	 */
	
	 Collection<String> foo();
	
	 int fooInt();
	 String fooString();
	
	<X,Y> X foo(Y bla);
	
	void foo(List<String> l);
}
