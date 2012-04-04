package aQute.libg.reporter;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import aQute.libg.generics.*;

/**
 * Mainly used for testing where reporters are needed.
 * 
 */
public class ReporterAdapter implements Reporter {
	final List<String>	errors		= new ArrayList<String>();
	final List<String>	warnings	= new ArrayList<String>();
	final Formatter		out;
	boolean				trace;
	boolean				pedantic;
	boolean				exceptions;

	/**
	 * @return the exceptions
	 */
	public boolean isExceptions() {
		return exceptions;
	}

	/**
	 * @param exceptions
	 *            the exceptions to set
	 */
	public void setExceptions(boolean exceptions) {
		this.exceptions = exceptions;
	}

	/**
	 * @return the out
	 */
	public Formatter getOut() {
		return out;
	}

	/**
	 * @return the trace
	 */
	public boolean isTrace() {
		return trace;
	}

	/**
	 * @param pedantic
	 *            the pedantic to set
	 */
	public void setPedantic(boolean pedantic) {
		this.pedantic = pedantic;
	}

	public ReporterAdapter() {
		out = null;
	}

	public ReporterAdapter(Appendable app) {
		out = new Formatter(app);
	}

	public void error(String s, Object... args) {
		String e = String.format(s, args);
		errors.add(e);
		if (out != null)
			out.format("ERROR: %s", e);
	}

	public void exception(Throwable t, String s, Object... args) {
		String e = String.format(s, args);
		errors.add(e);
		if (out != null)
			out.format("ERROR: %s", e);
		if (isExceptions() || isTrace())
			t.printStackTrace(System.err);
	}

	public void warning(String s, Object... args) {
		String e = String.format(s, args);
		warnings.add(e);
		if (out != null)
			out.format("WARNING: %s", e);
	}

	public void progress(String s, Object... args) {
		if (out != null)
			out.format(s, args);
	}

	public void trace(String s, Object... args) {
		if (trace && out != null) {
			out.format(s, args);
			out.format("\n");
			out.flush();
		}
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public List<String> getErrors() {
		return errors;
	}

	public boolean isPedantic() {
		return false;
	}

	public void setTrace(boolean b) {
		this.trace = b;
	}

	public boolean isOk() {
		return errors.isEmpty();
	}

	public boolean isPerfect() {
		return isOk() && warnings.isEmpty();
	}

	public boolean check(String... pattern) {
		Set<String> missed = Create.set();

		if (pattern != null) {
			for (String p : pattern) {
				boolean match = false;
				Pattern pat = Pattern.compile(p);
				for (Iterator<String> i = errors.iterator(); i.hasNext();) {
					if (pat.matcher(i.next()).find()) {
						i.remove();
						match = true;
					}
				}
				for (Iterator<String> i = warnings.iterator(); i.hasNext();) {
					if (pat.matcher(i.next()).find()) {
						i.remove();
						match = true;
					}
				}
				if (!match)
					missed.add(p);

			}
		}
		if (missed.isEmpty() && isPerfect())
			return true;

		if (!missed.isEmpty())
			error("Missed the following patterns in the warnings or errors: %s", missed);

		return false;
	}

	/**
	 * Report the errors and warnings
	 */

	public void report(PrintStream out) {
		report("Error", getErrors(), out);
		report("Warning", getWarnings(), out);
	}

	void report(String title, Collection<String> list, PrintStream out) {
		if (list.isEmpty())
			return;

		out.println(title + (list.size() > 1 ? "s" : ""));
		int n=0;
		for (String s : list) {
			out.printf("%3s. %s\n", n++, s);
		}
	}

}
