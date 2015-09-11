package aQute.bnd.service.repository;

public enum Phase {
	/**
	 * Submitted by developer, can be used by other developer but is not yet
	 * generally available.
	 */
	STAGING,
	/**
	 * Locked, generally for QA to test in preparation for becoming the next
	 * master
	 */
	LOCKED,
	/**
	 * Curremt master
	 */
	MASTER,

	/**
	 * Retired, no longer available for searching though still available for
	 * existing projects.
	 */
	RETIRED,

	/**
	 * Revoked/withdrawn. Should fail any dependent builds.
	 */
	WITHDRAWN;
}
