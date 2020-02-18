package aQute.bnd.osgi;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import aQute.bnd.version.Version;

/**
 * This package contains a number of classes that assists by analyzing JARs and
 * constructing bundles. The Analyzer class can be used to analyze an existing
 * bundle and can create a manifest specification from proposed (wildcard)
 * Export-Package, Bundle-Includes, and Import-Package headers. The Builder
 * class can use the headers to construct a JAR from the classpath. The Verifier
 * class can take an existing JAR and verify that all headers are correctly set.
 * It will verify the syntax of the headers, match it against the proper
 * contents, and verify imports and exports. A number of utility classes are
 * available. Jar, provides an abstraction of a Jar file. It has constructors
 * for creating a Jar from a stream, a directory, or a jar file. A Jar, keeps a
 * collection Resource's. There are Resource implementations for File, from
 * ZipFile, or from a stream (which copies the data). The Jar tries to minimize
 * the work during build up so that it is cheap to use. The Resource's can be
 * used to iterate over the names and later read the resources when needed.
 * Clazz, provides a parser for the class files. This will be used to define the
 * imports and exports. Headers are translated to
 * {@link aQute.bnd.header.Parameters Parameter} that contains all headers (the
 * order is maintained). The attribute of each header are maintained in an
 * {@link aQute.bnd.header.Attrs Attrs}. Each additional file in a header
 * definition will have its own entry (only native code does not work this way).
 * The ':' of directives is considered part of the name. This allows attributes
 * and directives to be maintained in the Attributes map. An important aspect of
 * the specification is to allow the use of wildcards. Wildcards select from a
 * set and can decorate the entries with new attributes. This functionality is
 * implemented in Instructions. Much of the information calculated is in
 * packages. A package is identified by a PackageRef (and a type by a TypeRef).
 * The namespace is maintained by {@link Descriptors}, which here is owned by
 * {@link Analyzer}. A special class, {@link Packages} maintains the attributes
 * that are found in the code. @version $Revision: 1.2 $
 */
public class About {
	public static final Version					_2_3		= new Version(2, 3, 0);
	public static final Version					_2_4		= new Version(2, 4, 0);
	public static final Version					_3_0		= new Version(3, 0, 0);
	public static final Version					_3_1		= new Version(3, 1, 0);
	public static final Version					_3_2		= new Version(3, 2, 0);
	public static final Version					_3_3		= new Version(3, 3, 0);
	public static final Version					_3_4		= new Version(3, 4, 0);
	public static final Version					_3_5		= new Version(3, 5, 0);
	public static final Version					_4_0		= new Version(4, 0, 0);
	public static final Version					_4_1		= new Version(4, 1, 0);
	public static final Version					_4_2		= new Version(4, 2, 0);
	public static final Version					_4_3		= new Version(4, 3, 0);
	public static final Version					_5_0		= new Version(5, 0, 1);
	public static final Version					CURRENT		= _5_0;

	public static final String[]				CHANGES_5_0	= {};
	public static final String[]				CHANGES_4_3	= {};
	public static final String[]				CHANGES_4_2	= {};
	public static final String[]				CHANGES_4_1	= {};
	public static final String[]				CHANGES_4_0	= {};

	public static final String[]				CHANGES_3_5	= {};

	public static final String[]				CHANGES_3_4	= {};

	public static final String[]				CHANGES_3_3	= {};

