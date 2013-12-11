package biz.aQute.bndoc.main;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import aQute.lib.consoleapp.*;
import aQute.lib.getopt.*;
import biz.aQute.bndoc.lib.*;

/**
 * This is the command line interface for bndoc.
 */
public class Main extends AbstractConsoleApp {

	private static final Pattern	BNDOC_P	= Pattern.compile("^.*\\.bndoc$");

	public Main() throws Exception {
		super();
	}

	@Description("The main options for bndoc")
	interface BndocOptions extends MainOptions {}

	/**
	 * Local initialization point. Is called first for the
	 * global commands.
	 */

	public void __main(BndocOptions opts) throws IOException {
		super.__main(opts);
	}

	@Arguments(arg = {})
	@Description("Generate the output documents as specified in the local bndoc.bndoc properties file")
	interface GenerateOptions extends Options {
		
		@Description("Clean the output directories")
		boolean clean();

		@Description("Add additional properties.")
		List<String> properties();
	}

	@Description("Generate the output files.")
	public void _generate(GenerateOptions options) throws Exception {
		
		Generator generator = new Generator(this);

		if (options.clean())
			generator.clean();

		if (options.properties() != null)
			for (String props : options.properties()) {
				File f = getFile(props);
				addProperties(f, BNDOC_P);
			}
		
		generator.generate();
		getInfo(generator);
	}

	@Description("Clean the output directories specified in the `clean` property")
	public void _clean(Options options) throws Exception {
		Generator generator = new Generator(this);
		generator.clean();
		getInfo(generator);
	}
	
	public static void main(String args[]) throws Exception {
		Main main = new Main();
		main.run(args);
	}
}
