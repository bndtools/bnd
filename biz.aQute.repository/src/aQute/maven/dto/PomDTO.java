package aQute.maven.dto;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import aQute.bnd.version.MavenVersion;

/**
 * The &lt;code&gt;&amp;lt;project&amp;gt;&lt;/code&gt; element is the root of
 * the descriptor. The following table lists all of the possible child elements.
 * The &lt;code&gt;&amp;lt;project&amp;gt;&lt;/code&gt; element is the root of
 * the descriptor. The following table lists all of the possible child elements.
 */

public class PomDTO {

	/**
	 * Declares to which version of project descriptor this POM conforms.<
	 */
	public String						modelVersion			= "4.0.0";

	/**
	 * The location of the parent project, if one exists. Values from the parent
	 * project will be the default for this project if they are left
	 * unspecified. The location is given as a group ID, artifact ID and
	 * version.
	 */
	public ParentDTO					parent;

	/**
	 * A universally unique identifier for a project. It is normal to use a
	 * fully-qualified package name to distinguish it from other projects with a
	 * similar name (eg. &lt;code&gt;org.apache.maven&lt;/code&gt;).
	 */
	public String						groupId;

	/**
	 * The identifier for this artifact that is unique within the group given by
	 * the group ID. An artifact is something that is either produced or used by
	 * a project. Examples of artifacts produced by Maven for a project include:
	 * JARs, source and binary distributions, and WARs.
	 */
	public String						artifactId;

	/**
	 * The current version of the artifact produced by this project.
	 */
	public MavenVersion					version;

	/**
	 * The type of artifact this project produces, for example
	 * &lt;code&gt;jar&lt;/code&gt; &lt;code&gt;war&lt;/code&gt;
	 * &lt;code&gt;ear&lt;/code&gt; &lt;code&gt;pom&lt;/code&gt;. Plugins can
	 * create their own packaging, and therefore their own packaging types, so
	 * this list does not contain all possible types.
	 */
	public String						packaging				= "jar";

	/**
	 * The full name of the project.
	 */

	public String						name;

	/**
	 * A detailed description of the project, used by Maven whenever it needs to
	 * describe the project, such as on the web site. While this element can be
	 * specified as CDATA to enable the use of HTML tags within the description,
	 * it is discouraged to allow plain text representation. If you need to
	 * modify the index page of the generated web site, you are able to specify
	 * your own instead of adjusting this text.
	 */

	public String						description;

	/**
	 * The URL to the project's homepage. <br />
	 * <b>Default value is</b>: parent value [+ path adjustment] + artifactId
	 */
	public URI							url;

	/**
	 * The year of the project's inception, specified with 4 digits. This value
	 * is used when generating copyright notices as well as being informational.
	 */
	public int							inceptionYear;

	/**
	 * This element describes various attributes of the organization to which
	 * the project belongs. These attributes are utilized when documentation is
	 * created (for copyright notices and links).
	 */
	public OrganizationDTO				organization;

	/**
	 * This element describes all of the licenses for this project. Each license
	 * is described by a <code>license</code> element, which is then described
	 * by additional elements. Projects should only list the license(s) that
	 * applies to the project and not the licenses that apply to dependencies.
	 * If multiple licenses are listed, it is assumed that the user can select
	 * any of them, not that they must accept all.
	 */
	public LicenseDTO[]					licenses				= new LicenseDTO[0];

	/**
	 * Describes the committers of a project.
	 */

	public DeveloperDTO[]				developers				= new DeveloperDTO[0];

	/**
	 * Describes the contributors to a project that are not yet committers.
	 */
	public DeveloperDTO[]				contributors			= new DeveloperDTO[0];

	/**
	 * Contains information about a project's mailing lists.
	 */

	public MailingListDTO[]				mailingLists			= new MailingListDTO[0];

	/**
	 * Describes the prerequisites in the build environment for this project.
	 */

	public PrerequisitesDTO				prerequisites			= new PrerequisitesDTO();

	/**
	 * The modules (sometimes called subprojects) to build as a part of this
	 * project. Each module listed is a relative path to the directory
	 * containing the module. To be consistent with the way default urls are
	 * calculated from parent, it is recommended to have module names match
	 * artifact ids.
	 */

	String[]							modules;

	/**
	 * Specification for the SCM used by the project, such as CVS, Subversion,
	 * etc.
	 */
	public ScmDTO						scm						= new ScmDTO();

	/**
	 * The project's issue management system information.
	 */
	public IssueManagementDTO			issueManagement			= new IssueManagementDTO();

	/**
	 * The project's continuous integration information.
	 */

	public CiManagementDTO				ciManagement			= new CiManagementDTO();

	/**
	 * Distribution information for a project that enables deployment of the
	 * site and artifacts to remote web servers and repositories respectively.
	 */
	public DistributionManagementDTO	distributionManagement	= new DistributionManagementDTO();

	/**
	 * Properties that can be used throughout the POM as a substitution, and are
	 * used as filters in resources if enabled. The format is
	 * <code>&lt;name&gt;value&lt;/name&gt;</code>.
	 */
	public Map<String, String>			properties				= new HashMap<>();

	/**
	 * Default dependency information for projects that inherit from this one.
	 * The dependencies in this section are not immediately resolved. Instead,
	 * when a POM derived from this one declares a dependency described by a
	 * matching groupId and artifactId, the version and other values from this
	 * section are used for that dependency if they were not already specified.
	 */
	public DependencyManagementDTO		dependencyManagement	= new DependencyManagementDTO();
	/**
	 * This element describes all of the dependencies associated with a project.
	 * These dependencies are used to construct a classpath for your project
	 * during the build process. They are automatically downloaded from the
	 * repositories defined in this project. See <a href=
	 * "http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html">
	 * the dependency mechanism</a> for more information.
	 */

	public DependencyDTO[]				dependencies			= new DependencyDTO[0];

	/**
	 * The lists of the remote repositories for discovering dependencies and
	 * extensions.
	 */

	public RepositoryDTO[]				repositories			= new RepositoryDTO[0];

	/**
	 * The lists of the remote repositories for discovering plugins for builds
	 * and reports.
	 */
	public RepositoryDTO[]				pluginRepositories		= new RepositoryDTO[0];

	/**
	 * Information required to build the project.
	 */
	public BuildDTO						build					= new BuildDTO();

	/**
	 * This element includes the specification of report plugins to use to
	 * generate the reports on the Maven-generated site. These reports will be
	 * run when a user executes <code>mvn site</code>. All of the reports will
	 * be included in the navigation bar for browsing.
	 */

	public ReportingDTO					reporting				= new ReportingDTO();

	/**
	 * A listing of project-local build profiles which will modify the build
	 * process when activated.
	 */

	ProfileDTO[]						profiles				= new ProfileDTO[0];
}
