package test.api;

public interface Interf2 {
	String fooString();

	default String barString() {
		return "bar";
	}
}
