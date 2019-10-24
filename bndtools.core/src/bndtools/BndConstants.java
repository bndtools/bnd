package bndtools;

import aQute.bnd.osgi.Constants;

public interface BndConstants extends Constants {

	String	OUTPUT						= "-output";
	String	RUNFW						= "-runfw";
	String	BACKUP_RUNBUNDLES			= "-runbundles-old";

	/**
	 * The URI to which a resource was resolved by OBR
	 */
	String	RESOLUTION_URI_ATTRIBUTE	= "resolution";
}
