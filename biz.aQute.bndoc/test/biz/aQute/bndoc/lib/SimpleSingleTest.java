package biz.aQute.bndoc.lib;

import java.io.*;

import junit.framework.*;

public class SimpleSingleTest extends TestCase {

	public void testSimple() throws Exception {
		try (SinglePage sp = new SinglePage()) {
			sp.input(new File("testdocs/simple"));
			sp.output(System.out);
			sp.generate();
			assertEquals(3, sp.decorator.counters[0]);
			assertEquals(1, sp.decorator.counters[1]);
			assertEquals(0, sp.decorator.counters[2]);
		}
	}
	
	public void testPreprocess() throws Exception {
		try (SinglePage sp = new SinglePage()) {
			StringWriter sb = new StringWriter();
			sp.input(new File("testdocs/preprocess"));
			sp.output(sb);
			sp.generate();
			System.out.println(sb);
		}		
	}

}
