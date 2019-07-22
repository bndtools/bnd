package aQute.bnd.runtime.facade;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Enumeration;
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
		.parseInt(System.getProperty("snapshot.MAX_LOG_ENTRIES", "5000")
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
				@SuppressWarnings("unchecked")
				Enumeration<LogEntry> log = s.getLog();
				while (log.hasMoreElements()) {
					entry(log.nextElement());
				}
				s.addLogListener(LogFacade.this::entry);
				return s;
			}
		};
		this.reader.open();
	}

	private void entry(LogEntry entry) {
		synchronized (queue) {
			//
			// if queue is full we discard oldest entry
			//
			if (queue.size() > MAX_ENTRIES)
				queue.poll();

			queue.add(entry);
		}
	}

	public static class LogDTO extends DTO {
		public List<Map<String, Object>>	log		= new ArrayList<>();
		public List<String>					errors	= new ArrayList<>();
	}

	public LogDTO doLog() {
		LogDTO log = new LogDTO();
		if (reader.isEmpty() && queue.isEmpty()) {
			log.errors.add("No LogReaderService registered, no logs");
		}

		while (true) {
			try {
				LogEntry e;
				synchronized (queue) {
					e = queue.poll();
					if (e == null)
						break;
				}

				Map<String, Object> entry = Util.asBean(LogEntry.class, e);
				if (!entry.containsKey("sequence"))
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
