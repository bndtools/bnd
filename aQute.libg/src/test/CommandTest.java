package test;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.lib.collections.*;
import aQute.lib.getopt.*;
import aQute.lib.justif.*;
import aQute.libg.reporter.*;

public class CommandTest extends TestCase {
	ReporterAdapter rp = new ReporterAdapter(System.err);
	
	
	
	
	public void testWrap() {
		StringBuilder sb = new StringBuilder();
		sb.append("Abc \t3Def ghi asoudg gd ais gdiasgd asgd auysgd asyudga8sdga8sydga 8sdg\fSame column\nbegin\n"
				+"\t3abc\t5def\nabc");
		Justif justif = new Justif(30);
		justif.wrap(sb);
		System.err.println(sb);
	}
	
	
	interface xoptions extends Options {
		boolean exceptions();
	}
	static class X {
		public void _cmda(xoptions opts) {
			
		}
		public void _cmdb(xoptions opts) {
			
		}
	}
	
	public void testCommand() throws Exception {
		CommandLine getopt = new CommandLine(rp);
		assertEquals( "[cmda, cmdb]", getopt.getCommands(new X()).keySet().toString());

		getopt.execute(new X(), "cmda", Arrays.asList("-e", "help"));
		
	}
	
	static interface c1options extends Options {
		boolean flag();
		boolean notset();
		int a();
		String bb();
		Collection<File> input();
	}
	
	interface c2options extends Options {
		
	}
	
	public static class C1 extends Assert {
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
		CommandLine getopt = new CommandLine(rp);
		C1 c1 = new C1();
		getopt.execute(c1, "help", new ExtList<String>("c1") );
		
	}
	public void testSimple() throws Exception {
		CommandLine getopt = new CommandLine(rp);
		C1 c1 = new C1();
		String help = (String) getopt.execute(c1, "c1", new ExtList<String>("-f", "-a", "33", "--bb", "bb", "-i", "f1.txt", "-i", "f2.txt", "--", "-a", "--a", "a"));
		System.err.println(help);
	}
	
	
}
