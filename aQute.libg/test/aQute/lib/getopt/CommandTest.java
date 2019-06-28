package aQute.lib.getopt;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import aQute.lib.collections.ExtList;
import aQute.lib.justif.Justif;
import aQute.libg.reporter.ReporterAdapter;
import junit.framework.TestCase;

public class CommandTest extends TestCase {
	ReporterAdapter rp = new ReporterAdapter(System.err);

	public static void testWrap() {
		StringBuilder sb = new StringBuilder();
		sb.append("Abc \t3Def ghi asoudg gd ais gdiasgd asgd auysgd asyudga8sdga8sydga 8sdg\fSame column\nbegin\n"
			+ "\t3abc\t5def\nabc");
		Justif justif = new Justif(30);
		justif.wrap(sb);
		System.err.println(sb);
	}

	interface xoptions extends Options {
		boolean exceptions();
	}

	static class X {
		public void _cmda(@SuppressWarnings("unused") xoptions opts) {

		}

		public void _cmdb(@SuppressWarnings("unused") xoptions opts) {

		}
	}

	public void testCommand() throws Exception {
		CommandLine getopt = new CommandLine(rp);
		assertEquals("[cmda, cmdb]", getopt.getCommands(new X())
			.keySet()
			.toString());

		getopt.execute(new X(), "cmda", Arrays.asList("-e", "help"));

	}

	interface c1options extends Options {
		boolean flag();

		boolean notset();

		int a();

		String bb();

		Collection<File> input();
	}

	interface c2options extends Options {

	}

	public static class C1 {
		public static String _c1(c1options x) {

			assertEquals(true, x.flag());
			assertEquals(33, x.a());
			assertEquals("bb", x.bb(), "bb");
			assertEquals(Arrays.asList(new File("f1.txt"), new File("f2.txt")), x.input());
			assertEquals(false, x.notset());
			assertEquals(Arrays.asList("-a", "--a", "a"), x._arguments());

			return "a";
		}

		public void _c2(@SuppressWarnings("unused") c2options x) {

		}
	}

	public void testHelp() throws Exception {
		CommandLine getopt = new CommandLine(rp);
		C1 c1 = new C1();
		getopt.execute(c1, "help", new ExtList<>("c1"));

	}

	public void testSimple() throws Exception {
		CommandLine getopt = new CommandLine(rp);
		C1 c1 = new C1();
		String help = getopt.execute(c1, "c1",
			new ExtList<>("-f", "-a", "33", "--bb", "bb", "-i", "f1.txt", "-i", "f2.txt", "--", "-a", "--a", "a"));
		System.err.println(help);
	}

	interface Opt1 {
		String title();
	}

	interface Opt2 {
		String test();
	}

	interface Opt3 {
		String third();
	}

	interface Opt4 {
		String Wrong();
	}

	@Arguments(arg = {
		"..."
	})
	interface TwoOptions extends Opt1, Opt2, Options {}

	interface ThreeOptions extends Opt1, Opt2, Opt3 {}

	public static class CommandTwoOptions {
		public String test, title;

		public void _commandTwoOptions(TwoOptions opts) {
			test = opts.test();
			title = opts.title();
		}
	}

	public static class CommandThreeOptions {
		public void _commandThreeOptions(ThreeOptions opts) {}
	}

	public static class CommandWrongOption {
		public void _commandWrongOption(Opt4 opts) {}
	}

	public void test_SameFirstChar() throws Exception {
		CommandLine getopt = new CommandLine(rp);
		CommandTwoOptions c = new CommandTwoOptions();
		getopt.execute(c, "commandTwoOptions", new ExtList<>("-t", "test", "-T", "title"));
		assertEquals("title", c.title);
		assertEquals("test", c.test);
	}

	public void test_SameFirstChar_NoCapitalizedCommands() throws Exception {
		CommandLine getopt = new CommandLine(rp);
		CommandWrongOption c = new CommandWrongOption();
		try {
			getopt.execute(c, "commandWrongOption", new ExtList<>());
			fail();
		} catch (Error e) {}
	}

	public void test_SameFirstChar_MaxTwoOptionsWithSameShortcut() throws Exception {
		CommandLine getopt = new CommandLine(rp);
		CommandThreeOptions c = new CommandThreeOptions();
		try {
			getopt.execute(c, "commandThreeOptions", new ExtList<>());
			fail();
		} catch (Error e) {}
	}
}
