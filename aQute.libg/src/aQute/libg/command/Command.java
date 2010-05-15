package aQute.libg.command;

import java.io.*;

import aQute.libg.reporter.*;

public class Command {
	boolean trace;
	Reporter reporter;
	
	
	public int execute(String cmd, String input, StringBuffer stdout,
			StringBuffer stderr) throws Exception {
		int result = 0;
		if ( reporter != null ) {
			reporter.trace("executing cmd: %s with input=%s", cmd, input);
		}
		Process p = Runtime.getRuntime().exec(cmd);
		InputStream out = p.getInputStream();
		try {
			InputStream err = p.getErrorStream();
			try {
				new Collector(out, stdout).start();
				new Collector(err, stdout).start();
				if (input != null) {
					OutputStream inp = p.getOutputStream();
					try {
						inp.write(input.getBytes());
					} finally {
						inp.close();
					}
				}
				result = p.waitFor();
			} finally {
				err.close();
			}
		} finally {
			out.close();
		}
		if ( reporter != null )
			reporter.trace( "cmd %s executed with result=%d, result: %s/%s", cmd, result, stdout, stderr);
		
		return result;
	}

	class Collector extends Thread {
		final InputStream in;
		final StringBuffer sb;

		public Collector(InputStream inputStream, StringBuffer sb) {
			this.in = inputStream;
			this.sb = sb;
		}

		public void run() {
			try {
				int c = in.read();
				while (c >= 0) {
					if ( trace) 
						System.out.print((char)c);
					sb.append((char) c);
					c = in.read();
				}
			} catch (Exception e) {
				sb.append("\n**************************************\n");
				sb.append(e);
				sb.append("\n**************************************\n");
				if ( reporter != null ) {
					reporter.trace("cmd exec: %s", e);
				}
		}
		}
	}
	
	public void setTrace() {
		this.trace=true;
	}
}
