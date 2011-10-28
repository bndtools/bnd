package test.diff.classes;

import java.util.Date;

public class TestClass extends TestAbstractClass implements TestInterface {

	public void testMethod1(String s) throws Exception {
		System.out.println("Test!");
	}

	@TestRuntimeAnnotation(value1="x",currentRevision=2,array={"ary1","ary2"})
	public String testMethod2(String s1, String s2) {
		return s1 + s2;
	}

	public String testMethod3(String s1, Date... s2) {
		return s1 + s2.length;
	}

	public class InnerClass {
		
		public void innerClassMethod() {
			
		}
	}
}
