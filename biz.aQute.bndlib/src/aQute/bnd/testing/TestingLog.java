package aQute.bnd.testing;

import java.util.*;
import java.util.regex.*;

import org.osgi.framework.*;
import org.osgi.service.log.*;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.testing.TestingLog.*;

/**
 * Provides a log service object to be used in testing. It can filter levels and
 * messages and can trace stack traces and directly output the diagnostic info.
 * At the end it can be checked if there were any messages logged that fell
 * within the set criteria.
 */

@Component(designate = Config.class)
@SuppressWarnings("rawtypes")
public class TestingLog implements LogService {
	boolean			stacktrace;
	boolean			direct;
	int				level;
	long			start	= System.currentTimeMillis();

	List<LogEntry>	entries	= new ArrayList<LogEntry>();
	List<Pattern>	filters	= new ArrayList<Pattern>();

	interface Config {
		boolean stacktrace();

		boolean direct();

		int level();

		String[] filters();
	}

	Config	config;

	@Activate
	void activate(Map<String,Object> props) {
		config = Configurable.createConfigurable(Config.class, props);
		if (config.stacktrace())
			stacktrace();

		if (config.direct())
			direct();

		level(config.level());
		if (config.filters() != null) {
			for (String pattern : config.filters())
				filter(pattern);
		}
	}

	public void log(int level, String message) {
		log(null, level, message, null);
	}

	public void log(int level, String message, Throwable exception) {
		log(null, level, message, exception);

	}

	public void log(ServiceReference sr, int level, String message) {
		log(sr, level, message, null);
	}

	public synchronized void log(final ServiceReference sr, final int level, final String message,
			final Throwable exception) {

		if (exception != null && stacktrace)
			exception.printStackTrace();

		if (level < this.level)
			return;

		for (Pattern p : filters) {
			if (p.matcher(message).find())
				return;
		}

		final long now = System.currentTimeMillis();
		LogEntry entry = new LogEntry() {

			public long getTime() {
				return now;
			}

			public ServiceReference getServiceReference() {
				return sr;
			}

			public String getMessage() {
				return message;
			}

			public int getLevel() {
				return level;
			}

			public Throwable getException() {
				return exception;
			}

			public Bundle getBundle() {
				return null;
			}

			public String toString() {
				StringBuilder sb = new StringBuilder();
				Formatter f = new Formatter(sb);
				try {
					f.format("%6s %-4s %s %s", (now - start + 500) / 1000,
							(sr == null ? "" : sr.getProperty("service.id")), message, (exception == null ? ""
									: exception.getMessage()));
					return sb.toString();
				}
				finally {
					f.close();
				}
			}
		};
		entries.add(entry);
		if (direct)
			System.out.println(entry);
	}

	public List<LogEntry> getEntries() {
		return entries;
	}

	public TestingLog filter(String pattern) {
		filters.add(Pattern.compile(pattern));
		return this;
	}

	public TestingLog stacktrace() {
		stacktrace = true;
		return this;
	}

	public TestingLog direct() {
		direct = true;
		return this;
	}

	public TestingLog errors() {
		return level(LogService.LOG_ERROR);
	}

	public TestingLog warnings() {
		return level(LogService.LOG_WARNING);
	}

	public TestingLog infos() {
		return level(LogService.LOG_INFO);
	}

	public TestingLog debugs() {
		return level(LogService.LOG_DEBUG);
	}

	public TestingLog level(int level) {
		this.level = level;
		return this;
	}

	public TestingLog full() {
		stacktrace = true;
		direct = true;
		level = Integer.MIN_VALUE;
		return this;
	}

	public boolean check(String... patterns) {
		if (entries.isEmpty())
			return true;

		int n = entries.size();

		for (LogEntry le : entries) {
			for (String pattern : patterns) {
				if (le.getMessage().contains(pattern))
					n--;
				else
					System.out.println(le);
			}
		}

		entries.clear();
		return n != 0;
	}

}
