package test.diff.classes.oldclasses;

import java.util.*;

public class MajorModifiedClass {

	public static final String PUBLIC_CONSTANT = "constant";
	protected static final String PROTECTED_CONSTANT = "protected";
	private static final String PRIVATE_CONSTANT = "private";
	
	public void method1(String s) {
		if (PRIVATE_CONSTANT.equals(s)) {
			
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map method2(String s1, String s2) {
		Map map = new HashMap();
		map.put(s1, s2);
		return map;
	}
}
