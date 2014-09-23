package aQute.bnd.osgi;

import java.util.*;

import aQute.bnd.version.*;

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
 * imports and exports. Headers are translated to {@link Parameters} that
 * contains all headers (the order is maintained). The attribute of each header
 * are maintained in an {@link Attrs}. Each additional file in a header
 * definition will have its own entry (only native code does not work this way).
 * The ':' of directives is considered part of the name. This allows attributes
 * and directives to be maintained in the Attributes map. An important aspect of
 * the specification is to allow the use of wildcards. Wildcards select from a
 * set and can decorate the entries with new attributes. This functionality is
 * implemented in Instructions. Much of the information calculated is in
 * packages. A package is identified by a PackageRef (and a type by a TypeRef).
 * The namespace is maintained by {@link Descriptors}, which here is owned by
 * {@link Analyzer}. A special class, {@link Packages} maintains the attributes
 * that are found in the code.
 * 
 * @version $Revision: 1.2 $
 */
public class About {
	public static Version				_2_3		= new Version(2, 3, 0);
	public static Version				_2_4		= new Version(2, 4, 0);

	public static String[]				CHANGES_2_4	= {
			"Laucher moved to Java 6",
			"Always read bnd files with UTF-8 with a fallback to ISO-8859-1",
			"Full Java 8 support",
			"Added life cycle plugin that can interact with workspace/project creation/deletion",
			"Allow includes in bnd files to specify a URL",
			"Support for Gradle plugin",
			"Support Travis builds",
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
	public static String[]				CHANGES_2_3	= {
			"More aggressive upgrade to a later DS release, current analyzer was missing cases that required a higher version",
			"Allow bnd -version",
			"Added bnd sync command that forces the cache to be the current version",
			"Invoke Dynamic instruction length was missing",
			"Skips Class Constants if not used in the code (in Java 8, a constant expression can still refer to the defining class",
			"Added x86-64 for the processors",
			"For 1.8 (and later), the -eeprofile option specifies either 'auto' or a set of profiles: "
					+ "-eeprofile: compact1=\"java.lang,java.io,...\", compact2=...",
			"Wherever a version range can be used, you can now use a version that starts with a @ (consumer range) "
					+ "or ends with a @ (provider range). I.e. @1.2.3 -> [1.2.3,2) and 1.2.3@ -> [1.2.3,1.3.0) ",
			"Added an ${unescape;...} macro that changes \n, \t etc to their unescaped characters",
			"Warns about '=' signs in DS Component annotation for properties element to catch confusion with bnd Component annotation",
			"Added -runenv to add environment vars in the launched vm",
			"Now correctly supports JUnit 4",
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
			"Added a [bnd changes] command",
			"#388 ${githead} macro, provides the SHA for the current workspace",
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

	public static Map<Version,String[]>	CHANGES		= new TreeMap<Version,String[]>(Collections.reverseOrder());
	static {
		CHANGES.put(_2_3, CHANGES_2_3);
	}

}
