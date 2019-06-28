package aQute.bnd.connection.settings;

import aQute.bnd.util.dto.DTO;

public class ServerDTO extends DTO {
	public String	id		= "default";
	public String	username;
	public String	password;

	/**
	 * The private key location used to authenticate.
	 */
	public String	privateKey;
	/**
	 * The pass phrase used in conjunction with the privateKey to authenticate.
	 */
	public String	passphrase;

	public String	match;
	public boolean	verify	= true;
	public String	trust;
}
