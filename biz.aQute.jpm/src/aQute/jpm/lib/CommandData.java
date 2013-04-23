package aQute.jpm.lib;

import java.util.*;

import aQute.bnd.version.*;
import aQute.lib.data.*;
import aQute.struct.*;

public class CommandData extends struct {
	public long			time			= System.currentTimeMillis();
	public String		name;
	public String		title;
	@AllowNull
	public String		description;
	public boolean		force;
	@AllowNull
	public String		jvmArgs			= "";

	public String		main;

	public List<String>	dependencies	= new ArrayList<String>();
	public List<String> runbundles		= new ArrayList<String>();
	public String		jpmRepoDir;
	
	public boolean		installed;
	public byte[]		sha;
	public String		bin;
	public String		java;
	public String		coordinates;
	public boolean		trace;

	public Version		version;
}
