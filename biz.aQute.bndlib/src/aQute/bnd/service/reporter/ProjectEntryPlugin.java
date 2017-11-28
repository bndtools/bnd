package aQute.bnd.service.reporter;

import org.osgi.annotation.versioning.ProviderType;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Processor;

/**
 * This plugin extracts a piece of information from a BND project and converts
 * it into a DTO representation. Its result will contributes to the formation of
 * a project metadata report.
 */
@ProviderType
public interface ProjectEntryPlugin {

	/**
	 * Extracts a piece of information from the BND project in arguments.
	 * 
	 * @param project the project to inspect, must not be {@code null}
	 * @param reporter used to report error, must not be {@code null}
	 * @return a DTO representation or {@code null} if no data is available
	 */
	public Object extract(final Project project, final Processor reporter) throws Exception;

	/**
	 * Get the entry name under which this plugin will contribute to the main
	 * report.
	 * 
	 * @return the entry name, never {@code null}
	 */
	public String getEntryName();
}
