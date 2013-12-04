package biz.aQute.bndoc.main;

import java.io.*;
import java.util.*;

import aQute.lib.consoleapp.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import biz.aQute.bndoc.lib.*;

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
	@Description("Just Another Package Manager for Java (\"jpm help jpm\" to see a list of global options)")
	public void _main(BndocOptions opts) throws IOException {
		super._main(opts);
	}

	
	interface GenerateOptions extends Options {
		String output();
		String template();
		List<String> properties();
		String number();
		String set();
	}
	interface SingleOptions extends GenerateOptions {
		
	}

	public void _single(SingleOptions options ) throws Exception {
		SinglePage sp = new SinglePage();
		set(sp, options);
		sp.reporter(this);
	}

	private void set(Bndoc doc, SingleOptions options) throws Exception {
		if ( options.output() == null)
			doc.output(out);
		else
			doc.output( getFile(options.output()));

		if ( options.template() != null) {
			File file = getFile(options.template(), "No such template %s" );
			if ( file != null)
				doc.template( IO.collect(file));
		}

		if ( options.properties() != null) {
			for ( String propertiesFile : options.properties()) {
				File f = getFile(propertiesFile, "No such properties file %s");
				if ( f != null) {
					
				}
			}
		}
	}
}
