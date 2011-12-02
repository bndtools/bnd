package aQute.bnd.apidiff;

import aQute.bnd.service.apidiff.*;
import aQute.lib.osgi.*;

public class APIDifferImpl {

	
	public Diff diff( Jar newer, Jar older ) throws Exception {
		BundleDef newerDef = new BundleDef(newer);
		BundleDef olderDef = new BundleDef(older);
		return new DiffImpl<BundleDef>("",newerDef,olderDef);
	}
	
	
}
