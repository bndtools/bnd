package aQute.libg.reporter;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import aQute.libg.generics.*;
import aQute.service.reporter.*;

/**
 * Mainly used for testing where reporters are needed.
 */
public class ReporterAdapter implements Reporter, Report, Runnable {
	final List<String>	errors		= new ArrayList<String>();
	final List<String>	warnings	= new ArrayList<String>();
	final List<LocationImpl> locations = new ArrayList<LocationImpl>();
	
	static class LocationImpl extends Location implements SetLocation {



		public LocationImpl(String e) {
			// TODO Auto-generated constructor stub
		}

		public SetLocation file(String file) {
			this.file = file;
			return this;
		}

		public SetLocation header(String header) {
			this.header = header;
			return this;
		}

		public SetLocation context(String context) {
			this.context = context;
			return this;
		}

		public SetLocation method(String methodName) {
			this.methodName = methodName;
			return this;
		}

		public SetLocation line(int line) {
			this.line = line;
			return this;
		}

		public SetLocation reference(String reference) {
			this.reference = reference;
			return this;
		}
		
	}
	
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

	public SetLocation error(String s, Object... args) {
		String e = String.format(s, args);
		errors.add(e);
		trace("ERROR: %s", e);
		return location(e);
	}

	public SetLocation exception(Throwable t, String s, Object... args) {
		String e = String.format(s, args);
		errors.add(e);
		trace("ERROR: %s", e);
		if (isExceptions() || isTrace())
			if (t instanceof InvocationTargetException)
				t.getCause().printStackTrace(System.err);
			else
				t.printStackTrace(System.err);
		return location(e);
	}

	public SetLocation warning(String s, Object... args) {
		String e = String.format(s, args);
		warnings.add(e);
		trace("warning: %s", e);
		return location(e);
	}

	private SetLocation location(String e) {
		LocationImpl loc = new LocationImpl(e);
		locations.add( loc );
		return loc;
	}

	public void progress(float progress, String s, Object... args) {
		if (out != null) {
			out.format(s, args);
			if (!s.endsWith(String.format("%n")))
				out.format("%n");
		}
	}

	public void trace(String s, Object... args) {
		if (trace && out != null) {
			out.format("# " + s + "%n", args);
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
			System.err.println("Missed the following patterns in the warnings or errors: " + missed);

		report(System.err);
		return false;
	}

	/**
	 * Report the errors and warnings
	 */

	public void report(Appendable out) {
		Formatter f = new Formatter(out);
		report("Error", getErrors(), f);
		report("Warning", getWarnings(), f);
		f.flush();
	}

	void report(String title, Collection<String> list, Formatter f) {
		if (list.isEmpty())
			return;
		f.format(title + (list.size() > 1 ? "s" : "") + "%n");
		int n = 0;
		for (String s : list) {
			f.format("%3s. %s%n", n++, s);
		}
	}

	public boolean getInfo(Report other) {
		return getInfo(other,null);
	}
	public boolean getInfo(Report other, String prefix) {
		boolean ok = true;
		if ( prefix == null)
			prefix = "";
		else
			prefix = prefix + ": ";
		for ( String error : other.getErrors()) {
			errors.add( prefix + error);
			ok = false;
		}
		
		for ( String warning : other.getWarnings()) {
			warnings.add( prefix + warning);
		}
		return ok;
	}

	public Location getLocation(String msg) {
		for ( LocationImpl loc : locations ) {
			if ((loc.message != null) && loc.message.equals(msg))
				return loc;
		}
		return null;
	}

	/**
	 * Handy routine that can be extended by subclasses
	 * so they can run inside the context
	 */
	public void run() {
		throw new UnsupportedOperationException("Must be implemented by subclass");
	}

	/**
	 * Return a messages object bound to this adapter
	 */
	
	public <T> T getMessages(Class<T> c) {
		return ReporterMessages.base(this, c);
	}
}
