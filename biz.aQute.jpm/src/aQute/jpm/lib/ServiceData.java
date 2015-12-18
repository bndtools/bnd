package aQute.jpm.lib;

import java.util.ArrayList;
import java.util.List;

import aQute.lib.data.AllowNull;

public class ServiceData extends CommandData {
	public String		args	= "";
	public String		user;
	public String		sdir;
	public String		work;
	public String		lock;
	public String		log;
	public String		epilog	= "";
	public String		prolog	= "";
	public List<String>	after	= new ArrayList<String>();
	@AllowNull
	public String		serviceLib;
	public byte[]		artifact;
}
