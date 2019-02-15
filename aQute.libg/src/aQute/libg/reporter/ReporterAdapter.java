package aQute.libg.reporter;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.strings.Strings;
import aQute.libg.generics.Create;
import aQute.service.reporter.Report;
import aQute.service.reporter.Reporter;

/**
 * Mainly used for testing where reporters are needed.
 */
public class ReporterAdapter implements Reporter, Report, Runnable {
	final List<String>			errors		= new ArrayList<>();
	final List<String>			warnings	= new ArrayList<>();
	final List<LocationImpl>	locations	= new ArrayList<>();

	static class LocationImpl extends Location implements SetLocation {

		public LocationImpl(String e) {
			this.message = e;
		}

		@Override
		public SetLocation file(String file) {
			this.file = (file != null) ? file.replace(File.separatorChar, '/') : null;
			return this;
		}

		@Override
		public SetLocation header(String header) {
			this.header = header;
			return this;
		}

		@Override
		public SetLocation context(String context) {
			this.context = context;
			return this;
		}

		@Override
		public SetLocation method(String methodName) {
			this.methodName = methodName;
			return this;
		}

		@Override
		public SetLocation line(int line) {
			this.line = line;
			return this;
		}

		@Override
		public SetLocation reference(String reference) {
			this.reference = reference;
			return this;
		}

		@Override
		public SetLocation details(Object details) {
			this.details = details;
			return this;
		}

		@Override
		public Location location() {
			return this;
		}

		@Override
		public SetLocation length(int length) {
			this.length = length;
			return this;
		}

	}

	final Formatter	out;
	boolean			trace;
	boolean			pedantic;
	boolean			exceptions;

	/**
	 * @return the exceptions
	 */
	public boolean isExceptions() {
		return exceptions;
	}

	/**
	 * @param exceptions the exceptions to set
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
	 * @param pedantic the pedantic to set
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

	@Override
	public SetLocation error(String s, Object... args) {
		String e = Strings.format(s, args);
		errors.add(e);
		trace("ERROR: %s", e);
		return location(e);
	}

	@Override
	public SetLocation exception(Throwable t, String s, Object... args) {
		StackTraceElement[] stackTrace = t.getStackTrace();
		String method = stackTrace[0].getMethodName();
		String cname = stackTrace[0].getClassName();
		String e = "[" + shorten(cname) + "." + method + "] " + Strings.format(s, args);
		errors.add(e);
		trace("ERROR: %s", e);
		if (isExceptions() || isTrace())
			Exceptions.unrollCause(t, InvocationTargetException.class)
				.printStackTrace(System.err);
		return location(e);
	}

	private String shorten(String cname) {
		int index = cname.lastIndexOf('$');
		if (index < 0)
			index = cname.lastIndexOf('.');

		return cname.substring(index + 1);
	}

	@Override
	public SetLocation warning(String s, Object... args) {
		String e = Strings.format(s, args);
		warnings.add(e);
		trace("warning: %s", e);
		return location(e);
	}

	private SetLocation location(String e) {
		LocationImpl loc = new LocationImpl(e);
		locations.add(loc);
		return loc;
	}

	/**
	 * @deprecated Use SLF4J
	 *             Logger.info(aQute.libg.slf4j.GradleLogging.LIFECYCLE)
	 *             instead.
	 */
	@Override
	@Deprecated
	public void progress(float progress, String s, Object... args) {
		if (out != null) {
			out.format(s, args);
			if (!s.endsWith(String.format("%n"))) {
				out.format("%n");
			}
			out.flush();
		}
	}

	/**
	 */
	@Override
	public void trace(String s, Object... args) {
		if (trace && out != null) {
			out.format("# " + s + "%n", args);
			out.flush();
		}
	}

	@Override
	public List<String> getWarnings() {
		return warnings;
	}

	@Override
	public List<String> getErrors() {
		return errors;
	}

	@Override
	public boolean isPedantic() {
		return false;
	}

	public void setTrace(boolean b) {
		this.trace = b;
	}

	@Override
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
					if (pat.matcher(i.next())
						.find()) {
						i.remove();
						match = true;
					}
				}
				for (Iterator<String> i = warnings.iterator(); i.hasNext();) {
					if (pat.matcher(i.next())
						.find()) {
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
		f.format("%s%s%n", title, list.size() > 1 ? "s" : "");
		int n = 0;
		for (String s : list) {
			f.format("%3s. %s%n", n++, s);
		}
	}

	public boolean getInfo(Report other) {
		return getInfo(other, null);
	}

	public boolean getInfo(Report other, String prefix) {
		addErrors(prefix, other.getErrors());
		addWarnings(prefix, other.getWarnings());
		return other.isOk();
	}

	@Override
	public Location getLocation(String msg) {
		for (LocationImpl loc : locations) {
			if ((loc.message != null) && loc.message.equals(msg))
				return loc;
		}
		return null;
	}

	/**
	 * Handy routine that can be extended by subclasses so they can run inside
	 * the context
	 */
	@Override
	public void run() {
		throw new UnsupportedOperationException("Must be implemented by subclass");
	}

	/**
	 * Return a messages object bound to this adapter
	 */

	public <T> T getMessages(Class<T> c) {
		return ReporterMessages.base(this, c);
	}

	/**
	 * Add a number of errors
	 */

	public void addErrors(String prefix, Collection<String> errors) {
		if (prefix == null)
			prefix = "";
		else
			prefix = prefix + ": ";
		for (String s : errors) {
			this.errors.add(prefix + s);
		}
	}

	/**
	 * Add a number of warnings
	 */

	public void addWarnings(String prefix, Collection<String> warnings) {
		if (prefix == null)
			prefix = "";
		else
			prefix = prefix + ": ";
		for (String s : warnings) {
			this.warnings.add(prefix + s);
		}
	}
}
