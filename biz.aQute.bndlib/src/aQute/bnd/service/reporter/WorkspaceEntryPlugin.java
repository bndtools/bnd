package aQute.bnd.service.reporter;

import org.osgi.annotation.versioning.ProviderType;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;

/**
 * This plugin extracts a piece of information from a BND workspace and converts
 * it into a DTO representation. Its result will contributes to the formation of
 * a workspace metadata report.
 */
@ProviderType
public interface WorkspaceEntryPlugin {

	/**
	 * Extracts a piece of information from the BND workspace in arguments.
	 * 
	 * @param workspace the workspace to inspect, must not be {@code null}
	 * @param reporter used to report error, must not be {@code null}
	 * @return a DTO representation or {@code null} if no data is available
	 */
	public Object extract(final Workspace workspace, final Processor reporter) throws Exception;

	/**
	 * Get the entry name under which this plugin will contribute to the main
	 * report.
	 * 
	 * @return the entry name, never {@code null}
	 */
	public String getEntryName();
}
