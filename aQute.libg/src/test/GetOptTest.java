package test;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.lib.getopt.*;

public class GetOptTest extends TestCase {

	static interface c1 extends IGetOpt {
		boolean flag();
		boolean notset();
		int a();
		String bb();
		Collection<File> input();
	};
	
	public void testSimple() {
		c1 x = GetOpt.getopt(new String[]{"-f", "-a", "33", "--bb", "bb", "-i", "f1.txt", "-i", "f2.txt", "--", "-a", "--a", "a"}, 0, c1.class);
		assertEquals( true, x.flag());
		assertEquals( 33, x.a());
		assertEquals( "bb", x.bb(), "bb");
		assertEquals( Arrays.asList( new File("f1.txt"), new File("f2.txt")), x.input());
		assertEquals( false, x.notset());
		assertEquals( Arrays.asList( "-a", "--a", "a"), x._());
	}
	
	
}
