package biz.aQute.bnd.reporter.generator;


/**
 * Defines a set of entry name and their intended content.
 * 
 */
public interface EntryNamesReference {

  /**
   * An arbitrary entry.
   * 
   */
  static final String ANY_ENTRY = "anyEntry";

  /**
   * An arbitrary file content.
   */
  static final String IMPORT_FILE = "importFile";

  /**
   * An arbitrary file content.
   */
  static final String IMPORT_JAR_FILE = "importJarFile";

  /**
   * A list of headers.
   */
  static final String MANIFEST = "manifest";

  /**
   * A list of declarative services descriptions.
   */
  static final String COMPONENTS = "components";

  /**
   * A list of metatypes descriptions.
   */
  static final String METATYPES = "metatypes";

  /**
   * The file name (of folder) in which the analyzed object is backed up.
   */
  static final String FILE_NAME = "fileName";

  /**
   * A list of bundles.
   */
  static final String BUNDLES = "bundles";

  /**
   * A list of projects.
   */
  static final String PROJECTS = "projects";

  /**
   * The common info of a multi-module project (eg; a workspace).
   */
  static final String COMMON_INFO = "commonInfo";

  /**
   * The version, the artifactId and the groupId of the analyzed object.
   */
  static final String MAVEN_COORDINATE = "mavenCoordinate";
}
