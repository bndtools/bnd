package aQute.jpm.lib;

import java.util.*;

import aQute.bnd.version.*;

public class ArtifactData {
	public long				time	= System.currentTimeMillis();
	public String			bsn;
	public List<Version>	version;

	@Override
	public String toString() {
		return "[" + bsn + "]";
	}
}
