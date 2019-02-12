package aQute.remote.plugin;

import java.util.Map;

import aQute.bnd.util.dto.DTO;

/**
 * Definition of the -runremote header
 */
public class RunRemoteDTO extends DTO {
	public Map<String, Object>	__extra;
	public String				name;
	public String				host;
	public String				jmx;
	public int					agent;
	public int					jdb;
	public int					timeout;
	public int					shell		= 0;
	public boolean				reachable	= false;
}
