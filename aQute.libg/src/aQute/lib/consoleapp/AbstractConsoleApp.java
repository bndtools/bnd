package aQute.lib.consoleapp;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import aQute.lib.collections.*;
import aQute.lib.env.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.lib.justif.*;
import aQute.lib.settings.*;

public abstract class AbstractConsoleApp extends Env {
	Settings					settings;

	protected final PrintStream	err;
	protected final PrintStream	out;

	static String				encoding	= System.getProperty("file.encoding");
	int							width		= 120;									// characters
	int							tabs[]		= {
			40, 48, 56, 64, 72, 80, 88, 96, 104, 112
											};
	private final Object		target;

	static {
		if (encoding == null)
			encoding = Charset.defaultCharset().name();
	}

	/**
	 * Default constructor
	 * 
	 * @throws UnsupportedEncodingException
	 */

	public AbstractConsoleApp(Object target) throws UnsupportedEncodingException {
		this.target = target == null ? this : target;
		err = new PrintStream(System.err, true, encoding);
		out = new PrintStream(System.out, true, encoding);
	}

	public AbstractConsoleApp() throws UnsupportedEncodingException {
		this(null);
	}

	/**
	 * Main entry
	 * 
	 * @throws Exception
	 */
	public void run(String args[]) throws Exception {
		try {
			CommandLine cl = new CommandLine(this);
			ExtList<String> list = new ExtList<String>(args);
			String help = cl.execute(target, "_main", list);
			check();
			if (help != null)
				err.println(help);
		}
		finally {
			err.flush();
			out.flush();
		}
	}

	/**
	 * Main options
	 */

	@Arguments(arg = "cmd ...")
	@Description("Options valid for all commands. Must be given before sub command")
	protected interface MainOptions extends Options {

		@Description("Print exception stack traces when they occur.")
		boolean exceptions();

		@Description("Trace on.")
		boolean trace();

		@Description("Be pedantic about all details.")
		boolean pedantic();

		@Description("Specify a new base directory (default working directory).")
		String base();

		@Description("Do not return error status for error that match this given regular expression.")
		String[] failok();

		@Description("Wait for a key press, might be useful when you want to see the result before it is overwritten by a next command")
		boolean key();

		@Description("The output width, used for wrapping diagnostic output")
		int width();
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
	public void __main(MainOptions opts) throws IOException {

		try {
			setExceptions(opts.exceptions());
			setTrace(opts.trace());
			setPedantic(opts.pedantic());

			if (opts.base() != null)
				setBase(IO.getFile(getBase(), opts.base()));
			else
				setBase(IO.work);
			
			if (opts.width() > 0)
				this.width = opts.width();

			CommandLine handler = opts._command();
			List<String> arguments = opts._();

			if (arguments.isEmpty()) {
				Justif j = new Justif();
				Formatter f = j.formatter();
				handler.help(f, this);
				err.println(j.wrap());
			} else {
				String cmd = arguments.remove(0);
				String help = handler.execute(this, cmd, arguments);
				if (help != null) {
					err.println(help);
				}
			}

		}
		catch (InvocationTargetException t) {
			Throwable tt = t;
			while (tt instanceof InvocationTargetException)
				tt = ((InvocationTargetException) tt).getTargetException();

			exception(tt, "%s", tt.getMessage());
		}
		catch (Throwable t) {
			exception(t, "Failed %s", t);
		}
		finally {
			// Check if we need to wait for it to finish
			if (opts.key()) {
				System.out.println("Hit a key to continue ...");
				System.in.read();
			}
		}

		if (!check(opts.failok())) {
			System.exit(getErrors().size());
		}
	}

}
