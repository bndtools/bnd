package simple.pkg;

public class RecursiveParameterizedClass<T extends RecursiveParameterizedClass<T>> {

	String field;

	public void method() {}
}
