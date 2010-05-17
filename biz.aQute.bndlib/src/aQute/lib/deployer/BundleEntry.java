package aQute.lib.deployer;

import aQute.libg.version.*;

class BundleEntry implements Comparable<BundleEntry>{
	final String bsn;
	final Version version;
	
	
	public BundleEntry( String bsn, Version version ) {
		this.bsn = bsn;
		this.version = version;
	}
	
	public int compareTo(BundleEntry o) {
		int result = bsn.compareTo(o.bsn);
		if ( result != 0 )
			return result;
		
		return version.compareTo(o.version);
	}
	
}
