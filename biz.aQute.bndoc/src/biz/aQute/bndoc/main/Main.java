package biz.aQute.bndoc.main;

import java.io.*;

import aQute.lib.consoleapp.*;
import aQute.lib.getopt.*;

public class Main extends AbstractConsoleApp {
	

	public Main() throws UnsupportedEncodingException {
		super();
	}
	
	interface BndocOptions extends MainOptions {
		
	}

	/**
	 * Initialize the repository and other global vars.
	 * 
	 * @param opts
	 *            the options
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Description("")
	public void _main(BndocOptions opts) throws IOException {
		super._main(opts);
	}

	
	interface GenerateOptions extends Options {
	}
	interface SingleOptions extends GenerateOptions {		
	}

	public void _generate(GenerateOptions go) {
		
	}
}
