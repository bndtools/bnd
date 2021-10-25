package aQute.remote.agent;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

import aQute.remote.api.Supervisor;
import aQute.remote.api.XLogEntryDTO;

public class OSGiLogListener implements LogListener {

    private final Supervisor supervisor;

	public OSGiLogListener(Supervisor supervisor) {
        this.supervisor = supervisor;
    }

    @Override
	public void logged(LogEntry entry) {
        if (supervisor != null) {
            supervisor.logged(toDTO(entry));
        }
    }

	private XLogEntryDTO toDTO(LogEntry entry) {
		XLogEntryDTO dto = new XLogEntryDTO();

		dto.bundle = XBundleAdmin.toDTO(entry.getBundle());
		dto.message = entry.getMessage();
		dto.level = entry.getLogLevel()
			.name();
		dto.exception = toExceptionString(entry.getException());
		dto.loggedAt = entry.getTime();
		dto.threadInfo = entry.getThreadInfo();
		dto.logger = entry.getLoggerName();

		return dto;
	}

	private String toExceptionString(Throwable exception) {
        if (exception == null) {
            return null;
        }
		StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));

        return sw.toString();
    }

}
