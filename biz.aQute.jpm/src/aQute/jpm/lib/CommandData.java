package aQute.jpm.lib;

import java.util.List;

import aQute.bnd.version.Version;
import aQute.struct.Define;
import aQute.struct.struct;

public class CommandData extends struct {
	public byte[]		sha;											// primary
																		// source
																		// of
																		// data
	public long			time			= System.currentTimeMillis();
	public String		name;
	@Define(optional = true)
	public String		title;
	@Define(optional = true)
	public String		description;
	@Define(optional = true)
	public String		jvmArgs;
	@Define(optional = true)
	public String		jvmLocation;

	public List<byte[]>	dependencies	= list();
	public List<byte[]>	runbundles		= list();
	public String		jpmRepoDir;

	public boolean		installed;
	@Define(optional = true)
	public String		bin;
	@Define(optional = true)
	public String		java;
	public boolean		trace;

	public Version		version;
	@Define(optional = true)
	public String		bsn;
	public String		main;
	/**
	 * Use javaw instead of java
	 */
	public boolean		windows;
}
