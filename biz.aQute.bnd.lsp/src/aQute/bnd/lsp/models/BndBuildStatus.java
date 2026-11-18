package aQute.bnd.lsp.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload of the {@code bnd/buildStatus} notification. Plain public fields so
 * that the JSON mapping does not depend on record support of the JSON library.
 */
public class BndBuildStatus {
	public static final String	STATE_IDLE		= "idle";
	public static final String	STATE_BUILDING	= "building";
	public static final String	STATE_DISABLED	= "disabled";

	public static final String	REASON_UNTRUSTED	= "untrusted";

	public String				state				= STATE_IDLE;
	public boolean				enabled;
	public boolean				active;
	public String				reason;
	public String				workspace;
	public List<String>			projects			= new ArrayList<>();
	public String				current;
	public Summary				summary;
	public List<Message>		messages			= new ArrayList<>();

	public static class Summary {
		public List<String>	built	= new ArrayList<>();
		public List<String>	failed	= new ArrayList<>();
		public List<String>	skipped	= new ArrayList<>();
		public int			errors;
		public int			warnings;
	}

	public static class Message {
		public String	project;
		public String	severity;
		public String	message;

		public Message(String project, String severity, String message) {
			this.project = project;
			this.severity = severity;
			this.message = message;
		}
	}
}
