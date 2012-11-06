package aQute.bnd.help;

import java.util.*;
import java.util.regex.*;

import aQute.bnd.osgi.*;

public class Syntax implements Constants {
	final String							header;
	final String							lead;
	final String							example;
	final Pattern							pattern;
	final String							values;
	final Syntax[]							children;

	static Syntax							version					= new Syntax(
																			VERSION_ATTRIBUTE,
																			"A version range to select the version of an export definition. The default value is 0.0.0.",
																			VERSION_ATTRIBUTE + "=\"[1.2,3.0)\"", null,
																			Verifier.VERSIONRANGE);
	static Syntax							bundle_symbolic_name	= new Syntax(
																			BUNDLE_SYMBOLIC_NAME_ATTRIBUTE,
																			"The bundle symbolic name of the exporting bundle.",
																			BUNDLE_SYMBOLIC_NAME_ATTRIBUTE + "=com.acme.foo.daffy",
																			null, Verifier.SYMBOLICNAME);

	static Syntax							bundle_version			= new Syntax(
																			BUNDLE_VERSION_ATTRIBUTE,
																			"A version range to select the bundle version of the exporting bundle. The default value is 0.0.0.",
																			BUNDLE_VERSION_ATTRIBUTE + "=1.3", null,
																			Verifier.VERSIONRANGE);

	static Syntax							path_version			= new Syntax(
																			VERSION_ATTRIBUTE,
																			"Specifies the range in the repository, project or file.",
																			VERSION_ATTRIBUTE + "=project", "project,type", Pattern
																					.compile("project|type|"
																							+ Verifier.VERSIONRANGE
																									.toString()));

