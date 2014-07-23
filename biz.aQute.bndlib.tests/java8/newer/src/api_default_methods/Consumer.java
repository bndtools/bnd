package api_default_methods;

public interface Consumer {
	void bar();
	default void foo() {
		
	}
}
