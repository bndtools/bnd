package aQute.libg.reporter;

import java.util.*;

/**
 * Mainly used for testing where reporters are needed.
 *
 */
public class ReporterAdapter implements Reporter {
	final List<String> errors = new ArrayList<String>();
	final List<String> warnings = new ArrayList<String>();
	final Formatter out;
	boolean trace;
	
	public ReporterAdapter(){
		out = null;
	}
	
	public ReporterAdapter(Appendable app){
		out = new Formatter(app);
	}
	
	public void error(String s, Object... args) {
		String e = String.format(s, args);
		errors.add(e);
		if ( out!=null)
			out.format("ERROR: %s", e);
	}

	public void warning(String s, Object... args) {
		String e = String.format(s, args);
		warnings.add(e);
		if ( out!=null)
			out.format("WARNING: %s", e);
	}

	public void progress(String s, Object... args) {
		if ( out!=null)
			out.format(s, args);
	}

	public void trace(String s, Object... args) {
		if ( trace && out!=null)
			out.format(s, args);		
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
		this.trace=b;
	}
}
