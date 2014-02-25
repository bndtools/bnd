package aQute.bnd.osgi;

import java.util.*;

import aQute.bnd.header.*;
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
	public static String[]				CHANGES_2_3	= {
			"Testing will look for unresolveds and fail unless -testunresolved=false is set",
			"Cleaned up version augmentation. Finding a version has priority properties, Manifest, "
					+ "package-info.java with Version annotation, packageinfo",
			"Added annotations to create manifest headers: BundleCopyright, BundleDevelopers, BundleContributors, "
					+ "BundleDocURL, RequireCapability, ProvideCapability. These headers work also when applied to a "
					+ "custom annotation that is then applied to a class",
			"Added a ees command in bnd to print out the Execution Environment of a JAR",
			"Verify that the Meta-Persistence header's locations actually exist",
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
