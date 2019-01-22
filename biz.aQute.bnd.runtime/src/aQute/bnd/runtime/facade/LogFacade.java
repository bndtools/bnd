package aQute.bnd.runtime.facade;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.runtime.api.SnapshotProvider;
import aQute.bnd.runtime.util.Util;
import aQute.bnd.util.dto.DTO;

public class LogFacade implements SnapshotProvider {
	private static final int									MAX_ENTRIES	= Integer
		.parseInt(System.getProperty("snapshot.MAX_LOG_ENTRIES", "1000")
			.trim());
	final ServiceTracker<LogReaderService, LogReaderService>	reader;
	final BundleContext											context;
	final Queue<LogEntry>										queue		= new ArrayDeque<>();
	final AtomicInteger											sequencer	= new AtomicInteger();

	public LogFacade(BundleContext context) {
		this.context = context;
		this.reader = new ServiceTracker<LogReaderService, LogReaderService>(context, LogReaderService.class, null) {
			@Override
			public LogReaderService addingService(ServiceReference<LogReaderService> reference) {
				LogReaderService s = super.addingService(reference);
				s.addLogListener(LogFacade.this::entry);
				return s;
			}
		};
		this.reader.open();
	}

	private synchronized void entry(LogEntry entry) {
		queue.add(entry);
		if (queue.size() > MAX_ENTRIES)
			queue.poll();
	}

	public static class LogDTO extends DTO {
		public List<Map<String, Object>>	log		= new ArrayList<>();
		public List<String>					errors	= new ArrayList<>();
	}

	public LogDTO doLog() {
		LogDTO log = new LogDTO();
		if (reader.size() == 0 && queue.size() == 0) {
			log.errors.add("No log service registered");
		}

		for (LogEntry e : queue) {
			try {
				Map<String, Object> entry = Util.asBean(LogEntry.class, e);
				if ( !entry.containsKey("sequence"))
					entry.put("sequence", sequencer.incrementAndGet());
				log.log.add(entry);
			} catch (Exception e1) {
				log.errors.add(Util.toString(e1));
			}
		}
		return log;
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}

	@Override
	public Object getSnapshot() throws Exception {
		return doLog();
	}
}
