package org.osgi.service.indexer.impl.util;

import java.io.PrintWriter;
import java.util.Arrays;

public class Indent {
	/** the platform specific EOL */
	static private String eol = String.format("%n");

	private final boolean newLine;
	private final int level;
	private final int increment;
	
	/** the indent string */
	private char[] indent;

	public static final Indent NONE = new Indent(false, 0, 0);
	public static final Indent PRETTY = new Indent(true, 0, 2);
	
	private Indent(boolean newLine, int level, int increment) {
		this.newLine = newLine;
		this.level = level;
		this.increment = increment;
		this.indent = new char[level];
		Arrays.fill(this.indent, ' ');
	}

	public void print(PrintWriter pw) {
		if (newLine)
			pw.print(eol);
		pw.print(indent);
	}

	public Indent next() {
		return (increment <= 0) ? this : new Indent(newLine, level + increment, increment);
	}
}
