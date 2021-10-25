package aQute.remote.api;

import org.osgi.dto.DTO;

public class XLogEntryDTO extends DTO {

	public XBundleDTO	bundle;
	public String		level;
	public String		message;
	public String		exception;
	public long			loggedAt;
	public String		threadInfo;
	public String		logger;

}
