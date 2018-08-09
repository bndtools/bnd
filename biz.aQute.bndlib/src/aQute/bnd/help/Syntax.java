package aQute.bnd.help;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import aQute.bnd.help.instructions.BuilderInstructions;
import aQute.bnd.help.instructions.LauncherInstructions;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.version.Version;
import aQute.lib.strings.Strings;

public class Syntax implements Constants {
	final String							header;
	final String							lead;
	final String							example;
	final Pattern							pattern;
	final String							values;
	final Syntax[]							children;

	static Syntax							version					= new Syntax(VERSION_ATTRIBUTE,
		"A version range to select the version of an export definition. The default value is 0.0.0.",
		VERSION_ATTRIBUTE + "=\"[1.2,3.0)\"", null, Verifier.VERSIONRANGE);
	static Syntax							bundle_symbolic_name	= new Syntax(BUNDLE_SYMBOLIC_NAME_ATTRIBUTE,
		"The bundle symbolic name of the exporting bundle.", BUNDLE_SYMBOLIC_NAME_ATTRIBUTE + "=com.acme.foo.daffy",
		null, Verifier.SYMBOLICNAME);

	static Syntax							bundle_version			= new Syntax(BUNDLE_VERSION_ATTRIBUTE,
		"A version range to select the bundle version of the exporting bundle. The default value is 0.0.0.",
		BUNDLE_VERSION_ATTRIBUTE + "=1.3", null, Verifier.VERSIONRANGE);

	static Syntax							path_version			= new Syntax(VERSION_ATTRIBUTE,
		"Specifies the range in the repository, project or file.", VERSION_ATTRIBUTE + "=project", "project,type",
		Pattern.compile("project|type|" + Verifier.VERSIONRANGE.toString()));

