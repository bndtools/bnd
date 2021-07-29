package simple.pkg;

import iface.bundle.InterfaceExtendingMyInterface;

public class ClassWithInterfaceExtendingMyInterface implements InterfaceExtendingMyInterface {
	private static final long serialVersionUID = 1L;

	public void zMethod() {}

	@Override
	public void myInterfaceMethod() {}
}
