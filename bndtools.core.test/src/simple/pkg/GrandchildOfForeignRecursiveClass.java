package simple.pkg;

public class GrandchildOfForeignRecursiveClass<T extends GrandchildOfForeignRecursiveClass<T>>
	extends ClassExtendingForeignRecursiveClass<T> {

	public void grandchildMethod() {

	}

}