	static final Syntax[]					syntaxes				= new Syntax[] {
		new Syntax(".bnd", "Home directory usage (~/.bnd) in bnd.", null, null, null,
			new Syntax("build-deps",
				"Stores build dependencies of bnd from gradle, ant, etc. In general, bnd will be "
					+ "among this. The files in this directory must be fully versioned",
				"~/.bnd/biz.aQute.bnd-2.2.0.jar", null, null),
			new Syntax("settings.json",
				"Contains the settings used by bnd in json format. These settings are maintained by "
					+ "bnd command line (bnd help settings). These settings can be used through macros "
					+ "and can provide passwords, user ids, and platform specific settings. Names starting with"
					+ "a dot (.) are considered protected",
				"{\"id\":\"30...001\",\"map\":{\".github.secret\":\"xxxxxx\",\"github.user\":\"minime\","
					+ "\"email\":\"Peter.Kriens@aQute.biz\"},\"secret\":\"308...CC56\"}",
				null, null, new Syntax("email", "The user's email address", null, null, null),
				new Syntax("id", "The public key for this machine", null, null, null),
				new Syntax("secret", "The private key for this machine", null, null, null)),
			new Syntax("caches/shas",
				"Directory with sha artifacts. The sha is the name of the "
					+ "directory, it contains the artifact with a normal bsn-version.jar name",
				null, null, null)),
		new Syntax(org.osgi.framework.Constants.BUNDLE_ACTIVATIONPOLICY,
			"The " + org.osgi.framework.Constants.BUNDLE_ACTIVATIONPOLICY
				+ " header specifies how the framework should activate the bundle once started.",
			org.osgi.framework.Constants.BUNDLE_ACTIVATIONPOLICY + ": lazy", "lazy", Pattern.compile("lazy")),

		new Syntax(org.osgi.framework.Constants.BUNDLE_ACTIVATOR,
			"The " + org.osgi.framework.Constants.BUNDLE_ACTIVATOR
				+ " header specifies the name of the class used to start and stop the bundle.",
			org.osgi.framework.Constants.BUNDLE_ACTIVATOR + ": com.acme.foo.Activator",
			"${classes;implementing;org.osgi.framework.BundleActivator}",
			Verifier.FQNPATTERN),
		new Syntax(org.osgi.framework.Constants.BUNDLE_CATEGORY,
			"The " + org.osgi.framework.Constants.BUNDLE_CATEGORY
				+ " header holds a comma-separated list of category names.",
			org.osgi.framework.Constants.BUNDLE_CATEGORY + ": test",
			"osgi,test,game,util,eclipse,netbeans,jdk,specification", null),
		new Syntax(org.osgi.framework.Constants.BUNDLE_CLASSPATH, "The " + org.osgi.framework.Constants.BUNDLE_CLASSPATH
			+ " header defines a comma-separated list of JAR file path names or directories (inside the bundle) containing classes and resources. The period (’.’) specifies the root directory of the bundle’s JAR. The period is also the default.",
			org.osgi.framework.Constants.BUNDLE_CLASSPATH + ": /lib/libnewgen.so, .", null, Verifier.PATHPATTERN),
		new Syntax(org.osgi.framework.Constants.BUNDLE_CONTACTADDRESS,
			"The " + org.osgi.framework.Constants.BUNDLE_CONTACTADDRESS
				+ " header provides the contact address of the vendor.",
			org.osgi.framework.Constants.BUNDLE_CONTACTADDRESS + ": 2400 Oswego Road, Austin, TX 74563", null, null),
		new Syntax(BUNDLE_DEVELOPERS, "Defines the primary developers of this bundle", BUNDLE_DEVELOPERS
			+ ": Peter.Kriens@aQute.biz;name='Peter Kriens Ing';organization=aQute;organizationUrl='http://www.aQute.biz';roles=ceo;timezone=+1",
			null, null),
		new Syntax(BUNDLE_SCM, "Defines the information about the source code of the bundle", BUNDLE_SCM
			+ ": url=https://github.com/bndtools/bnd, connection=scm:git:https://github.com/bndtools/bnd.git, developerConnection=scm:git:git@github.com:bndtools/bnd.git",
			null, null),
		new Syntax(BUNDLE_CONTRIBUTORS, "Defines the people that contrbuted to this bundle", BUNDLE_CONTRIBUTORS
			+ ": Peter.Kriens@aQute.biz;name='Peter Kriens Ing';organization=aQute;organizationUrl='http://www.aQute.biz';roles=ceo;timezone=+1",
			null, null, new Syntax("name", "The display name of the developer", "name='Peter Kriens'", null, null),
			new Syntax("organization", "The display name of organization that employs the developer",
				"organization='aQute'", null, null),																																																																																																										//
			new Syntax("roles", "Roles played by the developer in this bundle's project (see Maven)", "roles=ceo", null,
				null),																																																																																																																		//
			new Syntax("timezone", "Timezone in offset of UTC this developer usually resides in", "timezone+2", null,
				null),																																																																																																																		//
			new Syntax("organizationUrl", "The URL of the developer's organization",
				"organizationURL='http://www.aQute.biz'", null, null)),
		new Syntax(org.osgi.framework.Constants.BUNDLE_COPYRIGHT,
			"The " + org.osgi.framework.Constants.BUNDLE_COPYRIGHT
				+ " header contains the copyright specification for this bundle.",
			org.osgi.framework.Constants.BUNDLE_COPYRIGHT + ": OSGi (c) 2002", null, null),
		new Syntax(org.osgi.framework.Constants.BUNDLE_DESCRIPTION,
			"The " + org.osgi.framework.Constants.BUNDLE_DESCRIPTION
				+ " header defines a short description of this bundle.",
			org.osgi.framework.Constants.BUNDLE_DESCRIPTION + ": Ceci ce n'est pas une bundle", null, null),

		new Syntax(org.osgi.framework.Constants.BUNDLE_DOCURL,
			"The " + org.osgi.framework.Constants.BUNDLE_DOCURL
				+ " header must contain a URL pointing to documentation about this bundle.",
			org.osgi.framework.Constants.BUNDLE_DOCURL + ": http://www.aQute.biz/Code/Bnd", null, Verifier.URLPATTERN),

		new Syntax(org.osgi.framework.Constants.BUNDLE_ICON, "The optional " + org.osgi.framework.Constants.BUNDLE_ICON
				+ " header provides a list of (relative) URLs to icons representing this bundle in different sizes.",
			org.osgi.framework.Constants.BUNDLE_ICON + ": /icons/bnd.png;size=64", "/icons/bundle.png",
			Verifier.URLPATTERN,
			new Syntax("size", "Icons size in pixels, e.g. 64.", "size=64", "16,32,48,64,128", Verifier.NUMBERPATTERN)),

		new Syntax(org.osgi.framework.Constants.BUNDLE_LICENSE, "The " + org.osgi.framework.Constants.BUNDLE_LICENSE
			+ " header provides an optional machine readable form of license information. The purpose of this header is to automate some of the license processing required by many organizations.",
			org.osgi.framework.Constants.BUNDLE_LICENSE + ": http://www.opensource.org/licenses/jabberpl.php",
			"http://www.apache.org/licenses/LICENSE-2.0,<<EXTERNAL>>",
			Pattern.compile("(" + Verifier.URLPATTERN + "|<<EXTERNAL>>)"),
			new Syntax(DESCRIPTION_ATTRIBUTE, "Human readable description of the license.",
				DESCRIPTION_ATTRIBUTE + "=\"Describe the license here\"", null, Verifier.ANYPATTERN),
			new Syntax(LINK_ATTRIBUTE, "", "", null, Verifier.URLPATTERN)),
		new Syntax(org.osgi.framework.Constants.BUNDLE_LOCALIZATION, "The "
			+ org.osgi.framework.Constants.BUNDLE_LOCALIZATION
			+ " header contains the location in the bundle where localization files can be found. The default value is OSGI-INF/l10n/bundle. Translations are by default therefore OSGI-INF/l10n/bundle_de.properties, OSGI-INF/l10n/bundle_nl.properties, etc.",
			org.osgi.framework.Constants.BUNDLE_LOCALIZATION + ": OSGI-INF/l10n/bundle", "OSGI-INF/l10n/bundle",
			Verifier.URLPATTERN),
		new Syntax(org.osgi.framework.Constants.BUNDLE_MANIFESTVERSION, "The "
			+ org.osgi.framework.Constants.BUNDLE_MANIFESTVERSION
			+ " header is set by bnd automatically to 2. The header defines that the bundle follows the rules of this specification.",
			"# " + org.osgi.framework.Constants.BUNDLE_MANIFESTVERSION + ": 2", "2", Verifier.NUMBERPATTERN),
		new Syntax(org.osgi.framework.Constants.BUNDLE_NAME, "The " + org.osgi.framework.Constants.BUNDLE_NAME
			+ " header will be derived from the " + org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME
			+ " header if not set. The " + org.osgi.framework.Constants.BUNDLE_NAME
			+ " header defines a readable name for this bundle. This should be a short, human-readable name that can contain spaces.",
			org.osgi.framework.Constants.BUNDLE_NAME + ": My Bundle", null, Verifier.ANYPATTERN),
		new Syntax(BUNDLE_NATIVECODE,
			"The " + BUNDLE_NATIVECODE
				+ " header contains a specification of native code libraries contained in this bundle.",
			BUNDLE_NATIVECODE + ": /lib/http.DLL; osname = QNX; osversion = 3.1", null, Verifier.PATHPATTERN,
			new Syntax(OSNAME_ATTRIBUTE, "The name of the operating system.", OSNAME_ATTRIBUTE + "=MacOS",
				Processor.join(Verifier.OSNAMES, ","), Verifier.ANYPATTERN),
			new Syntax(OSVERSION_ATTRIBUTE, "Operating System Version.", OSVERSION_ATTRIBUTE + "=3.1", null,
				Verifier.ANYPATTERN),
			new Syntax(LANGUAGE_ATTRIBUTE, "Language ISO 639 code.", LANGUAGE_ATTRIBUTE + "=nl", null, Verifier.ISO639),
			new Syntax(PROCESSOR_ATTRIBUTE, "Processor name.", PROCESSOR_ATTRIBUTE + "=x86",
				Processor.join(Verifier.PROCESSORNAMES, ","), Verifier.ANYPATTERN),
			new Syntax(SELECTION_FILTER_ATTRIBUTE,
				"The value of this attribute must be a filter expression that indicates if the native code clause should be selected or not.",
				SELECTION_FILTER_ATTRIBUTE + "=\"(com.acme.windowing=win32)\"", null, Verifier.FILTERPATTERN)),
		new Syntax(org.osgi.framework.Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "The "
			+ org.osgi.framework.Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT
			+ " contains a comma-separated list of execution environments that must be present on the Service Platform.",
			org.osgi.framework.Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT + ": CDC-1.0/Foundation-1.0",
			Processor.join(Verifier.EES, ","),
			Verifier.ANYPATTERN),

		new Syntax(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME, "The "
			+ org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME
			+ " header specifies a non-localizable name for this bundle. The bundle symbolic name together with a version must identify a unique bundle. The bundle symbolic name should be based on the reverse domain name convention.",
			org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME + ": com.acme.foo.daffy;singleton:=true", "${p}",
			Verifier.SYMBOLICNAME,
			new Syntax(SINGLETON_DIRECTIVE,
				"Indicates that the bundle can only have a single version resolved. A value of true indicates that the bundle is a singleton bundle. The default value is false. The Framework must resolve at most one bundle when multiple versions of a singleton bundle with the same symbolic name are installed. Singleton bundles do not affect the resolution of non-singleton bundles with the same symbolic name.",
				SINGLETON_DIRECTIVE + "=false", "true,false", Verifier.TRUEORFALSEPATTERN),
			new Syntax(FRAGMENT_ATTACHMENT_DIRECTIVE,
				"Defines how fragments are allowed to be attached, see the fragments in Fragment Bundles on page 73. The following values are valid for this directive:",
				"", "always|never|resolve-time", Pattern.compile("always|never|resolve-time")),
			new Syntax(BLUEPRINT_WAIT_FOR_DEPENDENCIES_ATTRIBUTE, "", "", "true,false", Verifier.TRUEORFALSEPATTERN),
			new Syntax(BLUEPRINT_TIMEOUT_ATTRIBUTE, "", "", "30000,60000,300000", Verifier.NUMBERPATTERN)),

		new Syntax(org.osgi.framework.Constants.BUNDLE_UPDATELOCATION, "The "
			+ org.osgi.framework.Constants.BUNDLE_UPDATELOCATION
			+ " header specifies a URL where an update for this bundle should come from. If the bundle is updated, this location should be used, if present, to retrieve the updated JAR file.",
			org.osgi.framework.Constants.BUNDLE_UPDATELOCATION + ": http://www.acme.com/Firewall/bundle.jar", null,
			Verifier.URLPATTERN),

		new Syntax(org.osgi.framework.Constants.BUNDLE_VENDOR,
			"The " + org.osgi.framework.Constants.BUNDLE_VENDOR
				+ " header contains a human-readable description of the bundle vendor.",
			org.osgi.framework.Constants.BUNDLE_VENDOR + ": OSGi Alliance", null, null),

		new Syntax(org.osgi.framework.Constants.BUNDLE_VERSION,
			"The " + org.osgi.framework.Constants.BUNDLE_VERSION + " header specifies the version of this bundle.",
			org.osgi.framework.Constants.BUNDLE_VERSION + ": 1.23.4.build200903221000", null, Verifier.VERSION),

		new Syntax(COMPRESSION, "Set the compression for writing JARs. Default is deflate", COMPRESSION + "=store",
			"deflate,store", Pattern.compile("deflate|store")),

		new Syntax(org.osgi.framework.Constants.DYNAMICIMPORT_PACKAGE, "The "
			+ org.osgi.framework.Constants.DYNAMICIMPORT_PACKAGE
			+ " header contains a comma-separated list of package names that should be dynamically imported when needed.",
			org.osgi.framework.Constants.DYNAMICIMPORT_PACKAGE + ": com.acme.plugin.*", "",
			Verifier.WILDCARDNAMEPATTERN, version,
			bundle_symbolic_name, bundle_version),

		new Syntax(org.osgi.framework.Constants.EXPORT_PACKAGE,
			"The " + org.osgi.framework.Constants.EXPORT_PACKAGE
				+ " header contains a declaration of exported packages.",
			org.osgi.framework.Constants.EXPORT_PACKAGE + ": org.osgi.util.tracker;version=1.3", "${packages}", null,
			new Syntax(NO_IMPORT_DIRECTIVE,
				"By default, bnd makes all exports also imports. Adding a " + NO_IMPORT_DIRECTIVE
					+ " to an exported package will make it export only.",
				NO_IMPORT_DIRECTIVE + "=true", "true,false", Verifier.TRUEORFALSEPATTERN),
			new Syntax(USES_DIRECTIVE,
				"Calculated by bnd: It is a comma-separated list of package names that are used by the exported package.",
				"Is calculated by bnd", null, null),
			new Syntax(MANDATORY_DIRECTIVE,
				"A comma-separated list of attribute names. Note that the use of a comma in the value requires it to be enclosed in double quotes. A bundle importing the package must specify the mandatory attributes, with a value that matches, to resolve to the exported package.",
				MANDATORY_DIRECTIVE + "=\"bar,foo\"", null, null),
			new Syntax(INCLUDE_DIRECTIVE, "A comma-separated list of class names that must be visible to an importer.",
				INCLUDE_DIRECTIVE + "=\"Qux*\"", null, null),
			new Syntax(EXCLUDE_DIRECTIVE,
				"A comma-separated list of class names that must not be visible to an importer.",
				EXCLUDE_DIRECTIVE + "=\"QuxImpl*,BarImpl\"", null, Verifier.WILDCARDNAMEPATTERN),
			new Syntax(IMPORT_DIRECTIVE, "Experimental.", "", null, null)

		),
		new Syntax(CONDITIONAL_PACKAGE, "The " + CONDITIONAL_PACKAGE
			+ " works as private package but will only include the packages when they are imported. When this header is used, bnd will recursively add packages that match the patterns until there are no more additions.",
			CONDITIONAL_PACKAGE + ": com.*", "${packages}", null),
		new Syntax(CONDITIONALPACKAGE, "The " + CONDITIONALPACKAGE
			+ " works as private package but will only include the packages when they are imported. When this header is used, bnd will recursively add packages that match the patterns until there are no more additions.",
			CONDITIONALPACKAGE + ": com.*", "${packages}", null),
		new Syntax(PRIVATE_PACKAGE, "The " + PRIVATE_PACKAGE
			+ " header contains a declaration of packages to be included in the resulting bundle, the only difference is, is that these packages will not be exported.",
			PRIVATE_PACKAGE + ": com.*", "${packages}", null),
		new Syntax(PRIVATEPACKAGE, "The " + PRIVATEPACKAGE
			+ " header contains a declaration of packages to be included in the resulting bundle, the only difference is, is that these packages will not be exported.",
			PRIVATEPACKAGE + ": com.*", "${packages}", null),
		new Syntax(org.osgi.framework.Constants.EXPORT_SERVICE, "Deprecated.",
			org.osgi.framework.Constants.EXPORT_SERVICE + ": org.osgi.service.log.LogService",
			"${classes;implementing;*}", null),
		new Syntax(org.osgi.framework.Constants.FRAGMENT_HOST,
			"The " + org.osgi.framework.Constants.FRAGMENT_HOST + " header defines the host bundle for this fragment.",
			org.osgi.framework.Constants.FRAGMENT_HOST + ": org.eclipse.swt; bundle-version=\"[3.0.0,4.0.0)\"", null,
			null,
			new Syntax(EXTENSION_DIRECTIVE,
				"Indicates this extension is a system or boot class path extension. It is only applicable when the "
					+ org.osgi.framework.Constants.FRAGMENT_HOST + " is the System Bundle.",
				EXTENSION_DIRECTIVE + "=framework", "framework,bootclasspath",
				Pattern.compile("framework|bootclasspath")),
			bundle_version),
		new Syntax(org.osgi.framework.Constants.IMPORT_PACKAGE, "The " + org.osgi.framework.Constants.IMPORT_PACKAGE
			+ " header is normally calculated by bnd, however, you can decorate packages or skip packages. The header declares the imported packages for this bundle.",
			org.osgi.framework.Constants.IMPORT_PACKAGE + ": !com.exotic.*, com.acme.foo;vendor=ACME, *",
			"${exported_packages}",
			Verifier.WILDCARDNAMEPATTERN,
			new Syntax(REMOVE_ATTRIBUTE_DIRECTIVE, "Remove the given attributes from matching imported packages.",
				REMOVE_ATTRIBUTE_DIRECTIVE + "=foo.*", null, Verifier.WILDCARDNAMEPATTERN),
			new Syntax(RESOLUTION_DIRECTIVE,
				"Indicates that the packages must be resolved if the value is mandatory, which is the default. If mandatory packages cannot be resolved, then the bundle must fail to resolve. A value of optional indicates that the packages are optional.",
				RESOLUTION_DIRECTIVE + "=optional", "mandatory,optional", Pattern.compile("mandatory|optional")

			), version, bundle_symbolic_name, bundle_version),

		new Syntax(org.osgi.framework.Constants.REQUIRE_BUNDLE,
			"The " + org.osgi.framework.Constants.REQUIRE_BUNDLE
				+ " header specifies the required exports from another bundle.",
			org.osgi.framework.Constants.REQUIRE_BUNDLE + ": com.acme.chess", null, Verifier.WILDCARDNAMEPATTERN,

			new Syntax(VISIBILITY_DIRECTIVE,
				"If the value is private (Default), then all visible packages from the required bundles are not re-exported. If the value is reexport then bundles that require this bundle will transitively have access to these required bundle’s exported packages.",
				VISIBILITY_DIRECTIVE + "=private", "private,reexport", Pattern.compile("private|reexport")),

			new Syntax(RESOLUTION_DIRECTIVE,
				"If the value is mandatory (default) then the required bundle must exist for this bundle to resolve. If the value is optional, the bundle will resolve even if the required bundle does not exist.",
				RESOLUTION_DIRECTIVE + "=optional", "mandatory,optional", Pattern.compile("mandatory|optional")),

			new Syntax(SPLIT_PACKAGE_DIRECTIVE,
				"Indicates how an imported package should be merged when it is split between different exporters. The default is merge-first with warning.",
				SPLIT_PACKAGE_DIRECTIVE + "=merge-first", "merge-first,merge-last,error,first",
				Pattern.compile("merge-first|merge-last|error|first")),
			bundle_version

		),
		new Syntax(org.osgi.framework.Constants.PROVIDE_CAPABILITY,
			"The " + org.osgi.framework.Constants.PROVIDE_CAPABILITY
				+ " header specifies that a bundle provides a set of Capabilities, other bundles can use "
				+ org.osgi.framework.Constants.REQUIRE_CAPABILITY + " to match this capability.",
			org.osgi.framework.Constants.PROVIDE_CAPABILITY
				+ ": com.acme.dictionary; from:String=nl; to=de; version:Version=3.4",
			null,
			Verifier.WILDCARDNAMEPATTERN,

			new Syntax(EFFECTIVE_DIRECTIVE,
				"(resolve) Specifies the time a capabiltity is available, either resolve (default) or another name. The OSGi framework resolver only considers Capabilities without an effective directive or effective:=resolve. Capabilties with other values for the effective directive can be considered by an external agent.",
				EFFECTIVE_DIRECTIVE + "=resolve", "resolve or another word", null),

			new Syntax(USES_DIRECTIVE,
				"The uses directive lists package names that are used by this Capability. This information is intended to be used for uses constraints.",
				USES_DIRECTIVE + "='foo,bar,baz'", null, null)),
		new Syntax(org.osgi.framework.Constants.REQUIRE_CAPABILITY,
			"The " + org.osgi.framework.Constants.REQUIRE_CAPABILITY
				+ " header specifies that a bundle requires other bundles to provide a Capability, see "
				+ org.osgi.framework.Constants.PROVIDE_CAPABILITY,
			org.osgi.framework.Constants.REQUIRE_CAPABILITY + ": com.microsoft; filter:='(&(api=win32)(version=7))'",
			null,
			Verifier.WILDCARDNAMEPATTERN,

			new Syntax(EFFECTIVE_DIRECTIVE,
				"(resolve) Specifies the time a Requirement is considered, either resolve (default) or another name. The OSGi framework resolver only considers Requirements without an effective directive or effective:=resolve. Other Requirements can be considered by an external agent. Additonal names for the effective directive should be registered with the OSGi Alliance.",
				EFFECTIVE_DIRECTIVE + "=resolve", "resolve or another word", null),

			new Syntax(RESOLUTION_DIRECTIVE,
				"(mandatory|optional) A mandatory Requirement forbids the bundle to resolve when the Requirement is not satisfied; an optional Requirement allows a bundle to resolve even if the Requirement is not satisfied. No wirings are created when this Requirement cannot be resolved, this can result in Class Not Found Exceptions when the bundle attempts to use a package that was not resolved because it was optional.",
				RESOLUTION_DIRECTIVE + "=optional", "mandatory,optional", Pattern.compile("mandatory|optional")),
			new Syntax(FILTER_DIRECTIVE,
				" (Filter) A filter expression that is asserted on the Capabilities belonging to the given namespace. The matching of the filter against the Capability is done on one Capability at a time. A filter like (&(a=1)(b=2)) matches only a Capability that specifies both attributes at the required value, not two capabilties that each specify one of the attributes correctly. A filter is optional, if no filter directive is specified the Requirement always matches.",
				FILTER_DIRECTIVE + "= (&(a=1)(b=2))", null, null)),
		new Syntax(BUILDPATH,
			"Provides the class path for building the jar. The entries are references to the repository.",
			BUILDPATH + "=osgi;version=4.1", "${repo;bsns}", Verifier.SYMBOLICNAME, path_version),
		new Syntax(BUMPPOLICY, "Sets the version bump policy. This is a parameter to the ${version} macro.",
			BUMPPOLICY + "==+0", "==+,=+0,+00", Pattern.compile("[=+-0][=+-0][=+-0]")),

		new Syntax(CONDUIT, "Allows a bnd file to point to files which will be returned when the bnd file is build.",
			CONDUIT + "= jar/osgi.jar", null, null),

		new Syntax(DEPENDSON,
			"List of project names that this project directly depends on. These projects are always build ahead of this project.",
			DEPENDSON + "=org.acme.cm", "${projects}", null),

		new Syntax(DEPLOYREPO, "Specifies to which repo the project should be deployed.", DEPLOYREPO + "=cnf",
			"${repos}", null),

		new Syntax(DONOTCOPY,
			"Regular expression for names of files and directories that should not be copied when discovered.",
			DONOTCOPY + "=(CVS|\\.svn)", null, null),

		new Syntax(EXPORT_CONTENTS,
			"Build the JAR in the normal way but use this header for the " + org.osgi.framework.Constants.EXPORT_PACKAGE
				+ " header manifest generation, same format.",
			EXPORT_CONTENTS + "=!*impl*,*;version=3.0", null, null),

		new Syntax(FAIL_OK, "Return with an ok status (0) even if the build generates errors.", FAIL_OK + "=true",
			"true,false", Verifier.TRUEORFALSEPATTERN),
		new Syntax(FIXUPMESSAGES,
			"Rearrange and/or replace errors and warnings. Errors that should be ignore or be warnings (and vice versa for warnings) can be moved or rewritten by specifying a globbing pattern for the message.",
			FIXUPMESSAGES + "='Version mismatch';replace:='************* ${@}';restrict:=error", null, null),
		new Syntax(INCLUDE,
			"Include files. If an entry starts with '-', it does not have to exist. If it starts with '~', it must not overwrite any existing properties.",
			INCLUDE + ": -${java.user}/.bnd", null, null),
		new Syntax(INVALIDFILENAMES,
			"Specify a regular expressions to match against file or directory names. This is the segment, not the whole path."
				+ " The intention is to provide a check for files and directories that cannot be used on Windows. However, it can also be used "
				+ "on other platforms. You can specify the ${@} macro to refer to the default regular expressions used for this.",
			INVALIDFILENAMES + ":" + Verifier.ReservedFileNames, null, null),
		new Syntax(INCLUDERESOURCE,
			"Include resources from the file system. You can specify a directory, or file. All files are copied to the root, unless a destination directory is indicated.",
			INCLUDERESOURCE + ": lib=jar", null, null),
		new Syntax(INCLUDE_RESOURCE,
			"Include resources from the file system. You can specify a directory, or file. All files are copied to the root, unless a destination directory is indicated.",
			INCLUDE_RESOURCE + ": lib=jar", null, null),

		new Syntax(MAKE,
			"Set patterns for make plugins. These patterns are used to find a plugin that can make a resource that can not be found.",
			MAKE + ": (*).jar;type=bnd; recipe=\"bnd/$1.bnd\"", null, null,
			new Syntax("type", "Type name for plugin.", "type=bnd", "bnd", null),
			new Syntax("recipe", "Recipe for the plugin, can use back references.", "recipe=\"bnd/$1.bnd\"", "bnd",
				null)),

		new Syntax(MANIFEST, "Directly include a manifest, do not use the calculated manifest.",
			MANIFEST + "=META-INF/MANIFEST.MF", null, null),

		new Syntax(NOEXTRAHEADERS, "Do not generate housekeeping headers.", NOEXTRAHEADERS + "=true", "true,false",
			Verifier.TRUEORFALSEPATTERN),

		new Syntax(NOUSES, "Do not calculate the " + USES_DIRECTIVE + " directive on exports.", NOUSES + "=true",
			"true,false", Verifier.TRUEORFALSEPATTERN),

		new Syntax(NOEE, "Do not calculate the osgi.ee name space Execution Environment from the class file version",
			NOEE + "=true", "true,false", Verifier.TRUEORFALSEPATTERN),
		new Syntax(PEDANTIC, "Warn about things that are not really wrong but still not right.", PEDANTIC + "=true",
			"true,false", Verifier.TRUEORFALSEPATTERN),

		new Syntax(PLUGIN, "Define the plugins.",
			PLUGIN + "=aQute.lib.spring.SpringComponent,aQute.lib.deployer.FileRepo;location=${repo}", null, null),
		new Syntax(PLUGINPATH, "Define the plugins load path.",
			PLUGINPATH + "=${workspace}/cnf/cache/plugins-2.2.0.jar", null, null,
			new Syntax(PLUGINPATH_URL_ATTR, "Specify a URL to download this file from if it does not exist",
				"url=http://example.com/download/plugins-2.2.0.jar", null, null)),

		new Syntax(SERVICE_COMPONENT, "The header for Declarative Services.",
			SERVICE_COMPONENT + "=com.acme.Foo?;activate='start'", null, null),

		new Syntax(POM, "Generate a maven pom.", POM + "=true", "true,false", Verifier.TRUEORFALSEPATTERN),

		new Syntax(RELEASEREPO, "Specifies to which repo the project should be released.", RELEASEREPO + "=cnf",
			"${repos}", null),

		new Syntax(REMOVEHEADERS, "Remove all headers that match the regular expressions.",
			REMOVEHEADERS + "=FOO_.*,Proprietary", null, null),

		new Syntax(REPRODUCIBLE, "Use a fixed timestamp for all jar entries.", REPRODUCIBLE + "=true", "true,false",
			Verifier.TRUEORFALSEPATTERN),

		new Syntax(RESOURCEONLY,
			"Normally bnd warns when the JAR does not contain any classes, this option suppresses this warning.",
			RESOURCEONLY + "=true", "true,false", Verifier.TRUEORFALSEPATTERN),
		new Syntax(SOURCES, "Include sources in the jar.", SOURCES + "=true", "true,false",
			Verifier.TRUEORFALSEPATTERN),
		new Syntax(SOURCEPATH, "List of directory names that used to source sources for " + SOURCES + ".",
			SOURCEPATH + ":= src, test", null, null),
		new Syntax(TESTPATH,
			"List of bundles to be placed on the build path for local JUnit testing only. This content is never available for the bundle itself or any of its classes.",
			"-testpath=som.bundle.symbolicname;version=latest", null, null),
		new Syntax(TESTER,
			"The name of the tester. The preferred default is biz.aQute.tester, old style is biz.aQute.junit",
			"-tester=biz.aQute.tester;version=latest", null, null),
		new Syntax(SUB,
			"Build a set of bnd files that use this bnd file as a basis. The list of bnd file can be specified with wildcards.",
			SUB + "=com.acme.*.bnd", null, null),
		new Syntax(RUNPROPERTIES, "Properties that are set as system properties before the framework is started.",
			RUNPROPERTIES + "= foo=3, bar=4", null, null),
		new Syntax(RUNSYSTEMPACKAGES, "Add additional system packages to a framework run.",
			RUNSYSTEMPACKAGES + "=com.acme.foo,javax.management", null, null),
		new Syntax(RUNBUNDLES,
			"Add additional bundles, specified with their bsn and version like in " + BUILDPATH
				+ ", that are started before the project is run.",
			RUNBUNDLES + "=osgi;version=\"[4.1,4.2)\", junit.junit, com.acme.foo;version=project", null,
			Verifier.SYMBOLICNAME, path_version),
		new Syntax(RUNPATH, "Additional JARs for the VM path, can include a framework",
			RUNPATH + "=org.eclipse.osgi;version=3.5", null, null, path_version),
		new Syntax(RUNVM,
			"Additional arguments for the VM invocation. Keys that start with a - are added as options, otherwise they are treated as -D properties for the VM.",
			RUNVM + "=-Xmax=30, secondOption=secondValue", null, null),
		new Syntax(RUNPROGRAMARGS, "Additional arguments for the program invocation.",
			RUNPROGRAMARGS + "=/some/file /another/file some_argument", null, null),
		new Syntax(STANDALONE,
			"Used in bndrun files. Disconnects the bndrun file from the workspace and defines its own Capabilities repositories.",
			STANDALONE + "=index.html;name=..., ...", null, null),

		// Upto
		new Syntax(UPTO, "Limit bnd's behavior like it was up to the given version", "-upto: 2.3.1", null,
			Version.VERSION)
	};

