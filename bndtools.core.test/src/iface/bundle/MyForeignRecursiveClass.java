package iface.bundle;

import simple.pkg.RecursiveParameterizedClass;

public class MyForeignRecursiveClass<T extends MyForeignRecursiveClass<T>> extends RecursiveParameterizedClass<T> {

	protected String bField;

	public void bMethod() {}
}
