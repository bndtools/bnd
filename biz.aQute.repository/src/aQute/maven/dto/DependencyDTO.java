package aQute.maven.dto;

import aQute.bnd.util.dto.DTO;
import aQute.bnd.version.MavenVersion;

/**
 * The <code>&lt;dependency&gt;</code> element contains information about a
 * dependency of the project.
 */
public class DependencyDTO extends DTO {

	/**
	 * The project group that produced the dependency, e.g.
	 * <code>org.apache.maven</code>.
	 */
	public String		groupId;

	/**
	 * The unique id for an artifact produced by the project group, e.g.
	 * <code>maven-artifact</code>.
	 */
	public String		artifactId;

	/**
	 * The version of the dependency, e.g. <code>3.2.1</code>. In Maven 2, this
	 * can also be specified as a range of versions.
	 */
	public MavenVersion	version;

	/**
	 * The type of dependency. While it usually represents the extension on the
	 * filename of the dependency, that is not always the case. A type can be
	 * mapped to a different extension and a classifier. The type often
	 * corresponds to the packaging used, though this is also not always the
	 * case. Some examples are <code>jar</code>, <code>war</code>,
	 * <code>ejb-client</code> and <code>test-jar</code>: see
	 * <a href="../maven-core/artifact-handlers.html">default artifact
	 * handlers</a> for a list. New types can be defined by plugins that set
	 * <code>extensions</code> to <code>true</code>, so this is not a complete
	 * list.
	 */
	public String		type	= "jar";

	/**
	 * The classifier of the dependency. It is appended to the filename after
	 * the version. This allows:
	 * <ul>
	 * <li>refering to attached artifact, for example <code>sources</code> and
	 * <code>javadoc</code>: see
	 * <a href="../maven-core/artifact-handlers.html">default artifact
	 * handlers</a> for a list,</li>
	 * <li>distinguishing two artifacts that belong to the same POM but were
	 * built differently. For example, <code>jdk14</code> and <code>jdk15</code>
	 * .</li>
	 * </ul>
	 */
	public String		classifier;

	/**
	 * The scope of the dependency - <code>compile</code>, <code>runtime</code>,
	 * <code>test</code>, <code>system</code>, and <code>provided</code>. Used
	 * to calculate the various classpaths used for compilation, testing, and so
	 * on. It also assists in determining which artifacts to include in a
	 * distribution of this project. For more information, see <a href=
	 * "http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html">
	 * the dependency mechanism</a>.
	 */

	public enum Scope {
		compile,
		runtime,
		test,
		system,
		provided;
	}

	public Scope			scope;

	/**
	 * FOR SYSTEM SCOPE ONLY. Note that use of this property is
	 * <b>discouraged</b> and may be replaced in later versions. This specifies
	 * the path on the filesystem for this dependency. Requires an absolute path
	 * for the value, not relative. Use a property that gives the machine
	 * specific absolute path, e.g. <code>${java.home}</code>.
	 */
	public String			systemPath;

	/**
	 * Lists a set of artifacts that should be excluded from this dependency's
	 * artifact list when it comes to calculating transitive dependencies.
	 */
	public ExclusionDTO[]	exclusions;

	/**
	 * Indicates the dependency is optional for use of this library. While the
	 * version of the dependency will be taken into account for dependency
	 * calculation if the library is used elsewhere, it will not be passed on
	 * transitively. Note: While the type of this field is <code>String</code>
	 * for technical reasons, the semantic type is actually <code>Boolean</code>
	 * . Default value is <code>false</code>.
	 */
	public String			optional;

}
