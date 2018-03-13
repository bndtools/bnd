package aQute.maven.dto;

import aQute.bnd.util.dto.DTO;

/**
 * Download policy.
 */
public class RepositoryPolicyDTO extends DTO {

	/**
	 * Whether to use this repository for downloading this type of artifact.
	 * Note: While the type of this field is <code>String</code> for technical
	 * reasons, the semantic type is actually <code>Boolean</code>. Default
	 * value is <code>true</code>.
	 */
	public boolean				enabled;

	/**
	 * The frequency for downloading updates - can be <code>always,</code>
	 * <code>daily</code> (default), <code>interval:XXX</code> (in minutes) or
	 * <code>never</code> (only if it doesn't exist locally).
	 */
	public static final String	UPDATEPOLICY_DAILY		= "daily";
	public static final String	UPDATEPOLICY_INTERVAL	= "interval:";
	public static final String	UPDATEPOLICY_NEVER		= "never";

	public String				updatePolicy			= UPDATEPOLICY_DAILY;

	/**
	 * What to do when verification of an artifact checksum fails. Valid values
	 * are <code>ignore</code> , <code>fail</code> or <code>warn</code> (the
	 * default).
	 */
	public enum ChecksumPolicy {
		ignore,
		fail,
		warn;
	}

	public ChecksumPolicy checksumPolicy = ChecksumPolicy.warn;
}
