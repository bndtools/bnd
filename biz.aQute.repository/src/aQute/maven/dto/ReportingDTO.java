package aQute.maven.dto;

import aQute.bnd.util.dto.DTO;

/**
 * Section for management of reports and their configuration.
 */
public class ReportingDTO extends DTO {

	/**
	 * If true, then the default reports are not included in the site
	 * generation. This includes the reports in the "Project Info" menu. Note:
	 * While the type of this field is <code>String</code> for technical
	 * reasons, the semantic type is actually <code>Boolean</code>. Default
	 * value is <code>false</code>.
	 */
	public boolean			excludeDefaults	= false;

	/**
	 * Where to store all of the generated reports. The default is
	 * <code>${project.build.directory}/site</code>.
	 */

	public String			outputDirectory;

	/**
	 * The reporting plugins to use and their configuration.
	 */

	public ReportPluginDTO	plugins;
}
