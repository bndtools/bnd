package api_default_methods;

public interface Provider {
	void bar();
	default void foo() {
		
	}
}
