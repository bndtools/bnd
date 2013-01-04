package aQute.jpm.lib;

import java.io.*;
import java.util.*;

import aQute.lib.data.*;
import aQute.struct.*;

public class CommandData extends struct {
	public long			time			= System.currentTimeMillis();
	public String		name;

	@AllowNull
	public String		description;
	public boolean		force;
	@AllowNull
	public String		jvmArgs			= "";

	public String		main;

	public List<String>	dependencies	= new ArrayList<String>();
	public boolean		installed;
	public byte[]		sha;
	public File			bin;
	public String	coordinates;

}
