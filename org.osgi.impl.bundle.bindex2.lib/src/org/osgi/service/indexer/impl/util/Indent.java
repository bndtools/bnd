package org.osgi.service.indexer.impl.util;

import java.io.PrintWriter;

public class Indent {
	
	private final boolean newLine;
	private final int level;
	private final int increment;
	
	public static final Indent NONE = new Indent(false, 0, 0);
	public static final Indent PRETTY = new Indent(true, 0, 2);
	
	private Indent(boolean newLine, int level, int increment) {
		this.newLine = newLine;
		this.level = level;
		this.increment = increment;
	}

	public void print(PrintWriter pw) {
		if (newLine)
			pw.print('\n');
		int n = level;
		while (n-- > 0)
			pw.print(' ');
	}

	public Indent next() {
		return (increment <= 0) ? this : new Indent(newLine, level + increment, increment);
	}
}
