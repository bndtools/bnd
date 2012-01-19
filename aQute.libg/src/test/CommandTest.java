package test;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.lib.collections.*;
import aQute.lib.getopt.*;

public class CommandTest extends TestCase {

	static interface c1options extends Options {
		boolean flag();
		boolean notset();
		int a();
		String bb();
		Collection<File> input();
	};
	
	interface c2options extends Options {
		
	}
	
	public class C1 extends Assert {
		public String _c1(c1options x ) {
			
			assertEquals( true, x.flag());
			assertEquals( 33, x.a());
			assertEquals( "bb", x.bb(), "bb");
			assertEquals( Arrays.asList( new File("f1.txt"), new File("f2.txt")), x.input());
			assertEquals( false, x.notset());
			assertEquals( Arrays.asList( "-a", "--a", "a"), x._());
			
			return "a";
		}
		
		public void _c2(c2options x) {
			
		}
	}

	public void testHelp() throws Exception {
		Command getopt = new Command(System.out);
		C1 c1 = new C1();
		getopt.execute(c1, "help", new ExtList<String>("c1") );
		
	}
	public void testSimple() throws Exception {
		Command getopt = new Command(System.out);
		C1 c1 = new C1();
		String s = (String) getopt.execute(c1, "c1", new ExtList<String>("-f", "-a", "33", "--bb", "bb", "-i", "f1.txt", "-i", "f2.txt", "--", "-a", "--a", "a"));
		assertEquals("a",s);
	}
	
	
}
