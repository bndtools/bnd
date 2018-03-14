package aQute.maven.dto;

import java.util.Map;

import aQute.bnd.util.dto.DTO;

/**
 * Represents a set of reports and configuration to be used to generate them.
 */
public class ReportSetDTO extends DTO {
	/**
	 * The unique id for this report set, to be used during POM inheritance and
	 * profile injection for merging of report sets.
	 */
	public String				id			= "default";

	/**
	 * The list of reports from this plugin which should be generated from this
	 * set.
	 */

	public String[]				reports;

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
