package aQute.bnd.connection.settings;

import java.net.Proxy.Type;
import java.util.List;

import aQute.bnd.util.dto.DTO;
import aQute.libg.glob.Glob;

public class ProxyDTO extends DTO {
	public String	id			= "default";
	public boolean	active		= true;
	public String	mask;
	public Type		protocol	= Type.HTTP;
	public String	username;
	public String	password;
	public int		port		= 8080;
	public String	nonProxyHosts;
	public String	host;

	List<Glob> globs;
}
