package aQute.jpm.lib;

import aQute.bnd.version.*;

public class ArtifactData {
	public long			time	= System.currentTimeMillis();
	public String		bsn;
	public Version		version;
	public CommandData	command;
	public ServiceData	service;
	public String	verify;
	public String	reason;


	@Override
	public String toString() {
		return "[" + bsn + "]";
	}
}
