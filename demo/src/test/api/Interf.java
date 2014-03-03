package test.api;

import java.util.*;

public interface Interf {
	/**
	 * Test if a change in generic type is detected
	 */
	
	public Collection<String> foo();
	
	public int fooInt();
	public String fooString();
	
	<X,Y> X foo(Y bla);
}
