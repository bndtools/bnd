package biz.aQute.bndoc.lib;

import junit.framework.*;
import biz.aQute.bndoc.main.*;

public class CommandLineTest extends TestCase {

	public void testBase() throws Exception {
		Main.main(new String[] {"-etb", "doc", "html","--clean","--resources","../www","-p", "bndoc.bndoc","-o","../www/en-bndoc.html","en"});
	}
	public void testBasePdf() throws Exception {
		Main.main(new String[] {"-etb", "doc", "pdf","--clean","--resources","../www","-p", "bndoc.bndoc","-o","../www/en-bndoc.pdf","en"});
	}
}