	public final static Map<String, Syntax>	HELP					= new HashMap<>();

	final static Map<Class<?>, Pattern>		BASE_PATTERNS			= new HashMap<>();

	static {
		BASE_PATTERNS.put(Byte.class, Verifier.NUMBERPATTERN);
		BASE_PATTERNS.put(byte.class, Verifier.NUMBERPATTERN);
		BASE_PATTERNS.put(Short.class, Verifier.NUMBERPATTERN);
		BASE_PATTERNS.put(short.class, Verifier.NUMBERPATTERN);
		BASE_PATTERNS.put(Integer.class, Verifier.NUMBERPATTERN);
		BASE_PATTERNS.put(int.class, Verifier.NUMBERPATTERN);
		BASE_PATTERNS.put(Long.class, Verifier.NUMBERPATTERN);
		BASE_PATTERNS.put(long.class, Verifier.NUMBERPATTERN);
		BASE_PATTERNS.put(Float.class, Verifier.FLOATPATTERN);
		BASE_PATTERNS.put(float.class, Verifier.FLOATPATTERN);
		BASE_PATTERNS.put(Double.class, Verifier.FLOATPATTERN);
		BASE_PATTERNS.put(double.class, Verifier.FLOATPATTERN);
		BASE_PATTERNS.put(Boolean.class, Verifier.BOOLEANPATTERN);
		BASE_PATTERNS.put(boolean.class, Verifier.BOOLEANPATTERN);

		for (Syntax s : syntaxes) {
			add(s);
		}
		add(BuilderInstructions.class);
		add(LauncherInstructions.class);
	}

