package biz.aQute.bnd.reporter.generator;

/**
 * Defines a set of entry name and their intended content.
 */
public interface EntryNamesReference {

	/**
	 * An arbitrary entry.
	 */
	String	ANY_ENTRY			= "anyEntry";

	/**
	 * An arbitrary file content.
	 */
	String	IMPORT_FILE			= "importFile";

	/**
	 * An arbitrary file content.
	 */
	String	IMPORT_JAR_FILE		= "importJarFile";

	/**
	 * A list of headers.
	 */
	String	MANIFEST			= "manifest";

	/**
	 * A list of declarative services descriptions.
	 */
	String	COMPONENTS			= "components";

	/**
	 * A list of metatypes descriptions.
	 */
	String	METATYPES			= "metatypes";

	/**
	 * The file name (of folder) in which the analyzed object is backed up.
	 */
	String	FILE_NAME			= "fileName";

	/**
	 * A list of bundles.
	 */
	String	BUNDLES				= "bundles";

	/**
	 * A list of projects.
	 */
	String	PROJECTS			= "projects";

	/**
	 * The common info of a multi-module project (eg; a workspace).
	 */
	String	COMMON_INFO			= "commonInfo";

	/**
	 * A list of code snippet.
	 */
	String	CODE_SNIPPETS		= "codeSnippets";

	/**
	 * The version, the artifactId and the groupId of the analyzed object.
	 */
	String	MAVEN_COORDINATE	= "mavenCoordinate";
}
