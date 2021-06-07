package test;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Test;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import junit.framework.AssertionFailedError;

public class MyTest2 {

	@Test
	public void myTest1() {}
	
	Multimap<String, String> m;
	
	@Test
	public void myTest2() {
		m = Multimaps.newMultimap(new HashMap<>(), ArrayList::new);
	}
	
	@Test
	public void myTest3() {
		throw new AssertionFailedError();
	}
	
	@Test
	public void myTest4() {
		throw new AssertionError();
	}
	
	@Test
	public void myTest6() {
		throw new RuntimeException();
	}
	
	@org.junit.jupiter.api.Test
	public void myTest7() {
		throw new RuntimeException();
	}
	
	@org.junit.jupiter.api.Test
	public void myTest5() {
		throw new org.opentest4j.AssertionFailedError();
	}
}