	private static void add(Syntax s) {
		HELP.put(s.header, s);
	}

	private static void add(Class<?> class1) {
		for (Syntax syntax : create(class1, Syntax::toInstruction, true)) {
			add(syntax);
		}
	}

	private static Syntax[] create(Class<?> class1, Function<Method, String> naming, boolean instruction) {
		List<Syntax> syntaxes = new ArrayList<>();
		for (Method m : class1.getMethods()) {

			if (Modifier.isStatic(m.getModifiers()))
				continue;

			if (m.getDeclaringClass() == Object.class)
				continue;

			String name = naming.apply(m);

			SyntaxAnnotation ann = m.getAnnotation(SyntaxAnnotation.class);
			String lead = null;
			String example = null;
			Pattern pattern = null;
			String values = null;

			if (ann != null) {
				if (!ann.lead()
					.isEmpty())
					lead = ann.lead();
				if (!ann.example()
					.isEmpty()) {
					example = ann.example();
					if (instruction) {
						example = name + ": " + ann.example();
					} else {
						example = name + "=" + ann.example();
					}
				}
				if (!ann.pattern()
					.isEmpty())
					pattern = Pattern.compile(ann.pattern());
			}

			Class<?> rtype = m.getReturnType();
			if (rtype == Optional.class) {
				Type optionalType = ((ParameterizedType) m.getGenericReturnType()).getActualTypeArguments()[0];
				assert optionalType instanceof Class : "Generic types in optional not supported";
				rtype = (Class<?>) optionalType;
			}
			if (rtype
				.isEnum()) {
				Object[] enumConstants = rtype
					.getEnumConstants();
				values = Strings.join(enumConstants);
			} else if (Boolean.class.isAssignableFrom(rtype)) {
				values = "true,false";
			}

			if (pattern == null) {
				pattern = BASE_PATTERNS.get(rtype);
			}

			if (rtype == Map.class) {
				// parameters
				ParameterizedType mapType = (ParameterizedType) m.getGenericReturnType();
				Type valueType = mapType.getActualTypeArguments()[1];

				assert valueType instanceof Class : "The type of the value of a parameters must be a class, not a generic type";

				Syntax[] clauses = create((Class<?>) valueType, Syntax::toProperty, false);
				syntaxes.add(new Syntax(name, lead, example, values, pattern, clauses));
			} else if (Iterable.class.isAssignableFrom(rtype)) {
				// list
				syntaxes.add(new Syntax(name, lead, example, values, pattern));
			} else if (rtype
				.isInterface()) {
				// properties
				Syntax[] clauses = create(rtype, Syntax::toProperty, false);
				syntaxes.add(new Syntax(name, lead, example, values, pattern, clauses));
			} else {
				// simple value
				syntaxes.add(new Syntax(name, lead, example, values, pattern));
			}

		}

		return syntaxes.toArray(new Syntax[0]);
	}

	static String toInstruction(Method m) {
		SyntaxAnnotation ann = m.getAnnotation(SyntaxAnnotation.class);
		if (ann != null && !ann.name()
			.isEmpty()) {
			return ann.name();
		}

		return "-" + preferDashes(m);
	}

	static String toProperty(Method m) {
		SyntaxAnnotation ann = m.getAnnotation(SyntaxAnnotation.class);
		if (ann != null && !ann.name()
			.isEmpty()) {
			return ann.name();
		}

		return preferDashes(m);
	}

	private static String preferDashes(Method m) {
		return m.getName()
			.replace('_', '-');
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
		if (pattern == null)
			return ".*";

		return pattern.pattern();
	}

	public Syntax[] getChildren() {
		return children;
	}

	public String getHeader() {
		return header;
	}

	public String toString() {
		return header;
	}

	public static <T> T getInstructions(Processor processor, Class<T> type) {
		return ProcessorHandler.getInstructions(processor, type);
	}

}
