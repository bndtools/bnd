package aQute.bnd.runtime.gogo;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.WeakHashMap;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import aQute.lib.dtoformatter.DTOFormatter;

public class Log implements Closeable {
	final List<LogEntry>										entries	= new ArrayList<>();
	final WeakHashMap<CommandSession, PrintStream>				watches	= new WeakHashMap<>();
	final ServiceTracker<LogService, LogService>				log;
	final ServiceTracker<LogReaderService, LogReaderService>	logReader;
	final BundleContext											context;
	final DTOFormatter											formatter;

	enum Level {
		UNKNOWN(0),
		ERROR(LogService.LOG_ERROR),
		WARNING(LogService.LOG_WARNING),
		INFO(LogService.LOG_INFO),
		DEBUG(LogService.LOG_DEBUG);

		int l;

		Level(int l) {
			this.l = l;
		}
	}

	public Log(BundleContext context, DTOFormatter formatter) {
		this.context = context;
		this.formatter = formatter;
		dtos(formatter);
		this.log = new ServiceTracker<>(context, LogService.class, null);
		this.log.open();
		this.logReader = new ServiceTracker<LogReaderService, LogReaderService>(context, LogReaderService.class, null) {
			@Override
			public LogReaderService addingService(ServiceReference<LogReaderService> reference) {
				LogReaderService service = super.addingService(reference);
				synchronized (entries) {
					service.addLogListener(Log.this::logentry);
					for (@SuppressWarnings("unchecked")
					Enumeration<LogEntry> e = service.getLog(); e.hasMoreElements();) {
						logentry(e.nextElement());
					}
				}
				return service;
			}

			@Override
			public void removedService(ServiceReference<LogReaderService> reference, LogReaderService service) {
				service.addLogListener(Log.this::logentry);
				super.removedService(reference, service);
			}
		};
		logReader.open();
	}

	private void logentry(LogEntry entry) {
		synchronized (entries) {
			entries.add(entry);
		}
		if (entry.getLevel() <= LogService.LOG_ERROR)
			for (PrintStream p : watches.values()) {
				p.println("\n***** " + entry.getMessage());
			}
	}

	@Override
	public void close() throws IOException {
		logReader.close();
		log.close();
	}

	@Descriptor("show the current log")
	public List<LogEntry> log(@Parameter(names = {
		"-l", "--level"
	}, absentValue = "WARNING") String level, @Parameter(names = {
		"-n", "--number"
	}, absentValue = "100") int number

	) {
		return log0(level, number);
	}

	@Descriptor("show the current log")
	public List<LogEntry> ldebug(@Parameter(names = {
		"-n", "--number"
	}, absentValue = "100") int number

	) {
		return log0(Level.DEBUG.toString(), number);
	}

	@Descriptor("show the current log")
	public List<LogEntry> linfo(@Parameter(names = {
		"-n", "--number"
	}, absentValue = "100") int number

	) {
		return log0(Level.INFO.toString(), number);
	}

	private List<LogEntry> log0(String level, int number) {
		Level l = Level.valueOf(level.toUpperCase());
		List<LogEntry> result = new ArrayList<>();
		for (int i = entries.size() - 1; i >= 0 && result.size() <= number; i--) {
			LogEntry entry = entries.get(i);
			if (entry.getLevel() <= l.l) {
				result.add(entry);
			}
		}
		return result;
	}

	private void dtos(DTOFormatter formatter) {
		formatter.build(LogEntry.class)
			.inspect()
			.format("level", e -> level(e.getLevel()))
			.format("bundle", e -> e.getBundle()
				.getBundleId())
			.methods("*")
			.format("time", e -> Instant.ofEpochMilli(e.getTime()))
			.line()
			.format("level", e -> level(e.getLevel()))
			.format("bundle", e -> e.getBundle()
				.getBundleId())
			.method("message")
			.format("time", e -> Instant.ofEpochMilli(e.getTime()))
			.methods("*")
			.remove("location")
			.part()
			.as(e -> e.getMessage());
	}

	private Level level(int n) {
		try {
			return Level.values()[n];
		} catch (Exception e) {
			return Level.UNKNOWN;
		}
	}

	@Descriptor("")
	public void watch(CommandSession session) {
		watches.put(session, session.getConsole());
	}

	@Descriptor("")
	public void unwatch(CommandSession session) {
		watches.remove(session);
	}
}
