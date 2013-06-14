package aQute.jpm.lib;

import java.util.*;

import aQute.bnd.version.*;
import aQute.struct.*;

public class CommandData extends struct {
	public byte[]		sha;			// primary source of data
	public long			time			= System.currentTimeMillis();
	public String		name;
	@Define(optional = true)
	public String		title;
	@Define(optional = true)
	public String		description;
	@Define(optional = true)
	public String		jvmArgs;

	public List<String>	dependencies	= list();
	public List<String>	runbundles		= list();
	public String		jpmRepoDir;

	public boolean		installed;
	@Define(optional=true)
	public String		bin;
	@Define(optional = true)
	public String		java;
	public boolean		trace;

	public Version		version;
	@Define(optional=true)
	public String		bsn;
	public String		main;
}
