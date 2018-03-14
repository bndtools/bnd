package aQute.maven.dto;

import aQute.bnd.util.dto.DTO;

/**
 * Section for management of default plugin information for use in a group of
 * POMs.
 */
public class PluginManagementDTO extends DTO {

	/**
	 * The list of plugins to use.
	 */

	public PluginDTO[] plugins;
}
