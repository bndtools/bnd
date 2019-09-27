package test.api;

import aQute.bnd.annotation.baseline.BaselineIgnore;

public class C extends A {
	@BaselineIgnore("1.1.0")
	public int s;
}
