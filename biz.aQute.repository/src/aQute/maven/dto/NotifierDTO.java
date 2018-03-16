package aQute.maven.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Configures one method for notifying users/developers when a build breaks.
 */
public class NotifierDTO {
	/**
	 * The mechanism used to deliver notifications
	 */
	public String				type			= "mail";

	/**
	 * Whether to send notifications on error
	 */
	public boolean				sendOnError		= true;

	/**
	 * Whether to send notifications on failure.
	 */

	public boolean				sendOnFailure	= true;

	/**
	 * Whether to send notifications on warning.
	 */

	public boolean				sendOnWarning	= true;
	/**
	 * Whether to send notifications on success.
	 */

	public boolean				sendOnSuccess	= true;

	/**
	 * <b>Deprecated</b>. Where to send the notification to - eg email address.
	 */

	@Deprecated
	public String				address;

	/**
	 * Extended configuration specific to this notifier goes here.
	 */

	public Map<String, String>	configuration	= new HashMap<>();
}
