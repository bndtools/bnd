package test.diff.classes;

import java.util.Date;

public interface TestInterface {

	void testMethod1(String s) throws Exception;

	String testMethod2(String s1, String s2);

	String testMethod3(String s1, Date...s2);
}
