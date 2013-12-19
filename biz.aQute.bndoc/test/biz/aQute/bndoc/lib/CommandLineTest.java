package biz.aQute.bndoc.lib;

import junit.framework.*;
import biz.aQute.bndoc.main.*;

public class CommandLineTest extends TestCase {

	public void testBase() throws Exception {
		Main.main(new String[] {"-et","html","--clean","--output","www","-p", "doc/bndoc.bndoc","-p", "run.bnd","--name","en-bndoc.html","--resources","doc/resources","doc/en"});
	}
	public void testBasePdf() throws Exception {
		Main.main(new String[] {"-et","pdf","--clean","--output","www","--keep", "-p", "doc/bndoc.bndoc","-p", "run.bnd","--name","en-bndoc.pdf","--resources","doc/resources","doc/en"});
	}
}
