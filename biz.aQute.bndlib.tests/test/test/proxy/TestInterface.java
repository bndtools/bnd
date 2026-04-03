package test.proxy;

import java.nio.file.Path;
import java.util.List;

/**
 * Test interface that has methods returning types from different packages.
 * This is used to test that Proxy.newProxyInstance detection works correctly.
 */
public interface TestInterface {
	
	/**
	 * Method returning a type from java.nio.file
	 */
	Path getPath();
	
	/**
	 * Method returning a type from java.util
	 */
	List<String> getList();
	
	/**
	 * Method with parameter from java.nio.file
	 */
	void setPath(Path path);
}