	public static final String[]				CHANGES_3_2	= {
		"Default content for bundle", "bndlib: Remove synchronization which causes deadlocks",
		"packages macro: Fix negation processing", "pom: Update -pom processing of Bundle-License header",
		"junit: Write xml declaration to junit xml output", "test: Do not clear test-results folder",
		"class parsing: Parse and process MethodType constant pool entries",
		"Process Designate annotation based upon -metatypeannotations instruction",
		"builder: SubBuilders inherit pedantic/trace", "snapshot: Also support qualifier ending in '-SNAPSHOT'",
		"pom: Use -groupid if set as the default groupid on the -pom instruction",
		"Close classpath  jar files after execution.", "Added support for ${packages;versioned}",
		"Fixupmessage was not properly restricting warnings",
		"Added a ${glob;expo} macro that turns a glob expression into a regex",
		"Bundle-Activator test did not take extend or implement via another interface into account.",
		"Baselining 'cross compiler' fails when using switch on an enum", "Expand the bundle class path",
		"Allow -releaserepo to contain multiple names",
		"Skips comparing class files when a corresponding source can be found",
		"Added HttpClient. bnd now reads ~/.bnd/bnd-settings.xml or ~/.m2/settings.xml to find out proxy & authentication settings",
		"version macro: Use MavenVersion to test for SNAPSHOT version",
		"-pom: Add support for @bsn and @version scoped properties",
		"Bnd DS, metatype, and versioning annotations are deprecated; support to be removed in Bnd 4.0",
		"Added a ${fileuri;file} macro which converts a file path into a file: URI",
		"Processor.trace calls now log the trace information via SLF4J"
	};
	public static final String[]				CHANGES_3_1	= {
		"The embedded repo is expanded into cnf/cache/<bnd-version> folder to avoid potential conflicts.",
		"${uri;<uri>[;<base>]}: Add new uri macro that uses the Processor base as base URI",
		"New properties parser that reports common errors and is faster",
		"Allow donotcopy to match on path as well as on name",
		"Require/Provide Capabilities now allow the use of defaults (They were ignored)",
		"Added an instruction -init, the contents of this instruction will be expanded when the properties are first read",
		"Improved handling of packages with the use of Bundle-ClassPath",
		"Ensure the use of loopback when applicable over localhost",
		"Added a service to prompt, display a message, and/or show error/warnings in the host",
		"-bnd-require now works",
		"Added -prepare instruction that can build external 'things' in the build, e.g. typescript or coffeescript",
		"Added FileSet to libg", "Stand alone bdnrun files", "New macros: base64 and digest for files",
		"Updated Java 8 compact profiles"
	};
	public static final String[]				CHANGES_3_0	= {
		"No longer analyzes the packages on the classpath, only information in manifest is used to find out package information",
		"Added bnd command bnd resolve validate <index.xml> to validate that a repository file is complete",
		"Extended the resource package with many functions to work easily with resources, capabilities and requirements",
		"Properly handles optionals in resolve", "Handles SCR Release 6", "Added knowledge about OSGi R6 Core to OSGi ",
		"Build path can now handle wildcards for bsn",
		"Added a -augment instruction to add capabilities and requirements to resources during bnd resolves",
		"In places where a bundle returns a boolean words like not set, empty string, false, not, off are interpreted as false, "
			+ "otherwise the expression is true. As a slight addition, you can start it with ! to invert the remaining part.",
		"Replaced the biz.aQute.junit tester with biz.aQute.tester. The difference is that the biz.aQute.tester runs as a bundle and does NOT provide JUnit"
			+ " (and broken hamcrest) packages. The old tester can be set with -tester=biz.aQute.junit.",
		"Use source file for SHA when a class has corresponding source in OSGI-OPT/src. This ignores differences in compilers",
		"new maven plugin", "${tstamp} is now consistent for a project build",
		"target attribute is removed in bindex from a DS component", "Support multiple source folders",
		"Launcher changes to support multiple remote launchers", "Better support for error locations on errors",
		"Drop generale JAR urls on JPM repo will analyze the JAR and cerate an entry (though it will of course not be on JPM itself)",
		"-runpath.* adds any provided capabilities to the frameworks (as it already did for packages)",
		"Support for access rules as used in Eclipse", "Publish all artifacts on bintray", "Performance improvements",
		"SearchableRepo now provides the browse URL", "Bindex now supports DS > 1.2",
		"Allowing embedded activators to start before the bundles are started (they normally started after all bundles were started)",
		"Dropping URLs to JPM from Linux failed due to mucky URLs (newlines!!)",
		"No longer a warning when you import from yourself and there is no export metadata (we’re building it!)",
		"Additional conversions in converter; Dictionary, URI/L cleanup",
		"Force the current working directory to be the project base during launching",
		"Version mismatch between pacakge(-)info(.java) and manifest are now not taking qualifier into account ",
		"Missing bundles on -runbundles are now errors", "${isdir} and ${env} now return empty (false) if no value",
		"Additional checks for non-existent imports or empty imports", "-runkeep does now work",
		"${versionmask} macro to use when ${version} is used for other purposes",
		"Added annotation attributes by treating manifest annotations as meta annotations",
		"Fixed #708, versionless import when having split packages where earlier packages have no content",
		"-runproperties is now a merged header", "Added JSONCodec hook", "Gradle plugin supports non-bnd builds better",
		"OSGi Enterprise DS & Metatype annotations",
		"-pom now accepts artifactid, groupid, where, and version properties. General better support for generating POMs",
		"Added a ${stem;<string>} macro that returns the stem (before the extension) of a file name",
		"Added a ${thisfile} macro that returns the name of the properties file of the current processor if it exists."
	};

