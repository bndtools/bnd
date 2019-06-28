package aQute.maven.dto;

import java.util.Map;

import aQute.bnd.util.dto.DTO;

/**
 * Modifications to the build process which is activated based on environmental
 * parameters or command line arguments.
 */
public class ProfileDTO extends DTO {

	/**
	 * The identifier of this build profile. This is used for command line
	 * activation, and identifies profiles to be merged.
	 */
	public String						id	= "default";

	/**
	 * The conditional logic which will automatically trigger the inclusion of
	 * this profile.
	 */
	public ActivationDTO				activation;

	/**
	 * Information required to build the project.
	 */
	public BuildBaseDTO					build;

	/**
	 * The modules (sometimes called subprojects) to build as a part of this
	 * project. Each module listed is a relative path to the directory
	 * containing the module. To be consistent with the way default urls are
	 * calculated from parent, it is recommended to have module names match
	 * artifact ids.
	 */
	public String[]						modules;

	/**
	 * Distribution information for a project that enables deployment of the
	 * site and artifacts to remote web servers and repositories respectively.
	 */
	public DistributionManagementDTO	distributionManagement;

	/**
	 * Properties that can be used throughout the POM as a substitution, and are
	 * used as filters in resources if enabled. The format is
	 * <code>&lt;name&gt;value&lt;/name&gt;</code>.
	 */
	public Map<String, String>			properties;

	/**
	 * Default dependency information for projects that inherit from this one.
	 * The dependencies in this section are not immediately resolved. Instead,
	 * when a POM derived from this one declares a dependency described by a
	 * matching groupId and artifactId, the version and other values from this
	 * section are used for that dependency if they were not already specified.
	 */
	public DependencyManagementDTO		dependencyManagement;

	/**
	 * This element describes all of the dependencies associated with a project.
	 * These dependencies are used to construct a classpath for your project
	 * during the build process. They are automatically downloaded from the
	 * repositories defined in this project. See <a href=
	 * "http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html">
	 * the dependency mechanism</a> for more information.
	 */
	public DependencyDTO[]				dependencies;

	/**
	 * The lists of the remote repositories for discovering dependencies and
	 * extensions.
	 */
	public RepositoryDTO				repositories;

	/**
	 * The lists of the remote repositories for discovering plugins for builds
	 * and reports.
	 */
	public RepositoryDTO[]				pluginRepositories;

	/**
	 *
	 */
	public ReportingDTO					reporting;
}
