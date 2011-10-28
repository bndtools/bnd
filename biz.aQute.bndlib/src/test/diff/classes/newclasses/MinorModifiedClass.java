package test.diff.classes.newclasses;

import java.util.*;

public class MinorModifiedClass {

	public static final String PUBLIC_CONSTANT = "constant";
	protected static final String PROTECTED_CONSTANT = "protected";
	private static final String PRIVATE_CONSTANT = "private";

	private static final String PRIVATE_CONSTANT2 = "private2";
	
	public void method1(String s) {
		if (PRIVATE_CONSTANT.equals(s) || PRIVATE_CONSTANT2.equals(s)) {
			
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map method2(String s1, String s2) {
		Map map = new HashMap();
		map.put(s1, s2);
		return map;
	}
	
	//Added new method
	public void newMethod(Date d) {
		
	}
}
