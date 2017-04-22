package biz.aQute.resolve;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

import aQute.lib.io.IO;

public class ResolverLogger implements LogService {

	public static final int		DEFAULT_LEVEL	= 4;

	public static final int		LOG_ERROR		= 1;
	public static final int		LOG_WARNING		= 2;
	public static final int		LOG_INFO		= 3;
	public static final int		LOG_DEBUG		= 4;

	private final File			file;
	private final PrintWriter	printer;

	private int					level;

	private String				log;

	public ResolverLogger() {
		this(DEFAULT_LEVEL);
	}

	public ResolverLogger(int level) {
		try {
			this.level = level;
			file = File.createTempFile("tmp", ".log");
			file.deleteOnExit();
			printer = IO.writer(file, UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public ResolverLogger(int level, PrintStream out) {
		this.level = level;
		file = null;
		printer = new PrintWriter(out);
	}

	public void log(int level, String msg, Throwable throwable) {
		String s = "";
		s = s + msg;
		if (throwable != null)
			s = s + " (" + throwable + ")";
		switch (level) {
			case LOG_DEBUG :
				printer.println("DEBUG: " + s);
				break;
			case LOG_ERROR :
				printer.println("ERROR: " + s);
				if (throwable != null) {
					throwable.printStackTrace(printer);
				}
				break;
			case LOG_INFO :
				printer.println("INFO: " + s);
				break;
			case LOG_WARNING :
				printer.println("WARNING: " + s);
				break;
			default :
				printer.println("UNKNOWN[" + level + "]: " + s);
		}
		printer.flush();
		log = null;
	}

	public String getLog() {
		if (log == null) {

			try {
				printer.flush();
				if (file.length() <= 8001) {
					log = IO.collect(file);
				} else {

					StringBuilder sb = new StringBuilder(10000);

					sb.append("Log too large. Split from ")
							.append(file.getAbsolutePath())
							.append("\nsize ")
							.append((file.length() + 512) / 1024)
							.append(" Kb\n===================\n");

					byte[] buffer = new byte[4000];
					RandomAccessFile raf = new RandomAccessFile(file, "r");

					raf.readFully(buffer);
					sb.append(new String(buffer, UTF_8));
					sb.append("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv\n");

					raf.seek(raf.length() - buffer.length);
					raf.readFully(buffer);
					String s = new String(buffer, UTF_8);
					sb.append(s);

					raf.close();
					log = sb.toString();
				}
			} catch (Exception e) {
				log = e.getMessage();
			}
		}
		return log;
	}

	@Override
	public void finalize() {
		IO.delete(file);
	}

	public int getLogLevel() {
		return level;
	}

	@Override
	public void log(int level, String message) {
		log(level, message, null);
	}

	@Override
	public void log(ServiceReference sr, int level, String message) {
		log(level, message, null);
	}

	@Override
	public void log(ServiceReference sr, int level, String message, Throwable exception) {
		log(level, message, exception);
	}
}
