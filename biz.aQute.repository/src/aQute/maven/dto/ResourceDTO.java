package aQute.maven.dto;

import aQute.bnd.util.dto.DTO;

/**
 * This element describes all of the classpath resources associated with a
 * project or unit tests.
 */
public class ResourceDTO extends DTO {
	/**
	 * Describe the resource target path. The path is relative to the
	 * target/classes directory (i.e.
	 * <code>${project.build.outputDirectory}</code>). For example, if you want
	 * that resource to appear in a specific package (
	 * <code>org.apache.maven.messages</code>), you must specify this element
	 * with this value: <code>org/apache/maven/messages</code>. This is not
	 * required if you simply put the resources in that directory structure at
	 * the source, however.
	 */
	public String	targetPatg;

	/**
	 * Whether resources are filtered to replace tokens with parameterised
	 * values or not. The values are taken from the <code>properties</code>
	 * element and from the properties in the files listed in the
	 * <code>filters</code> element. Note: While the type of this field is
	 * <code>String</code> for technical reasons, the semantic type is actually
	 * <code>Boolean</code>. Default value is <code>false</code>.
	 */
	public boolean	filtering	= false;

	/**
	 * Describe the directory where the resources are stored. The path is
	 * relative to the POM.
	 */

	public String	directory;

	/**
	 * A list of patterns to include, e.g. <code>**&#47;*.xml</code>.
	 */

	public String[]	includes;

	/**
	 * A list of patterns to exclude, e.g. <code>**&#47;*.xml</code>
	 */

	public String[]	excludes;
}
