package aQute.bnd.help;

import java.lang.reflect.Field;
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
import aQute.bnd.help.instructions.ResolutionInstructions;
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
		new Syntax(AUTOMATIC_MODULE_NAME,
			"The module name of an automatic module is derived from the JAR file used to include the artifact if it has the attribute "
				+ AUTOMATIC_MODULE_NAME + " in its main manifest entry.",
			AUTOMATIC_MODULE_NAME + ": com.foo.bar", null, Verifier.SYMBOLICNAME),
		new Syntax(BASELINE, "The " + BASELINE
			+ " instruction controls what bundles are enabled for baselining and optionally specify the baseline version or file.",
			BASELINE + ": com.example.*", null, null),
		new Syntax(BND_ADDXMLTOTEST,
			"The " + BND_ADDXMLTOTEST
				+ " instruction adds XML resources from the tested bundle to the output of a test report.",
			BND_ADDXMLTOTEST + ": a.xml", null, null),
		new Syntax(BUNDLE_ACTIVATIONPOLICY,
			"The " + BUNDLE_ACTIVATIONPOLICY
				+ " header specifies how the framework should activate the bundle once started.",
			BUNDLE_ACTIVATIONPOLICY + ": lazy", "lazy", Pattern.compile("lazy")),
		new Syntax(BUNDLE_ACTIVATOR,
			"The " + BUNDLE_ACTIVATOR + " header specifies the name of the class used to start and stop the bundle.",
			BUNDLE_ACTIVATOR + ": com.acme.foo.Activator", "${classes;implementing;org.osgi.framework.BundleActivator}",
			Verifier.FQNPATTERN),
		new Syntax(BUNDLE_BLUEPRINT,
			"The " + BUNDLE_BLUEPRINT
				+ " header specifies the location of the blueprint descriptor files in the bundle",
			BUNDLE_BLUEPRINT + ": /blueprint/*.xml", null, null),
		new Syntax(BUNDLE_CATEGORY,
			"The " + BUNDLE_CATEGORY + " header holds a comma-separated list of category names.",
			BUNDLE_CATEGORY + ": test", "osgi,test,game,util,eclipse,netbeans,jdk,specification", null),
		new Syntax(BUNDLE_CLASSPATH, "The " + BUNDLE_CLASSPATH
			+ " header defines a comma-separated list of JAR file path names or directories (inside the bundle) containing classes and resources. The period (’.’) specifies the root directory of the bundle’s JAR. The period is also the default.",
			BUNDLE_CLASSPATH + ": /lib/libnewgen.so, .", null, Verifier.PATHPATTERN),
		new Syntax(BUNDLE_CONTACTADDRESS,
			"The " + BUNDLE_CONTACTADDRESS + " header provides the contact address of the vendor.",
			BUNDLE_CONTACTADDRESS + ": 2400 Oswego Road, Austin, TX 74563", null, null),
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
		new Syntax(BUNDLE_COPYRIGHT,
			"The " + BUNDLE_COPYRIGHT + " header contains the copyright specification for this bundle.",
			BUNDLE_COPYRIGHT + ": OSGi (c) 2002", null, null),
		new Syntax(BUNDLE_DESCRIPTION,
			"The " + BUNDLE_DESCRIPTION + " header defines a short description of this bundle.",
			BUNDLE_DESCRIPTION + ": Ceci ce n'est pas une bundle", null, null),

		new Syntax(BUNDLE_DOCURL,
			"The " + BUNDLE_DOCURL + " header must contain a URL pointing to documentation about this bundle.",
			BUNDLE_DOCURL + ": http://www.aQute.biz/Code/Bnd", null, Verifier.URLPATTERN),

		new Syntax(BUNDLE_ICON,
			"The optional " + BUNDLE_ICON
				+ " header provides a list of (relative) URLs to icons representing this bundle in different sizes.",
			BUNDLE_ICON + ": /icons/bnd.png;size=64", "/icons/bundle.png", Verifier.URLPATTERN,
			new Syntax("size", "Icons size in pixels, e.g. 64.", "size=64", "16,32,48,64,128", Verifier.NUMBERPATTERN)),

		new Syntax(BUNDLE_LICENSE, "The " + BUNDLE_LICENSE
			+ " header provides an optional machine readable form of license information. The purpose of this header is to automate some of the license processing required by many organizations.",
			BUNDLE_LICENSE + ": http://www.opensource.org/licenses/jabberpl.php",
			"http://www.apache.org/licenses/LICENSE-2.0,<<EXTERNAL>>",
			Pattern.compile("(" + Verifier.URLPATTERN + "|<<EXTERNAL>>)"),
			new Syntax(DESCRIPTION_ATTRIBUTE, "Human readable description of the license.",
				DESCRIPTION_ATTRIBUTE + "=\"Describe the license here\"", null, Verifier.ANYPATTERN),
			new Syntax(LINK_ATTRIBUTE, "", "", null, Verifier.URLPATTERN)),
		new Syntax(BUNDLE_LOCALIZATION, "The " + BUNDLE_LOCALIZATION
			+ " header contains the location in the bundle where localization files can be found. The default value is OSGI-INF/l10n/bundle. Translations are by default therefore OSGI-INF/l10n/bundle_de.properties, OSGI-INF/l10n/bundle_nl.properties, etc.",
			BUNDLE_LOCALIZATION + ": OSGI-INF/l10n/bundle", "OSGI-INF/l10n/bundle", Verifier.URLPATTERN),
		new Syntax(BUNDLE_MANIFESTVERSION, "The " + BUNDLE_MANIFESTVERSION
			+ " header is set by bnd automatically to 2. The header defines that the bundle follows the rules of this specification.",
			"# " + BUNDLE_MANIFESTVERSION + ": 2", "2", Verifier.NUMBERPATTERN),
		new Syntax(BUNDLE_NAME, "The " + BUNDLE_NAME + " header will be derived from the " + BUNDLE_SYMBOLICNAME
			+ " header if not set. The " + BUNDLE_NAME
			+ " header defines a readable name for this bundle. This should be a short, human-readable name that can contain spaces.",
			BUNDLE_NAME + ": My Bundle", null, Verifier.ANYPATTERN),
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
		new Syntax(BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "The " + BUNDLE_REQUIREDEXECUTIONENVIRONMENT
			+ " contains a comma-separated list of execution environments that must be present on the Service Platform.",
			BUNDLE_REQUIREDEXECUTIONENVIRONMENT + ": CDC-1.0/Foundation-1.0", Processor.join(Verifier.EES, ","),
			Verifier.ANYPATTERN),

		new Syntax(BUNDLE_SYMBOLICNAME, "The " + BUNDLE_SYMBOLICNAME
			+ " header specifies a non-localizable name for this bundle. The bundle symbolic name together with a version must identify a unique bundle. The bundle symbolic name should be based on the reverse domain name convention.",
			BUNDLE_SYMBOLICNAME + ": com.acme.foo.daffy;singleton:=true", "${p}", Verifier.SYMBOLICNAME,
			new Syntax(SINGLETON_DIRECTIVE,
				"Indicates that the bundle can only have a single version resolved. A value of true indicates that the bundle is a singleton bundle. The default value is false. The Framework must resolve at most one bundle when multiple versions of a singleton bundle with the same symbolic name are installed. Singleton bundles do not affect the resolution of non-singleton bundles with the same symbolic name.",
				SINGLETON_DIRECTIVE + "=false", "true,false", Verifier.TRUEORFALSEPATTERN),
			new Syntax(FRAGMENT_ATTACHMENT_DIRECTIVE,
				"Defines how fragments are allowed to be attached, see the fragments in Fragment Bundles on page 73. The following values are valid for this directive:",
				"", "always|never|resolve-time", Pattern.compile("always|never|resolve-time")),
			new Syntax(BLUEPRINT_WAIT_FOR_DEPENDENCIES_ATTRIBUTE, "", "", "true,false", Verifier.TRUEORFALSEPATTERN),
			new Syntax(BLUEPRINT_TIMEOUT_ATTRIBUTE, "", "", "30000,60000,300000", Verifier.NUMBERPATTERN)),

		new Syntax(BUNDLE_UPDATELOCATION, "The " + BUNDLE_UPDATELOCATION
			+ " header specifies a URL where an update for this bundle should come from. If the bundle is updated, this location should be used, if present, to retrieve the updated JAR file.",
			BUNDLE_UPDATELOCATION + ": http://www.acme.com/Firewall/bundle.jar", null, Verifier.URLPATTERN),

		new Syntax(BUNDLE_VENDOR,
			"The " + BUNDLE_VENDOR + " header contains a human-readable description of the bundle vendor.",
			BUNDLE_VENDOR + ": OSGi Alliance", null, null),

		new Syntax(BUNDLE_VERSION, "The " + BUNDLE_VERSION + " header specifies the version of this bundle.",
			BUNDLE_VERSION + ": 1.23.4.build200903221000", null, Verifier.VERSION),

		new Syntax(CLASSPATH, "The " + CLASSPATH + " instruction adds class path entries to a bnd file’s processing.",
			BASELINE + ": jar/foo.jar, jar/bar.jar", null, null),

		new Syntax(COMPRESSION, "Set the compression for writing JARs. Default is deflate", COMPRESSION + "=store",
			"deflate,store", Pattern.compile("deflate|store")),

		new Syntax(DYNAMICIMPORT_PACKAGE, "The " + DYNAMICIMPORT_PACKAGE
			+ " header contains a comma-separated list of package names that should be dynamically imported when needed.",
			DYNAMICIMPORT_PACKAGE + ": com.acme.plugin.*", "", Verifier.WILDCARDNAMEPATTERN, version,
			bundle_symbolic_name, bundle_version),

		new Syntax(EXPORT_PACKAGE, "The " + EXPORT_PACKAGE + " header contains a declaration of exported packages.",
			EXPORT_PACKAGE + ": org.osgi.util.tracker;version=1.3", "${packages}", null,
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
		new Syntax(TESTCASES, "The " + TESTCASES + " instruction is used to specify the list of test cases to run.",
			TESTCASES + ": com.foo.bar.MyTest", null, Verifier.FQNPATTERN),
		new Syntax(CONDITIONAL_PACKAGE, "The " + CONDITIONAL_PACKAGE
			+ " works as private package but will only include the packages when they are imported. When this header is used, bnd will recursively add packages that match the patterns until there are no more additions.",
			CONDITIONAL_PACKAGE + ": com.*", "${packages}", null),
		new Syntax(CONDITIONALPACKAGE, "The " + CONDITIONALPACKAGE
			+ " works as private package but will only include the packages when they are imported. When this header is used, bnd will recursively add packages that match the patterns until there are no more additions.",
			CONDITIONALPACKAGE + ": com.*", "${packages}", null),
		new Syntax(META_PERSISTENCE,
			"A bundle is regarded as a persistence bundle if it contains the header " + META_PERSISTENCE
				+ " in it's Manifest.",
			META_PERSISTENCE + ": persistence/myPu.xml", null, null),
		new Syntax(PRIVATEPACKAGE, "The " + PRIVATEPACKAGE
			+ " header contains a declaration of packages to be included in the resulting bundle, the only difference is, is that these packages will not be exported.",
			PRIVATEPACKAGE + ": com.*", "${packages}", null),
		new Syntax(PRIVATE_PACKAGE, "The " + PRIVATE_PACKAGE
			+ " header contains a declaration of packages to be included in the resulting bundle, the only difference is, is that these packages will not be exported.",
			PRIVATE_PACKAGE + ": com.*", "${packages}", null),
		new Syntax(IGNORE_PACKAGE,
			"The " + IGNORE_PACKAGE + " is used to ignore a package from being packaged inside the bundle.",
			IGNORE_PACKAGE + ": com.foo.bar", "${packages}", null),
		new Syntax(EXPORT_SERVICE, "Deprecated.", EXPORT_SERVICE + ": org.osgi.service.log.LogService",
			"${classes;implementing;*}", null),
		new Syntax(FRAGMENT_HOST, "The " + FRAGMENT_HOST + " header defines the host bundle for this fragment.",
			FRAGMENT_HOST + ": org.eclipse.swt; bundle-version=\"[3.0.0,4.0.0)\"", null, null,
			new Syntax(EXTENSION_DIRECTIVE,
				"Indicates this extension is a system or boot class path extension. It is only applicable when the "
					+ Constants.FRAGMENT_HOST + " is the System Bundle.",
				EXTENSION_DIRECTIVE + "=framework", "framework,bootclasspath",
				Pattern.compile("framework|bootclasspath")),
			bundle_version),
		new Syntax(IMPORT_PACKAGE, "The " + IMPORT_PACKAGE
			+ " header is normally calculated by bnd, however, you can decorate packages or skip packages. The header declares the imported packages for this bundle.",
			IMPORT_PACKAGE + ": !com.exotic.*, com.acme.foo;vendor=ACME, *", "${exported_packages}",
			Verifier.WILDCARDNAMEPATTERN,
			new Syntax(REMOVE_ATTRIBUTE_DIRECTIVE, "Remove the given attributes from matching imported packages.",
				REMOVE_ATTRIBUTE_DIRECTIVE + "=foo.*", null, Verifier.WILDCARDNAMEPATTERN),
			new Syntax(RESOLUTION_DIRECTIVE,
				"Indicates that the packages must be resolved if the value is mandatory, which is the default. If mandatory packages cannot be resolved, then the bundle must fail to resolve. A value of optional indicates that the packages are optional.",
				RESOLUTION_DIRECTIVE + "=optional", "mandatory,optional", Pattern.compile("mandatory|optional")

			), version, bundle_symbolic_name, bundle_version),

		new Syntax(REQUIRE_BUNDLE,
			"The " + REQUIRE_BUNDLE + " header specifies the required exports from another bundle.",
			REQUIRE_BUNDLE + ": com.acme.chess", null, Verifier.WILDCARDNAMEPATTERN,

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
		new Syntax(PROVIDE_CAPABILITY,
			"The " + PROVIDE_CAPABILITY
				+ " header specifies that a bundle provides a set of Capabilities, other bundles can use "
				+ REQUIRE_CAPABILITY + " to match this capability.",
			PROVIDE_CAPABILITY + ": com.acme.dictionary; from:String=nl; to=de; version:Version=3.4", null,
			Verifier.WILDCARDNAMEPATTERN,

			new Syntax(EFFECTIVE_DIRECTIVE,
				"(resolve) Specifies the time a capabiltity is available, either resolve (default) or another name. The OSGi framework resolver only considers Capabilities without an effective directive or effective:=resolve. Capabilties with other values for the effective directive can be considered by an external agent.",
				EFFECTIVE_DIRECTIVE + "=resolve", "resolve or another word", null),

			new Syntax(USES_DIRECTIVE,
				"The uses directive lists package names that are used by this Capability. This information is intended to be used for uses constraints.",
				USES_DIRECTIVE + "='foo,bar,baz'", null, null)),
		new Syntax(REQUIRE_CAPABILITY,
			"The " + REQUIRE_CAPABILITY
				+ " header specifies that a bundle requires other bundles to provide a Capability, see "
				+ PROVIDE_CAPABILITY,
			REQUIRE_CAPABILITY + ": com.microsoft; filter:='(&(api=win32)(version=7))'", null,
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
		new Syntax(AUGMENT, "The " + AUGMENT
			+ " instruction can be used to augment resources in the repositories. Augmenting is adding additional capabilities and requirements",
			AUGMENT
				+ ": com.example.prime; capability:='osgi.extender; osgi.extender=osgi.component; version:Version=1.2'",
			null, Verifier.WILDCARDNAMEPATTERN,

			new Syntax(AUGMENT_CAPABILITY_DIRECTIVE,
				"This directive specifies a Provide-Capability instruction, this will therefore likely have to be quoted. Any number of clauses can be specified.",
				AUGMENT_CAPABILITY_DIRECTIVE + "=osgi.extender; osgi.extender=osgi.component; version:Version=1.2",
				null, null),

			new Syntax(AUGMENT_REQUIREMENT_DIRECTIVE, "The directive specifies a Require-Capability instruction",
				AUGMENT_REQUIREMENT_DIRECTIVE + "=osgi.identity;filter:=\"(osgi.identity=a.b.c)\"", null, null),

			new Syntax(AUGMENT_RANGE_ATTRIBUTE,
				" A version range. If a single version is given it will be used as [<version>,∞). The version range can be prefixed with an ‘@’ for a consumer range (to the next major) or a provider range (to the next minor) when the ‘@’ is a suffix of the version. The range can restrict the augmentation to a limited set of bundles.",
				null, null, null)),
		new Syntax(BUILDPATH,
			"Provides the class path for building the jar. The entries are references to the repository.",
			BUILDPATH + "=osgi;version=4.1", "${repo;bsns}", Verifier.SYMBOLICNAME, path_version),
		new Syntax(BUILDREPO, "After building a JAR, release the JAR to the given repositories.", BUILDREPO + "=Local",
			null, null),
		new Syntax(BUILDERIGNORE,
			"List of project-relative directories to be ignored by the builder. This is processed by the Bndtools builder in Eclipse and the Bnd Gradle plugin for workspace model builds.",
			BUILDERIGNORE + "=${if;${driver;gradle};bin,bin_test,generated;build}", null, null),
		new Syntax(BUMPPOLICY, "Sets the version bump policy. This is a parameter to the ${version} macro.",
			BUMPPOLICY + "==+0", "==+,=+0,+00", Pattern.compile("[=+-0][=+-0][=+-0]")),
		new Syntax(BUNDLEANNOTATIONS, "Selects the classes that need processing for standard OSGi Bundle annotations.",
			BUNDLEANNOTATIONS + ": com.foo.bar.MyClazz", null, Verifier.FQNPATTERN),

		new Syntax(BASELINEREPO, "Define the repository to calculate baselining against.", BASELINEREPO + "=Baseline",
			null, null),
		new Syntax(BNDDRIVER, "Sets the driver property.", BNDDRIVER + ":" + BNDDRIVER_ECLIPSE,
			"(" + BNDDRIVER_ANT + " | " + BNDDRIVER_GRADLE + "|" + BNDDRIVER_ECLIPSE + BNDDRIVER_BND
				+ BNDDRIVER_GRADLE_NATIVE + BNDDRIVER_INTELLIJ + BNDDRIVER_MAVEN + BNDDRIVER_OSMORC + BNDDRIVER_SBT
				+ ")",
			null),
		new Syntax(CHECK, "Enable additional checking.", CHECK + "=EXPORTS", "(ALL|EXPORTS|IMPORTS)", null),
		new Syntax(CONTRACT, "Establishes a link to a contract and handles the low level details.",
			CONTRACT + "!Servlet,*", null, Verifier.WILDCARDNAMEPATTERN),
		new Syntax(CONSUMER_POLICY,
			"Specify the default version bump policy for a consumer when a binary incompatible change is detected.",
			CONSUMER_POLICY + "${range;[==,+)}", null, null),
		new Syntax(PROVIDER_POLICY,
			"Specify the default version bump policy for a provider when a binary incompatible change is detected.",
			PROVIDER_POLICY + "${range;[==,=+)}", null, null),

		new Syntax(CDIANNOTATIONS, "The " + CDIANNOTATIONS
			+ " instruction tells bnd which bundle classes, if any, to search for OSGI CDI Integration (or plain CDI) annotation.",
			CDIANNOTATIONS + ": *;discover=all", null, Verifier.WILDCARDNAMEPATTERN,

			new Syntax("discover", "Bean Discovery Mode.", "discover=all", "(all|annotated|annotated_by_bean|none)",
				null),

			new Syntax("noservicecapabilities", "indicates that no service capabilities will be added for matches.",
				"noservicecapabilities=true", "true, false", Pattern.compile("true|false"))),

		new Syntax(CONNECTION_SETTINGS, "Setting up the communications for bnd.",
			CONNECTION_SETTINGS + "= ~/.bnd/connection-settings.xml", null, null),

		new Syntax(CONDUIT, "Allows a bnd file to point to files which will be returned when the bnd file is build.",
			CONDUIT + "= jar/osgi.jar", null, null),

		new Syntax(DEPENDSON,
			"List of project names that this project directly depends on. These projects are always build ahead of this project.",
			DEPENDSON + "=org.acme.cm", "${projects}", null),

		new Syntax(DEPLOYREPO, "Specifies to which repo the project should be deployed.", DEPLOYREPO + "=cnf",
			"${repos}", null),

		new Syntax(DIFFIGNORE, "Manifest header names and resource paths to ignore during baseline comparison.",
			DIFFIGNORE + "=Bundle-Version", null, null),

		new Syntax(DIFFPACKAGES, "The names of exported packages to baseline.", DIFFPACKAGES + "=!*.internal.*, *",
			null, null),

		new Syntax(DIGESTS, "Set the digest algorithms to use.", DIGESTS + ": SHA-1 ", null, null),

		new Syntax(DISTRO, "Resolve against pre-defined system capabilities.", DISTRO + ": karaf-4.1.jar;version=file",
			null, null),

		new Syntax(DONOTCOPY,
			"Regular expression for names of files and directories that should not be copied when discovered.",
			DONOTCOPY + "=(CVS|\\.svn)", null, null),

		new Syntax(DSANNOTATIONS, "The " + DSANNOTATIONS
			+ " instruction tells bnd which bundle classes, if any, to search for Declarative Services (DS) annotations. bnd will then process those classes into DS XML descriptors.",
			DSANNOTATIONS + ": *", null, Verifier.FQNPATTERN),

		new Syntax(DEFAULT_PROP_SRC_DIR,
			"The " + DEFAULT_PROP_SRC_DIR + " instructs bnd to look for sources in the specified source directories",
			DEFAULT_PROP_SRC_DIR + ": src/main/java, src/main/resources", null, null),

		new Syntax(DEFAULT_PROP_BIN_DIR,
			"The " + DEFAULT_PROP_BIN_DIR + " is used to specify the directory to generate the output binaries.",
			DEFAULT_PROP_BIN_DIR + ": target/classes", null, null),

		new Syntax(DEFAULT_PROP_TESTSRC_DIR,
			"The " + DEFAULT_PROP_TESTSRC_DIR + " instructs bnd to look for test sources in the specified directories",
			DEFAULT_PROP_TESTSRC_DIR + ": src/test/java", null, null),

		new Syntax(DEFAULT_PROP_TESTBIN_DIR,
			"The " + DEFAULT_PROP_TESTBIN_DIR
				+ " is used to specify the directory to generate the output binaries for test sources.",
			DEFAULT_PROP_TESTBIN_DIR + ": target/test-classes", null, null),

		new Syntax(DEFAULT_PROP_TARGET_DIR,
			"The " + DEFAULT_PROP_TARGET_DIR + " is used to specify the directory to generate output JAR.",
			DEFAULT_PROP_TARGET_DIR + ": target", null, null),

		new Syntax(DSANNOTATIONS_OPTIONS,
			"The " + DSANNOTATIONS_OPTIONS
				+ " instruction configures how DS component annotations are processed and what metadata is generated.",
			DSANNOTATIONS_OPTIONS + ": version;minimum=1.2.0",
			"(inherit|felixExtensions|extender|nocapabilities|norequirements|version)", null),

		new Syntax(EXPORT, "The " + EXPORT + " instruction turns a bndrun file into its deployable format.",
			EXPORT + ": launcher.jar", "FILE ( ';' PARAMETER )* ( ',' FILE ( ';' PARAMETER )* )*", null),

		new Syntax(METATYPE_ANNOTATIONS, "The " + METATYPE_ANNOTATIONS
			+ " instruction tells bnd which bundle classes, if any, to search for Metatype annotations. bnd will then process those classes into Metatype XML descriptors.",
			METATYPE_ANNOTATIONS + ": *", null, Verifier.FQNPATTERN),

		new Syntax(METATYPE_ANNOTATIONS_OPTIONS,
			"The " + DSANNOTATIONS_OPTIONS
				+ " instruction configures how Metatype annotations are processed and what metadata is generated.",
			METATYPE_ANNOTATIONS_OPTIONS + ": version;minimum=1.2.0",
			"(inherit|felixExtensions|extender|nocapabilities|norequirements|version)", null),

		new Syntax(EEPROFILE, "Provides control over what Java 8 profile to use.", EEPROFILE + ": name=\"a,b,c\"",
			"name=\"a,b,c\", auto", null),

		new Syntax(EXTENSION,
			"A plugin that is loaded to its url, downloaded and then provides a header used instantiate the plugin.",
			null, null, null),

		new Syntax(EXECUTABLE,
			"Process an executable jar to strip optional directories of the contained bundles and/or change their compression.",
			EXECUTABLE + ": rejar=STORE, strip='OSGI-OPT,*.map'",
			"( rejar= STORE | DEFLATE ) ( ',' strip= matcher ( ',' matcher )* )", null),

		new Syntax(EXPORT_CONTENTS,
			"Build the JAR in the normal way but use this header for the " + EXPORT_PACKAGE
				+ " header manifest generation, same format.",
			EXPORT_CONTENTS + "=!*impl*,*;version=3.0", null, null),

		new Syntax(EXPORT_APIGUARDIAN,
			"Enable the APIGuardian plugin that searches for @API annotations for calculating the " + EXPORT_PACKAGE
				+ " header manifest generation, same format.",
			EXPORT_APIGUARDIAN + "=!*impl*,*;version=3.0", null, null),

		new Syntax(EXPORTTYPE, "This specifies the type of the exported content",
			EXPORTTYPE + "=bnd.executablejar;foo=bnd, bnd.runbundles;bar=bnd", null, null),

		new Syntax(FAIL_OK, "Return with an ok status (0) even if the build generates errors.", FAIL_OK + "=true",
			"true,false", Verifier.TRUEORFALSEPATTERN),
		new Syntax(FIXUPMESSAGES,
			"Rearrange and/or replace errors and warnings. Errors that should be ignore or be warnings (and vice versa for warnings) can be moved or rewritten by specifying a globbing pattern for the message.",
			FIXUPMESSAGES + "='Version mismatch';replace:='************* ${@}';restrict:=error", null, null),
		new Syntax(GESTALT, "provides access to the gestalt properties that describe the environment",
			GESTALT + "=interactive",
			"(" + GESTALT_INTERACTIVE + "|" + GESTALT_BATCH + "|" + GESTALT_CI + "|" + GESTALT_OFFLINE + "|"
				+ GESTALT_SHELL + ")",
			null),
		new Syntax(GROUPID, "Specifies the Maven Group ID to be used for bundles", GROUPID + "=com.foo.bar", null,
			null),
		new Syntax(INCLUDE,
			"Include files. If an entry starts with '-', it does not have to exist. If it starts with '~', it must not overwrite any existing properties.",
			INCLUDE + ": -${java.user}/.bnd", null, null),
		new Syntax(INVALIDFILENAMES,
			"Specify a regular expressions to match against file or directory names. This is the segment, not the whole path."
				+ " The intention is to provide a check for files and directories that cannot be used on Windows. However, it can also be used "
				+ "on other platforms. You can specify the ${@} macro to refer to the default regular expressions used for this.",
			INVALIDFILENAMES + ":" + Verifier.ReservedFileNames, null, null),
		new Syntax(INCLUDEPACKAGE, "Include a number of packages from the class path.",
			INCLUDEPACKAGE + ": !com.foo.bar, com.foo.* ", null, Verifier.WILDCARDNAMEPATTERN),
		new Syntax(INCLUDERESOURCE,
			"Include resources from the file system. You can specify a directory, or file. All files are copied to the root, unless a destination directory is indicated.",
			INCLUDERESOURCE + ": lib=jar", null, null),
		new Syntax(INCLUDE_RESOURCE,
			"Include resources from the file system. You can specify a directory, or file. All files are copied to the root, unless a destination directory is indicated.",
			INCLUDE_RESOURCE + ": lib=jar", null, null),
		new Syntax(INIT, "Executes macros while initializing the project for building", INIT + ": ${my_macro} ", null,
			null),
		new Syntax(JAVAAGENT, "Specify if classpath jars with Premain-Class headers are to be used as java agents.",
			JAVAAGENT + ": true", "true,false", Verifier.TRUEORFALSEPATTERN),
		new Syntax(JAVAC, "Java Compiler Specific Settings.", null, null, null),
		new Syntax(JAVAC_ENCODING, "Sets the Java Compiler Encoding Type.", JAVAC_ENCODING + ": UTF-8", null, null),
		new Syntax(JAVAC_SOURCE, "Sets the Java source compatibility version.", JAVAC_SOURCE + ": 1.8", null, null),
		new Syntax(JAVAC_PROFILE, "When using compact profiles, this option specifies the profile name when compiling.",
			JAVAC_PROFILE + ": compact1", null, null),
		new Syntax(JAVAC_TARGET, "Sets the Java target compatibility version.", JAVAC_TARGET + ": 1.8", null, null),

		new Syntax(MAKE,
			"Set patterns for make plugins. These patterns are used to find a plugin that can make a resource that can not be found.",
			MAKE + ": (*).jar;type=bnd; recipe=\"bnd/$1.bnd\"", null, null,
			new Syntax("type", "Type name for plugin.", "type=bnd", "bnd", null),
			new Syntax("recipe", "Recipe for the plugin, can use back references.", "recipe=\"bnd/$1.bnd\"", "bnd",
				null)),

		new Syntax(MAVEN_RELEASE, "Set the Maven release options for the Maven Bnd Repository.",
			MAVEN_RELEASE + ": local", "(local|remote)", null),

		new Syntax(MAVEN_SCOPE, "Set the default Maven scope for dependencies in the generated POM.",
			MAVEN_SCOPE + ": compile", "(compile|provided)", null),

		new Syntax(MANIFEST, "Directly include a manifest, do not use the calculated manifest.",
			MANIFEST + "=META-INF/MANIFEST.MF", null, null),

		new Syntax(NOBUILDINCACHE, "Do not use a build in cache for the launcher and JUnit.", NOBUILDINCACHE + "=true",
			"true,false", Verifier.TRUEORFALSEPATTERN),

		new Syntax(NOBUNDLES, "Do not create a target JAR for the project", NOBUNDLES + "=true", "true,false",
			Verifier.TRUEORFALSEPATTERN),

		new Syntax(NODEFAULTVERSION, "Do not add a default version to exported packages when no version is present.",
			NODEFAULTVERSION + "=true", "true,false", Verifier.TRUEORFALSEPATTERN),

		new Syntax(NOEXTRAHEADERS, "Do not generate housekeeping headers.", NOEXTRAHEADERS + "=true", "true,false",
			Verifier.TRUEORFALSEPATTERN),

		new Syntax(NOJUNIT, "Indicates that this project does not have JUnit tests", NOJUNIT + "=true", "true,false",
			Verifier.TRUEORFALSEPATTERN),

		new Syntax(NOJUNITOSGI, "Indicates that this project does not have JUnit OSGi tests", NOJUNITOSGI + "=true",
			"true,false", Verifier.TRUEORFALSEPATTERN),

		new Syntax(NOMANIFEST, "Do not safe the manifest in the JAR.", NOMANIFEST + "=true", "true,false",
			Verifier.TRUEORFALSEPATTERN),

		new Syntax(NOUSES,
			"Do not calculate the " + USES_DIRECTIVE + " directive on package exports or on capabilities.",
			NOUSES + "=true",
			"true,false", Verifier.TRUEORFALSEPATTERN),
		new Syntax(NOCLASSFORNAME,
			"Do not calculate " + IMPORT_PACKAGE
				+ " references for 'Class.forName(\"some.Class\")' usage found in method bodies during class processing.",
			NOCLASSFORNAME + "=true", "true,false", Verifier.TRUEORFALSEPATTERN),

		new Syntax(NOEE, "Do not calculate the osgi.ee name space Execution Environment from the class file version.",
			NOEE + "=true", "true,false", Verifier.TRUEORFALSEPATTERN),
		new Syntax(NAMESECTION,
			"Create a name section (second part of manifest) with optional property expansion and addition of custom attributes.",
			NAMESECTION + "=*;baz=true, abc/def/bar/X.class=3", null, null),
		new Syntax(OUTPUT, "Specify the output directory or file.", OUTPUT + "=my_directory", null, null),
		new Syntax(OUTPUTMASK,
			"If set, is used a template to calculate the output file. It can use any macro but the ${@bsn} and ${@version} macros refer to the current JAR being saved. The default is bsn + \".jar\".",
			OUTPUTMASK + "=my_file.zip", null, null),
		new Syntax(PACKAGEINFOTYPE, "Sets the different types of package info.", PACKAGEINFOTYPE + "=osgi", null, null),
		new Syntax(PEDANTIC, "Warn about things that are not really wrong but still not right.", PEDANTIC + "=true",
			"true,false", Verifier.TRUEORFALSEPATTERN),

		new Syntax(PLUGIN, "Define the plugins.",
			PLUGIN + "=aQute.lib.spring.SpringComponent,aQute.lib.deployer.FileRepo;location=${repo}", null, null),
		new Syntax(PLUGINPATH, "Define the plugins load path.",
			PLUGINPATH + "=${workspace}/cnf/cache/plugins-2.2.0.jar", null, null,
			new Syntax(PLUGINPATH_URL_ATTR, "Specify a URL to download this file from if it does not exist",
				"url=http://example.com/download/plugins-2.2.0.jar", null, null)),
		new Syntax(PREPROCESSMATCHERS, "Specify which files can be preprocessed.",
			PREPROCESSMATCHERS + "=!OSGI-INF/*,* ", null, null),

		new Syntax(SERVICE_COMPONENT, "The header for Declarative Services.",
			SERVICE_COMPONENT + "=com.acme.Foo?;activate='start'", null, null),

		new Syntax(POM, "Generate a maven pom.", POM + "=true", "true,false", Verifier.TRUEORFALSEPATTERN),

		new Syntax(RELEASEREPO, "Specifies to which repo the project should be released.", RELEASEREPO + "=cnf",
			"${repos}", null),

		new Syntax(REMOVEHEADERS, "Remove all headers that match the regular expressions.",
			REMOVEHEADERS + "=FOO_.*,Proprietary", null, null),

		new Syntax(REPRODUCIBLE, "Use a fixed timestamp for all jar entries.", REPRODUCIBLE + "=true", "true,false",
			Verifier.TRUEORFALSEPATTERN),

		new Syntax("-resolve.effective",
			"Each requirement and capability has an effective or is effective=resolve. An effective of resolve is always processed by the resolver.",
			"-resolve.effective=resolve,active", "qname (',' qname )", null),

		new Syntax("-resolve.preferences", "Override the default order and selection of repositories.",
			"-resolve.preferences=com.example.bundle.most.priority", "${packages}", null),

		new Syntax(RUNTIMEOUT, "Specifies the test execution timeout.", RUNTIMEOUT + "=10000", null, null),
		new Syntax(REQUIRE_BND, "Require a specific version of bnd.", REQUIRE_BND + "=\"(version>=4.1)\"",
			"(FILTER ( ',' FILTER )* )?", null),

		new Syntax(RESOURCEONLY,
			"Normally bnd warns when the JAR does not contain any classes, this option suppresses this warning.",
			RESOURCEONLY + "=true", "true,false", Verifier.TRUEORFALSEPATTERN),
		new Syntax(SAVEMANIFEST, "Write out the manifest to a separate file after it has been calculated.",
			SAVEMANIFEST + "=file.txt", null, null),
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
		new Syntax(TESTER_PLUGIN,
			"It points to a class that must extend the aQute.bnd.build.ProjectTester class. This class is loaded in the bnd environment and not in the target environment. This ProjectTester plugin then gets a chance to configure the launcher as it sees fit. It can get properties from the project and set these in the Project Launcher so they can be picked up in the target environment.",
			TESTER_PLUGIN + "= a.b.c.MyTester", null, Verifier.FQNPATTERN),
		new Syntax(SUB,
			"Build a set of bnd files that use this bnd file as a basis. The list of bnd file can be specified with wildcards.",
			SUB + "=com.acme.*.bnd", null, null),
		new Syntax(REPORTNEWER, "Report any entries that were added to the build since the last JAR was made.",
			REPORTNEWER + "=true", "true,false", Verifier.TRUEORFALSEPATTERN),
		new Syntax(RUNPROPERTIES, "Properties that are set as system properties before the framework is started.",
			RUNPROPERTIES + "= foo=3, bar=4", null, null),
		new Syntax(RUNREMOTE, "It provides remote debugging support for bnd projects.",
			RUNREMOTE + "= local; shell=4003; jdb=1044; host=localhost ", null, null),
		new Syntax(RUNSTORAGE, "Define the directory to use for the framework's work area.", RUNSTORAGE + "= foo", null,
			null),
		new Syntax(RUNSYSTEMPACKAGES, "Add additional system packages to a framework run.",
			RUNSYSTEMPACKAGES + "=com.acme.foo,javax.management", null, null),
		new Syntax(RUNSYSTEMCAPABILITIES, "Define extra capabilities for the remote VM.",
			RUNSYSTEMCAPABILITIES + "=some.namespace; some.namespace=foo", null, null),
		new Syntax(RUNPROVIDEDCAPABILITIES, "Extra capabilities for a distro resolve.",
			RUNPROVIDEDCAPABILITIES + "=some.namespace; some.namespace=foo", null, null),
		new Syntax(RUNTIMEOUT, "Define extra capabilities for the remote VM.",
			RUNSYSTEMCAPABILITIES + "=some.namespace; some.namespace=foo", null, null),
		new Syntax(RUNBUILDS,
			"Defines if this should add the bundles build by this project to the " + RUNBUNDLES
				+ ". For a bndrun file this is default false, for a bnd file this is default true.",
			RUNBUILDS + "=true", "true,false", Verifier.TRUEORFALSEPATTERN),
		new Syntax(RUNBUNDLES,
			"Add additional bundles, specified with their bsn and version like in " + BUILDPATH
				+ ", that are started before the project is run.",
			RUNBUNDLES + "=osgi;version=\"[4.1,4.2)\", junit.junit, com.acme.foo;version=project", null,
			Verifier.SYMBOLICNAME, path_version),
		new Syntax(RUNFRAMEWORK,
			"Sets the type of framework to run. If 'none', an internal dummy framework is used. Otherwise the Java META-INF/services model is used for the FrameworkFactory interface name.",
			RUNFW + ": none", "(none | services | ANY)", null),
		new Syntax(RUNENV, "Specify runtime properties for the framework.",
			RUNENV + ": org.osgi.service.http.port=9999, org.osgi.framework.bootdelegation=\"sun.*,com.sun.*,\"", null,
			null),
		new Syntax(RUNFW, "The " + RUNFW + " instruction sets the framework to use.",
			RUNFW + ": org.eclipse.osgi; version=3.10", null, null),
		new Syntax(RUNJDB,
			"Specify a JDB port on invocation when launched outside a debugger so the debugger can attach later.",
			RUNJDB + ": 10001", null, null),
		new Syntax(RUNKEEP, "Decides to keep the framework storage directory between launching.", RUNKEEP + ": true",
			"true,false", Verifier.TRUEORFALSEPATTERN),
		new Syntax(RUNPATH, "Additional JARs for the VM path, can include a framework",
			RUNPATH + "=org.eclipse.osgi;version=3.5", null, null, path_version),
		new Syntax(RUNNOREFERENCES,
			"Do not use the reference url for installing a bundle in the installer. This is the default for windows because it is quite obstinate about open files, on other platforms the more efficient reference urls are used.",
			RUNNOREFERENCES + ": true", "true,false", Verifier.TRUEORFALSEPATTERN),
		new Syntax(RUNTRACE, "Trace the launched process in detail.", RUNTRACE + ": true", "true,false",
			Verifier.TRUEORFALSEPATTERN),
		new Syntax(REMOTEWORKSPACE,
			"This setting enables the workspace to be available over a remote procedure call interface.",
			REMOTEWORKSPACE + ": true", "true,false", Verifier.TRUEORFALSEPATTERN),
		new Syntax(RUNVM,
			"Additional arguments for the VM invocation. Keys that start with a - are added as options, otherwise they are treated as -D properties for the VM.",
			RUNVM + "=-Xmax=30, secondOption=secondValue", null, null),
		new Syntax(RUNPROGRAMARGS, "Additional arguments for the program invocation.",
			RUNPROGRAMARGS + "=/some/file /another/file some_argument", null, null),
		new Syntax(RUNREPOS, "The " + RUNREPOS + " instruction is used to restrict or order the available repositories",
			RUNREPOS + "=Maven Central, Main, Distro, ...", null, null),
		new Syntax(RUNREQUIRES, "Comma seperated list of root requirements for a resolve operation.",
			RUNREQUIRES + "=osgi.identity;filter:='(osgi.identity=<bsn>)', ...", null, null),
		new Syntax(RUNBLACKLIST,
			"A set of requirements that is then removed from any result from the repositories, effectively making it impossible to use.",
			RUNBLACKLIST + "=osgi.identity;filter:='(osgi.identity=<bsn>)', ...", null, null),
		new Syntax(RUNEE,
			"Adds the capabilities of an execution environment to the system capabilities for a resolve operation.",
			RUNEE + "=JavaSE-1.8", null, null),
		new Syntax(SIGN, "Sign the Jar File", SIGN + "=alias",
			"<alias> [ ';' 'password:=' <password> ] [ ';'* 'keystore:=' <keystore> ] [ ';' 'sign-password:=' <pw> ] ( ',' ... )*",
			null),
		new Syntax(SNAPSHOT,
			"When the bundle version’s qualifier equals 'SNAPSHOT' or ends with '-SNAPSHOT', the STRING value of the -snapshot instruction is substituted for 'SNAPSHOT'.",
			SNAPSHOT + "=${tstamp}", null, null),
		new Syntax(STANDALONE,
			"Used in bndrun files. Disconnects the bndrun file from the workspace and defines its own Capabilities repositories.",
			STANDALONE + "=index.html;name=..., ...", null, null),
		new Syntax(STRICT, "If set to true, then extra verification is done.", STRICT + "=true", "true,false",
			Verifier.TRUEORFALSEPATTERN),
		new Syntax(SYSTEMPROPERTIES, "Properties that are set as system properties.",
			SYSTEMPROPERTIES + "= foo=3, bar=4", null, null),
		new Syntax(TESTCONTINUOUS,
			"Do not exit after running the test suites but keep watching the bundles and rerun the test cases if the bundle is updated.",
			TESTCONTINUOUS + "=true", "true,false", Verifier.TRUEORFALSEPATTERN),
		new Syntax(TESTSOURCES,
			"Specification to find JUnit test cases by traversing the test src directory and looking for java classes.",
			TESTSOURCES + "=*.java", "REGEX ( ',' REGEX )*", null),
		new Syntax(TESTPACKAGES,
			"It automatically adds the test packages if and only if " + UNDERTEST + " has been set to true",
			TESTPACKAGES + "=test;presence:=optional", null, null),
		new Syntax(TESTUNRESOLVED,
			"Will execute a JUnit testcase ahead of any other test case that will abort if there are any unresolved bundles.",
			TESTUNRESOLVED + "=true", "true,false", Verifier.TRUEORFALSEPATTERN),

		new Syntax(UNDERTEST,
			"Will be set by the project when it builds a JAR in test mode, intended to be used by plugins.",
			UNDERTEST + "=true", "true,false", Verifier.TRUEORFALSEPATTERN),

		new Syntax(UPTO, "Limit bnd's behavior like it was up to the given version", "-upto: 2.3.1", null,
			Version.VERSION),

		new Syntax(WAB, "Create a Web Archive Bundle (WAB) or a WAR.", WAB + "=static-pages/", null, null),
		new Syntax(WABLIB, "Specify the libraries that must be included in a Web Archive Bundle (WAB) or WAR.",
			WABLIB + "=lib/a.jar, lib/b.jar", null, null),
		new Syntax(WORKINGSET, "Groups the workspace into different working sets.",
			WORKINGSET + "=Implementations, Drivers", null, null),
		new Syntax("-x-overwritestrategy",
			"On windows we sometimes cannot delete a file because someone holds a lock in our or another process. So if we set the -overwritestrategy flag we use an avoiding strategy.",
			"-x-overwritestrategy=gc", "(classic|delay|gc|windows-only-disposable-names|disposable-names)", null)
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
		add(ResolutionInstructions.class);
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
			if (rtype.isEnum()) {
				Object[] enumConstants = rtype.getEnumConstants();
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
			} else if (rtype.isInterface()) {
				// properties
				Syntax[] clauses = create(rtype, Syntax::toProperty, false);
				syntaxes.add(new Syntax(name, lead, example, values, pattern, clauses));
			}
			if (rtype.isEnum()) {
				Field[] enumConstants = rtype.getFields();
				Syntax[] fields = new Syntax[enumConstants.length];

				for (int i = 0; i < enumConstants.length; i++) {
					Field e = enumConstants[i];
					fields[i] = createEnumField(e);
				}
				Syntax syntax = new Syntax(name, lead, example, values, pattern, fields);
				syntaxes.add(syntax);
			} else {
				// simple value
				syntaxes.add(new Syntax(name, lead, example, values, pattern));
			}

		}

		return syntaxes.toArray(new Syntax[0]);
	}

	private static Syntax createEnumField(Field e) {
		String name = e.getName();
		String lead = null;
		String example = null;
		Pattern pattern = Pattern.compile(Pattern.quote(e.getName()));
		SyntaxAnnotation sa = e.getAnnotation(SyntaxAnnotation.class);
		if (sa != null) {
			if (!sa.name()
				.isEmpty())
				name = sa.name();
			if (!sa.lead()
				.isEmpty())
				lead = sa.lead();
			if (!sa.example()
				.isEmpty())
				example = sa.example();
			if (!sa.pattern()
				.isEmpty())
				pattern = Pattern.compile(sa.pattern(), Pattern.LITERAL);
		}
		return new Syntax(name, lead, example, name, pattern);
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

	@Override
	public String toString() {
		return header;
	}

	public static <T> T getInstructions(Processor processor, Class<T> type) {
		return ProcessorHandler.getInstructions(processor, type);
	}

}