	public static final String[]				CHANGES_2_4	= {
		"Added checks for imports from private packages and exports of empty packages. These checks are enabled by -check ALL | (EXPORTS|IMPORTS)*. All is recommended but might break builds",
		"Added -runkeep instruction. It turned out that it was impossible to keep the framework directory between restarts",
		"Added ${versionmask} macro that is identical to ${version} but can be used when you want to use the version macro for yourself",
		"Manifest Annotations use any defined methods as attributes from the annotation they are applied to",
		"Made many more headers merged headers", "Added several support methods so bnd can generate a pom",
		"Launcher moved to Java 6", "Always read bnd files with UTF-8 with a fallback to ISO-8859-1",
		"Full Java 8 support", "Added life cycle plugin that can interact with workspace/project creation/deletion",
		"Allow includes in bnd files to specify a URL", "Support for Gradle plugin", "Support Travis builds",
		"Added support for gestalt, allows build tools to communicate out what the environment they run in supports. See -bnd-driver",
		"Full window testing, removed many window bugs",
		"Removed (rather unknown) feature that did not prefix a sub-bundle when the sub-bundle file name "
			+ "started with the project name. The reason was that it caused too much confusion. See bndtools#903",
		"Provide an identity command in bnd that can be used to authorize other systems",
		"Most paths can now be set with their primary key (e.g. -runbundles) but are appended with "
			+ "secondary keys (anything that begins with the primary key  + .*, e.g. '-runbundles.5'). "
			+ "The mergeProperties is the function creating this collective path",
		"Projects can now be selected by version in a path. This "
			+ "enables a project to depend on a version that starts as a project but "
			+ "later becomes a repository entry."
	};
	public static final String[]				CHANGES_2_3	= {
		"More aggressive upgrade to a later DS release, current analyzer was missing cases that required a higher version",
		"Allow bnd -version", "Added bnd sync command that forces the cache to be the current version",
		"Invoke Dynamic instruction length was missing",
		"Skips Class Constants if not used in the code (in Java 8, a constant expression can still refer to the defining class",
		"Added x86-64 for the processors",
		"For 1.8 (and later), the -eeprofile option specifies either 'auto' or a set of profiles: "
			+ "-eeprofile: compact1=\"java.lang,java.io,...\", compact2=...",
		"Wherever a version range can be used, you can now use a version that starts with a @ (consumer range) "
			+ "or ends with a @ (provider range). I.e. @1.2.3 -> [1.2.3,2) and 1.2.3@ -> [1.2.3,1.3.0) ",
		"Added an ${unescape;...} macro that changes \n, \t etc to their unescaped characters",
		"Warns about '=' signs in DS Component annotation for properties element to catch confusion with bnd Component annotation",
		"Added -runenv to add environment vars in the launched vm", "Now correctly supports JUnit 4",
		"Added options to the bnd test command: test names, and setting continuous and trace flags",
		"Included DSTestWiring class which makes it easier to handle dependencies in unit tests, both inside and outside a framework",
		"The replace macro can now take a separator string, this separator was alwas ', '",
		"#477 baseline did not take generics into account for return types",
		"Support R6 Version,ProviderType, and ConsumerType",
		"Testing will look for unresolveds and fail unless -testunresolved=false is set",
		"Cleaned up version augmentation. Finding a version has priority properties, Manifest, "
			+ "package-info.java with Version annotation, packageinfo",
		"Added annotations to create manifest headers: BundleCopyright, BundleDevelopers, BundleContributors, "
			+ "BundleDocURL, RequireCapability, ProvideCapability. These headers work also when applied to a "
			+ "custom annotation that is then applied to a class",
		"Added a ees command in bnd to print out the Execution Environment of a JAR",
		"Verify that the " + Constants.META_PERSISTENCE + " header's locations actually exist",
		"Added a find command to bnd that can find imported or exported packages from a set of bundles",
		"Support X.class instruction in older Groovy (189)",
		"[-fixupmessages] Patterns to fixup errors and warnings, you can remove, move, or replace messages.",
		"Added a [bnd changes] command", "#388 ${githead} macro, provides the SHA for the current workspace",
		"Improved bnd diff and bnd baseline commands. Better output and work better when no files are specied, defaults to project",
		"#414 Error reported when version=project is used in -runbundles",
		"#427 The classes macro and other places that crawl for classes now get a sorted list by the class name.",
		"The bnd diff and baseline command now can take the current project as source for the 2 JARs",
		"The -diffignore parameter (and the -i option in the bnd commands) can now take wildcards",
		"Ant fetches bnd.jar remotely and stores it in the ~/.bnd directory so it is shared",
		"Added an optional :i suffix to instructions for case insensitive matching",
		"Packaged/embedded launch configurations now also update bundles, previously they were only installed.",
		"Removed synthetic methods from the binary compatibility check since the compilers generate different methods.",
		"Added check for filenames that do not work on windows",
		"Implemented the {Processor.since() functionality with the " + Constants.UPTO + " instruction.",
		"#413 Automatically adding the EE to the Require Capability header based on the compiled class file version. Can be disabled with -noee=true",
		"Added 1.7 and 1.8 compiler files and EEs",
		"The package command not faithfully provides the classpath. In the previous version it flattened all jars",
		"#41 Supports parameters in macros: \n\u00A0   ${x;a;b} -> x = ${1}/${2} -> a/b\n${#} an array with arguments. ${0} is the name of the macro.",
		"Automatically excludes files from preprocessing for given extensions. The default list can be overridden with -preprocessmatchers",
		"Uses XML v1.0 instead of v1.1 in some places.",
		"Can now delete variables in ~/.bnd/settings.json with the command line",
		"Better checking for errors on -runbundles and -runpath.",
		"Can now execute actions from the command line with the [bnd action] command",
		"Set two properties (project.junit and project.junit.osgi) that indicate the availability of tests.",
		"Support URLs in the [bnd repo] put command",
		"It is now possible to specify a URL on a plugin path so that if the resource is not on the file system it will get downloaded."
	};

	public static final Map<Version, String[]>	CHANGES		= new TreeMap<>(Collections.reverseOrder());

	static {
		CHANGES.put(_5_0, CHANGES_5_0);
		CHANGES.put(_4_3, CHANGES_4_3);
		CHANGES.put(_4_2, CHANGES_4_2);
		CHANGES.put(_4_1, CHANGES_4_1);
		CHANGES.put(_4_0, CHANGES_4_0);
		CHANGES.put(_3_5, CHANGES_3_5);
		CHANGES.put(_3_4, CHANGES_3_4);
		CHANGES.put(_3_3, CHANGES_3_3);
		CHANGES.put(_3_2, CHANGES_3_2);
		CHANGES.put(_3_1, CHANGES_3_1);
		CHANGES.put(_3_0, CHANGES_3_0);
		CHANGES.put(_2_4, CHANGES_2_4);
		CHANGES.put(_2_3, CHANGES_2_3);
	}

}
