package aQute.bnd.exporter.template;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public @interface Configuration {

	/**
	 * The directory, where runbundles should be placed
	 */
	String runbundlesDir();

	/**
	 * If false, duplicated entries, will get a unique name.
	 */
	boolean runbundlesOverwriteExisting() default true;

	/**
	 * The target to copy the Framework. If empty, the framework will not be
	 * exported. If the String ends with / the framework will keep the original
	 * name. If it is a full file name, the framework will be renamed
	 * accordingly.
	 */
	String frameworkTarget() default "";

	/**
	 * The directory, where runbundles should be placed
	 */
	String runpathDir();

	/**
	 * If false, duplicated entries, will get a unique name.
	 */
	boolean runpathOverwriteExisting() default true;

	/**
	 * The location of the template. Can be empty.
	 */
	String template() default "";

	/**
	 * Creates a META-INF with a MANIFEST and maven metadata (if configured
	 * similar to any other project)
	 */
	boolean metadata() default false;

}
