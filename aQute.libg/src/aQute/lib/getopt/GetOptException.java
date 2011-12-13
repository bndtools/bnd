package aQute.lib.getopt;

import java.lang.reflect.*;

public class GetOptException extends IllegalArgumentException {
	private static final long	serialVersionUID	= 1L;
	final Method m;
	final String[] args;
	final int i;
	
	public GetOptException(String string, Method m, String[] args, int i) {
		super(string);
		this.m = m;
		this.args = args;
		this.i = i;
	}

}
