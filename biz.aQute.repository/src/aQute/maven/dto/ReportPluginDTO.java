package aQute.maven.dto;

import java.util.Map;

import aQute.bnd.util.dto.DTO;
import aQute.bnd.version.MavenVersion;

/**
 * The <code>&lt;plugin&gt;</code> element contains informations required for a
 * report plugin.
 */
public class ReportPluginDTO extends DTO {
	/**
	 * The group ID of the reporting plugin in the repository.
	 */
	public String				groupId		= "org.apache.maven.plugins";

	/**
	 * The artifact ID of the reporting plugin in the repository.
	 */
	public String				artifactId;

	/**
	 * The version of the reporting plugin to be used.
	 */

	public MavenVersion			version;

	/**
	 * Multiple specifications of a set of reports, each having (possibly)
	 * different configuration. This is the reporting parallel to an
	 * <code>execution</code> in the build.
	 */
	public ReportSetDTO[]		reportSets;

	/**
	 * Whether any configuration should be propagated to child POMs. Note: While
	 * the type of this field is <code>String</code> for technical reasons, the
	 * semantic type is actually <code>Boolean</code>. Default value is
	 * <code>true</code>.
	 */

	public boolean				inherited	= true;

	/**
	 * <p>
	 * The configuration as DOM object.
	 * </p>
	 * <p>
	 * By default, every element content is trimmed, but starting with Maven
	 * 3.1.0, you can add <code>xml:space="preserve"</code> to elements you want
	 * to preserve whitespace.
	 * </p>
	 * <p>
	 * You can control how child POMs inherit configuration from parent POMs by
	 * adding <code>combine.children</code> or <code>combine.self</code>
	 * attributes to the children of the configuration element:
	 * </p>
	 * <ul>
	 * <li><code>combine.children</code>: available values are
	 * <code>merge</code> (default) and <code>append</code>,</li>
	 * <li><code>combine.self</code>: available values are <code>merge</code>
	 * (default) and <code>override</code>.</li>
	 * </ul>
	 * <p>
	 * See <a href="http://maven.apache.org/pom.html#Plugins">POM Reference
	 * documentation</a> and <a href=
	 * "http://plexus.codehaus.org/plexus-utils/apidocs/org/codehaus/plexus/util/xml/Xpp3DomUtils.html">
	 * Xpp3DomUtils</a> for more information.
	 * </p>
	 */

	public Map<String, Object>	configuration;
}
