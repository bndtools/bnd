package simple.pkg;

import java.util.List;

import iface.bundle.MyInterface;

public class ClassWithMethodReturningBoundFromAnotherBundle<T extends MyInterface> {

	public List<T> aMethod() {
		return null;
	}
}
