package simple.pkg;

import iface.bundle.MyForeignException;
import iface.bundle.MyInterface;

// Add the MyInterface interface to check that we don't add suggestions for it
// when it's a missing exception; the missing interface isn't relevant to an
// error caused by a missing exception.
public class MyLocalException extends MyForeignException implements MyInterface {
	private static final long serialVersionUID = 1L;

	@Override
	public void myInterfaceMethod() {}
}
