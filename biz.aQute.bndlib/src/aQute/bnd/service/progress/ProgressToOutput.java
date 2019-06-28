package aQute.bnd.service.progress;

import java.io.Flushable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressToOutput implements ProgressPlugin {
	final static Logger	logger	= LoggerFactory.getLogger(ProgressToOutput.class);
	final Object		lock	= new Object();
	final Appendable	appendable;
	final String		eol;

	public ProgressToOutput(Appendable appendable, String eol) {
		this.appendable = appendable;
		if (eol == null) {
			if ("ansi".equalsIgnoreCase(System.getenv("TERM"))) {
				eol = "\u001B[0K .\r";
			} else
				eol = "\n";
		}
		this.eol = eol;
	}

	private synchronized void log(String msg, boolean nl, Object... args) {
		try {
			String out = String.format(msg + (nl ? "%n" : eol), args);
			if (out.length() > 100) {
				out = out.substring(0, 50) + ".." + out.substring(out.length() - 50);
			}
			appendable.append(out);
			if (appendable instanceof Flushable)
				((Flushable) appendable).flush();
		} catch (IOException e) {
			logger.error("sending output for progress ", e);
		}
	}

	@Override
	public Task startTask(String name, int size) {
		return new Task() {

			@Override
			public void worked(int units) {
				log("work : %02d %s", false, (units * 100 + size / 2) / size, name);
			}

			@Override
			public void done(String message, Throwable e) {
				if (e != null) {
					log("error:    %s %s", true, name, e.getMessage());
				} else {
					log("done :    %s", false, name);
				}
			}

			@Override
			public boolean isCanceled() {
				return false;
			}
		};
	}

	public void clear() {
		log("", false);
	}
}
