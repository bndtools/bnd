package simple.pkg;

import iface.bundle.MyForeignRecursiveClass;

public class ClassExtendingForeignRecursiveClass<T extends ClassExtendingForeignRecursiveClass<T>>
	extends MyForeignRecursiveClass<T> {

	public void aMethod() {

	}

}
