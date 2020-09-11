package simple.pkg;

public class MyParameterizedClass<T> {

	T field;

	public class MyInner {}

	// This is overloaded in a subclass to test a particular error
	protected void myOverloadedMethod(String param) {}
}