	static final Syntax[]					syntaxes				= new Syntax[] {
			new Syntax(
					BUNDLE_ACTIVATIONPOLICY,
					"The " + BUNDLE_ACTIVATIONPOLICY + " header specifies how the framework should activate the bundle once started.",
					BUNDLE_ACTIVATIONPOLICY + ": lazy", "lazy", Pattern.compile("lazy")),

			new Syntax(BUNDLE_ACTIVATOR,
					"The " + BUNDLE_ACTIVATOR + " header specifies the name of the class used to start and stop the bundle.",
					BUNDLE_ACTIVATOR + ": com.acme.foo.Activator",
					"${classes;implementing;org.osgi.framework.BundleActivator}", Verifier.FQNPATTERN),
			new Syntax(BUNDLE_CATEGORY, "The " + BUNDLE_CATEGORY + " header holds a comma-separated list of category names.",
					BUNDLE_CATEGORY + ": test", "osgi,test,game,util,eclipse,netbeans,jdk,specification", null),
			new Syntax(
					BUNDLE_CLASSPATH,
					"The " + BUNDLE_CLASSPATH + " header defines a comma-separated list of JAR file path names or directories (inside the bundle) containing classes and resources. The period (’.’) specifies the root directory of the bundle’s JAR. The period is also the default.",
					BUNDLE_CLASSPATH + ": /lib/libnewgen.so, .", null, Verifier.PATHPATTERN),
			new Syntax(BUNDLE_CONTACTADDRESS,
					"The " + BUNDLE_CONTACTADDRESS + " header provides the contact address of the vendor.",
					BUNDLE_CONTACTADDRESS + ": 2400 Oswego Road, Austin, TX 74563", null, null),
			new Syntax(BUNDLE_COPYRIGHT,
					"The " + BUNDLE_COPYRIGHT + " header contains the copyright specification for this bundle.",
					BUNDLE_COPYRIGHT + ": OSGi (c) 2002", null, null),
			new Syntax(BUNDLE_DESCRIPTION, "The " + BUNDLE_DESCRIPTION + " header defines a short description of this bundle.",
					BUNDLE_DESCRIPTION + ": Ceci ce n'est pas une bundle", null, null),

			new Syntax(BUNDLE_DOCURL,
					"The " + BUNDLE_DOCURL + " header must contain a URL pointing to documentation about this bundle.",
					BUNDLE_DOCURL + ": http://www.aQute.biz/Code/Bnd", null, Verifier.URLPATTERN),

			new Syntax(
					BUNDLE_ICON,
					"The optional " + BUNDLE_ICON + " header provides a list of (relative) URLs to icons representing this bundle in different sizes.",
					BUNDLE_ICON + ": /icons/bnd.png;size=64", "/icons/bundle.png", Verifier.URLPATTERN, new Syntax("size",
							"Icons size in pixels, e.g. 64.", "size=64", "16,32,48,64,128", Verifier.NUMBERPATTERN)),

			new Syntax(
					BUNDLE_LICENSE,
					"The " + BUNDLE_LICENSE + " header provides an optional machine readable form of license information. The purpose of this header is to automate some of the license processing required by many organizations.",
					BUNDLE_LICENSE + ": http://www.opensource.org/licenses/jabberpl.php",
					"http://www.apache.org/licenses/LICENSE-2.0,<<EXTERNAL>>", Pattern.compile("("
							+ Verifier.URLPATTERN + "|<<EXTERNAL>>)"), new Syntax(DESCRIPTION_ATTRIBUTE,
							"Human readable description of the license.", DESCRIPTION_ATTRIBUTE + "=\"Describe the license here\"",
							null, Verifier.ANYPATTERN), new Syntax(LINK_ATTRIBUTE, "", "", null, Verifier.URLPATTERN)),
			new Syntax(
					BUNDLE_LOCALIZATION,
					"The " + BUNDLE_LOCALIZATION + " header contains the location in the bundle where localization files can be found. The default value is OSGI-INF/l10n/bundle. Translations are by default therefore OSGI-INF/l10n/bundle_de.properties, OSGI-INF/l10n/bundle_nl.properties, etc.",
					BUNDLE_LOCALIZATION + ": OSGI-INF/l10n/bundle", "OSGI-INF/l10n/bundle", Verifier.URLPATTERN),
			new Syntax(
					BUNDLE_MANIFESTVERSION,
					"The " + BUNDLE_MANIFESTVERSION + " header is set by bnd automatically to 2. The header defines that the bundle follows the rules of this specification.",
					"# " + BUNDLE_MANIFESTVERSION + ": 2", "2", Verifier.NUMBERPATTERN),
			new Syntax(
					BUNDLE_NAME,
					"The " + BUNDLE_NAME + " header will be derived from the " + BUNDLE_SYMBOLICNAME + " header if not set. The " + BUNDLE_NAME + " header defines a readable name for this bundle. This should be a short, human-readable name that can contain spaces.",
					BUNDLE_NAME + ": My Bundle", null, Verifier.ANYPATTERN),
			new Syntax(
					BUNDLE_NATIVECODE,
					"The " + BUNDLE_NATIVECODE + " header contains a specification of native code libraries contained in this bundle.",
					BUNDLE_NATIVECODE + ": /lib/http.DLL; osname = QNX; osversion = 3.1",
					null,
					Verifier.PATHPATTERN,
					new Syntax(OSNAME_ATTRIBUTE, "The name of the operating system.", OSNAME_ATTRIBUTE + "=MacOS", Processor.join(
							Verifier.OSNAMES, ","), Verifier.ANYPATTERN),
					new Syntax(OSVERSION_ATTRIBUTE, "Operating System Version.", OSVERSION_ATTRIBUTE + "=3.1", null,
							Verifier.ANYPATTERN),
					new Syntax(LANGUAGE_ATTRIBUTE, "Language ISO 639 code.", LANGUAGE_ATTRIBUTE + "=nl", null, Verifier.ISO639),
					new Syntax(PROCESSOR_ATTRIBUTE, "Processor name.", PROCESSOR_ATTRIBUTE + "=x86", Processor.join(
							Verifier.PROCESSORNAMES, ","), Verifier.ANYPATTERN),
					new Syntax(
							SELECTION_FILTER_ATTRIBUTE,
							"The value of this attribute must be a filter expression that indicates if the native code clause should be selected or not.",
							SELECTION_FILTER_ATTRIBUTE + "=\"(com.acme.windowing=win32)\"", null, Verifier.FILTERPATTERN)),
			new Syntax(
					BUNDLE_REQUIREDEXECUTIONENVIRONMENT,
					"The " + BUNDLE_REQUIREDEXECUTIONENVIRONMENT + " contains a comma-separated list of execution environments that must be present on the Service Platform.",
					BUNDLE_REQUIREDEXECUTIONENVIRONMENT + ": CDC-1.0/Foundation-1.0", Processor.join(Verifier.EES, ","),
					Verifier.ANYPATTERN),

			new Syntax(
					BUNDLE_SYMBOLICNAME,
					"The " + BUNDLE_SYMBOLICNAME + " header specifies a non-localizable name for this bundle. The bundle symbolic name together with a version must identify a unique bundle. The bundle symbolic name should be based on the reverse domain name convention.",
					BUNDLE_SYMBOLICNAME + ": com.acme.foo.daffy;singleton:=true",
					"${p}",
					Verifier.SYMBOLICNAME,
					new Syntax(
							SINGLETON_DIRECTIVE,
							"Indicates that the bundle can only have a single version resolved. A value of true indicates that the bundle is a singleton bundle. The default value is false. The Framework must resolve at most one bundle when multiple versions of a singleton bundle with the same symbolic name are installed. Singleton bundles do not affect the resolution of non-singleton bundles with the same symbolic name.",
							SINGLETON_DIRECTIVE + "=false", "true,false", Verifier.TRUEORFALSEPATTERN),
					new Syntax(
							FRAGMENT_ATTACHMENT_DIRECTIVE,
							"Defines how fragments are allowed to be attached, see the fragments in Fragment Bundles on page 73. The following values are valid for this directive:",
							"", "always|never|resolve-time", Pattern.compile("always|never|resolve-time")), new Syntax(
							BLUEPRINT_WAIT_FOR_DEPENDENCIES_ATTRIBUTE, "", "", "true,false",
							Verifier.TRUEORFALSEPATTERN), new Syntax(BLUEPRINT_TIMEOUT_ATTRIBUTE, "", "",
							"30000,60000,300000", Verifier.NUMBERPATTERN)),

			new Syntax(
					BUNDLE_UPDATELOCATION,
					"The " + BUNDLE_UPDATELOCATION + " header specifies a URL where an update for this bundle should come from. If the bundle is updated, this location should be used, if present, to retrieve the updated JAR file.",
					BUNDLE_UPDATELOCATION + ": http://www.acme.com/Firewall/bundle.jar", null, Verifier.URLPATTERN),

			new Syntax(BUNDLE_VENDOR,
					"The " + BUNDLE_VENDOR + " header contains a human-readable description of the bundle vendor.",
					BUNDLE_VENDOR + ": OSGi Alliance", null, null),

			new Syntax(BUNDLE_VERSION, "The " + BUNDLE_VERSION + " header specifies the version of this bundle.",
					BUNDLE_VERSION + ": 1.23.4.build200903221000", null, Verifier.VERSION),

			new Syntax(
					DYNAMICIMPORT_PACKAGE,
					"The " + DYNAMICIMPORT_PACKAGE + " header contains a comma-separated list of package names that should be dynamically imported when needed.",
					DYNAMICIMPORT_PACKAGE + ": com.acme.plugin.*", "", Verifier.WILDCARDNAMEPATTERN, version,
					bundle_symbolic_name, bundle_version),

			new Syntax(
					EXPORT_PACKAGE,
					"The " + EXPORT_PACKAGE + " header contains a declaration of exported packages.",
					EXPORT_PACKAGE + ": org.osgi.util.tracker;version=1.3",
					"${packages}",
					null,
					new Syntax(
							NO_IMPORT_DIRECTIVE,
							"By default, bnd makes all exports also imports. Adding a " + NO_IMPORT_DIRECTIVE + " to an exported package will make it export only.",
							NO_IMPORT_DIRECTIVE + "=true", "true,false", Verifier.TRUEORFALSEPATTERN),
					new Syntax(
							USES_DIRECTIVE,
							"Calculated by bnd: It is a comma-separated list of package names that are used by the exported package.",
							"Is calculated by bnd", null, null),
					new Syntax(
							MANDATORY_DIRECTIVE,
							"A comma-separated list of attribute names. Note that the use of a comma in the value requires it to be enclosed in double quotes. A bundle importing the package must specify the mandatory attributes, with a value that matches, to resolve to the exported package.",
							MANDATORY_DIRECTIVE + "=\"bar,foo\"", null, null), new Syntax(INCLUDE_DIRECTIVE,
							"A comma-separated list of class names that must be visible to an importer.",
							INCLUDE_DIRECTIVE + "=\"Qux*\"", null, null), new Syntax(EXCLUDE_DIRECTIVE,
							"A comma-separated list of class names that must not be visible to an importer.",
							EXCLUDE_DIRECTIVE + "=\"QuxImpl*,BarImpl\"", null, Verifier.WILDCARDNAMEPATTERN), new Syntax(
							IMPORT_DIRECTIVE, "Experimental.", "", null, null)

			),
			new Syntax(EXPORT_SERVICE, "Deprecated.", EXPORT_SERVICE + ": org.osgi.service.log.LogService",
					"${classes;implementing;*}", null),
			new Syntax(
					FRAGMENT_HOST,
					"The " + FRAGMENT_HOST + " header defines the host bundle for this fragment.",
					FRAGMENT_HOST + ": org.eclipse.swt; bundle-version=\"[3.0.0,4.0.0)\"",
					null,
					null,
					new Syntax(
							EXTENSION_DIRECTIVE,
							"Indicates this extension is a system or boot class path extension. It is only applicable when the Fragment-Host is the System Bundle.",
							EXTENSION_DIRECTIVE + "=framework", "framework,bootclasspath", Pattern
									.compile("framework|bootclasspath")), bundle_version),
			new Syntax(
					IMPORT_PACKAGE,
					"The " + IMPORT_PACKAGE + " header is normally calculated by bnd, however, you can decorate packages or skip packages. The header declares the imported packages for this bundle.",
					IMPORT_PACKAGE + ": !com.exotic.*, com.acme.foo;vendor=ACME, *",
					"${exported_packages}",
					Verifier.WILDCARDNAMEPATTERN,
					new Syntax(REMOVE_ATTRIBUTE_DIRECTIVE,
							"Remove the given attributes from matching imported packages.", REMOVE_ATTRIBUTE_DIRECTIVE + "=foo.*",
							null, Verifier.WILDCARDNAMEPATTERN),
					new Syntax(
							RESOLUTION_DIRECTIVE,
							"Indicates that the packages must be resolved if the value is mandatory, which is the default. If mandatory packages cannot be resolved, then the bundle must fail to resolve. A value of optional indicates that the packages are optional.",
							RESOLUTION_DIRECTIVE + "=optional", "mandatory,optional", Pattern.compile("mandatory|optional")

					), version, bundle_symbolic_name, bundle_version),

			new Syntax(
					REQUIRE_BUNDLE,
					"The " + REQUIRE_BUNDLE + " header specifies the required exports from another bundle.",
					REQUIRE_BUNDLE + ": com.acme.chess",
					null,
					Verifier.WILDCARDNAMEPATTERN,

					new Syntax(
							VISIBILITY_DIRECTIVE,
							"If the value is private (Default), then all visible packages from the required bundles are not re-exported. If the value is reexport then bundles that require this bundle will transitively have access to these required bundle’s exported packages.",
							VISIBILITY_DIRECTIVE + "=private", "private,reexport", Pattern.compile("private|reexport")),

					new Syntax(
							RESOLUTION_DIRECTIVE,
							"If the value is mandatory (default) then the required bundle must exist for this bundle to resolve. If the value is optional, the bundle will resolve even if the required bundle does not exist.",
							RESOLUTION_DIRECTIVE + "=optional", "mandatory,optional", Pattern.compile("mandatory|optional")),

					new Syntax(
							SPLIT_PACKAGE_DIRECTIVE,
							"Indicates how an imported package should be merged when it is split between different exporters. The default is merge-first with warning.",
							SPLIT_PACKAGE_DIRECTIVE + "=merge-first", "merge-first,merge-last,error,first", Pattern
									.compile("merge-first|merge-last|error|first")), bundle_version

			),
			new Syntax(BUILDPATH,
					"Provides the class path for building the jar. The entries are references to the repository.",
					BUILDPATH + "=osgi;version=4.1", "${repo;bsns}", Verifier.SYMBOLICNAME, path_version),
			new Syntax(BUMPPOLICY, "Sets the version bump policy. This is a parameter to the ${version} macro.",
					BUMPPOLICY + "==+0", "==+,=+0,+00", Pattern.compile("[=+-0][=+-0][=+-0]")),

			new Syntax(CONDUIT,
					"Allows a bnd file to point to files which will be returned when the bnd file is build.",
					CONDUIT + "= jar/osgi.jar", null, null),

			new Syntax(
					DEPENDSON,
					"List of project names that this project directly depends on. These projects are always build ahead of this project.",
					DEPENDSON + "=org.acme.cm", "${projects}", null),

			new Syntax(DEPLOYREPO, "Specifies to which repo the project should be deployed.", DEPLOYREPO + "=cnf",
					"${repos}", null),

			new Syntax(DONOTCOPY,
					"Regular expression for names of files and directories that should not be copied when discovered.",
					DONOTCOPY + "=(CVS|\\.svn)", null, null),

			new Syntax(
					EXPORT_CONTENTS,
					"Build the JAR in the normal way but use this header for the " + EXPORT_PACKAGE + " header manifest generation, same format.",
					EXPORT_CONTENTS + "=!*impl*,*;version=3.0", null, null),

			new Syntax(FAIL_OK, "Return with an ok status (0) even if the build generates errors.", FAIL_OK + "=true",
					"true,false", Verifier.TRUEORFALSEPATTERN),

			new Syntax(
					INCLUDE,
					"Include files. If an entry starts with '-', it does not have to exist. If it starts with '~', it must not overwrite any existing properties.",
					INCLUDE + ": -${java.user}/.bnd", null, null),

			new Syntax(
					INCLUDERESOURCE,
					"Include resources from the file system. You can specify a directory, or file. All files are copied to the root, unless a destination directory is indicated.",
					INCLUDERESOURCE + ": lib=jar", null, null),

			new Syntax(
					MAKE,
					"Set patterns for make plugins. These patterns are used to find a plugin that can make a resource that can not be found.",
					MAKE + ": (*).jar;type=bnd; recipe=\"bnd/$1.bnd\"", null, null, new Syntax("type",
							"Type name for plugin.", "type=bnd", "bnd", null), new Syntax("recipe",
							"Recipe for the plugin, can use back references.", "recipe=\"bnd/$1.bnd\"", "bnd", null)),

			new Syntax(MANIFEST, "Directly include a manifest, do not use the calculated manifest.",
					MANIFEST + "=META-INF/MANIFEST.MF", null, null),

			new Syntax(NOEXTRAHEADERS, "Do not generate housekeeping headers.", NOEXTRAHEADERS + "=true", "true,false",
					Verifier.TRUEORFALSEPATTERN),

			new Syntax(NOUSES, "Do not calculate the " + USES_DIRECTIVE + " directive on exports.", NOUSES + "=true", "true,false",
					Verifier.TRUEORFALSEPATTERN),

			new Syntax(PEDANTIC, "Warn about things that are not really wrong but still not right.", PEDANTIC + "=true",
					"true,false", Verifier.TRUEORFALSEPATTERN),

			new Syntax(PLUGIN, "Define the plugins.",
					PLUGIN + "=aQute.lib.spring.SpringComponent,aQute.lib.deployer.FileRepo;location=${repo}", null, null),

			new Syntax(SERVICE_COMPONENT, "The header for Declarative Services.",
					SERVICE_COMPONENT + "=com.acme.Foo?;activate='start'", null, null),

			new Syntax(POM, "Generate a maven pom.", POM + "=true", "true,false", Verifier.TRUEORFALSEPATTERN),

			new Syntax(RELEASEREPO, "Specifies to which repo the project should be released.", RELEASEREPO + "=cnf",
					"${repos}", null),

			new Syntax(REMOVEHEADERS, "Remove all headers that match the regular expressions.",
					REMOVEHEADERS + "=FOO_.*,Proprietary", null, null),
			new Syntax(
					RESOURCEONLY,
					"Normally bnd warns when the JAR does not contain any classes, this option suppresses this warning.",
					RESOURCEONLY + "=true", "true,false", Verifier.TRUEORFALSEPATTERN),
			new Syntax(SOURCES, "Include sources in the jar.", SOURCES + "=true", "true,false",
					Verifier.TRUEORFALSEPATTERN),
			new Syntax(SOURCEPATH, "List of directory names that used to source sources for " + SOURCES + ".",
					SOURCEPATH + ":= src, test", null, null),
			new Syntax(
					SUB,
					"Build a set of bnd files that use this bnd file as a basis. The list of bnd file can be specified with wildcards.",
					SUB + "=com.acme.*.bnd", null, null),
			new Syntax(RUNPROPERTIES, "Properties that are set as system properties before the framework is started.",
					RUNPROPERTIES + "= foo=3, bar=4", null, null),
			new Syntax(RUNSYSTEMPACKAGES, "Add additional system packages to a framework run.",
					RUNSYSTEMPACKAGES + "=com.acme.foo,javax.management", null, null),
			new Syntax(
					RUNBUNDLES,
					"Add additional bundles, specified with their bsn and version like in " + BUILDPATH + ", that are started before the project is run.",
					RUNBUNDLES + "=osgi;version=\"[4.1,4.2)\", junit.junit, com.acme.foo;version=project", null,
					Verifier.SYMBOLICNAME, path_version),
			new Syntax(RUNPATH, "Additional JARs for the VM path, should include the framework.",
					RUNPATH + "=org.eclipse.osgi;version=3.5", null, null, path_version),
			new Syntax(
					RUNVM,
					"Additional arguments for the VM invokation. Keys that start with a - are added as options, otherwise they are treated as -D properties for the VM.",
					RUNVM + "=-Xmax=30", null, null)
																	};

	public final static Map<String,Syntax>	HELP					= new HashMap<String,Syntax>();

	static {
		for (Syntax s : syntaxes) {
			HELP.put(s.header, s);
		}
	}

	public Syntax(String header, String lead, String example, String values, Pattern pattern, Syntax... children) {
		this.header = header;
		this.children = children;
		this.lead = lead;
		this.example = example;
		this.values = values;
		this.pattern = pattern;
	}

	public String getLead() {
		return lead;
	}

	public String getExample() {
		return example;
	}

	public String getValues() {
		return values;
	}

	public String getPattern() {
		return lead;
	}

	public Syntax[] getChildren() {
		return children;
	}

	public String getHeader() {
		return header;
	}

}
