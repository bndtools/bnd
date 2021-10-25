package aQute.remote.api;

import org.osgi.dto.DTO;

public class XThreadDTO extends DTO {

	public String	name;
	public long		id;
	public int		priority;
	public String	state;
	public boolean	isInterrupted;
	public boolean	isAlive;
	public boolean	isDaemon;

}
