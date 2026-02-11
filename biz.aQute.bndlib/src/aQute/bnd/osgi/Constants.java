package aQute.bnd.osgi;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

import aQute.bnd.unmodifiable.Sets;

public interface Constants {
	String		AUTOMATIC_MODULE_NAME						= "Automatic-Module-Name";

	/**
	 * Consant string to be used when reporting it is not a bundle.
	 */
	String		NOT_A_BUNDLE_S								= "Not a bundle";

	/**
	 * Magic suffix to bsn that when used imply that the caller wants the
	 * sources for the bsn- suffix.
	 */
	String		BSN_SOURCE_SUFFIX							= ".source";

	String		IDENTITY_INITIAL_RESOURCE					= "<<INITIAL>>";
	String		IDENTITY_SYSTEM_RESOURCE					= "<<SYSTEM>>";

	/*
	 * Defined in OSGi
	 */
	String		BND_ADDXMLTOTEST							= "Bnd-AddXMLToTest";
	String		BUNDLE_ACTIVATIONPOLICY						= "Bundle-ActivationPolicy";
	String		BUNDLE_ACTIVATOR							= "Bundle-Activator";
	String		BUNDLE_BLUEPRINT							= "Bundle-Blueprint";
	String		BUNDLE_CATEGORY								= "Bundle-Category";
	String		BUNDLE_CLASSPATH							= "Bundle-ClassPath";
	String		BUNDLE_CONTACTADDRESS						= "Bundle-ContactAddress";
	String		BUNDLE_COPYRIGHT							= "Bundle-Copyright";
	String		BUNDLE_DESCRIPTION							= "Bundle-Description";
	String		BUNDLE_DOCURL								= "Bundle-DocURL";
	String		BUNDLE_ICON									= "Bundle-Icon";
	String		BUNDLE_LICENSE								= "Bundle-License";
	String		BUNDLE_LOCALIZATION							= "Bundle-Localization";
	String		BUNDLE_MANIFESTVERSION						= "Bundle-ManifestVersion";
	String		BUNDLE_NAME									= "Bundle-Name";
	String		BUNDLE_NATIVECODE							= "Bundle-NativeCode";
	String		BUNDLE_REQUIREDEXECUTIONENVIRONMENT			= "Bundle-RequiredExecutionEnvironment";
	String		BUNDLE_SYMBOLICNAME							= "Bundle-SymbolicName";
	String		BUNDLE_UPDATELOCATION						= "Bundle-UpdateLocation";
	String		BUNDLE_VENDOR								= "Bundle-Vendor";
	String		BUNDLE_VERSION								= "Bundle-Version";
	String		BUNDLE_DEVELOPERS							= "Bundle-Developers";
	String		BUNDLE_CONTRIBUTORS							= "Bundle-Contributors";
	String		BUNDLE_SCM									= "Bundle-SCM";
	String		DYNAMICIMPORT_PACKAGE						= "DynamicImport-Package";
	String		EXPORT_PACKAGE								= "Export-Package";
	String		EXPORT_SERVICE								= "Export-Service";
	String		FRAGMENT_HOST								= "Fragment-Host";
	String		IMPORT_PACKAGE								= "Import-Package";
	String		IMPORT_SERVICE								= "Import-Service";
	String		LAUNCHER_PLUGIN								= "Launcher-Plugin";
	String		MAIN_CLASS									= "Main-Class";
	String		META_PERSISTENCE							= "Meta-Persistence";
	String		PROVIDE_CAPABILITY							= "Provide-Capability";
	String		REQUIRE_BUNDLE								= "Require-Bundle";
	String		REQUIRE_CAPABILITY							= "Require-Capability";
	String		SERVICE_COMPONENT							= "Service-Component";
	String		TESTER_PLUGIN								= "Tester-Plugin";

	String		PRIVATE_PACKAGE								= "Private-Package";
	String		IGNORE_PACKAGE								= "Ignore-Package";
	@Deprecated
	String		INCLUDE_RESOURCE							= "Include-Resource";
	String		CONDITIONAL_PACKAGE							= "Conditional-Package";
	String		BND_LASTMODIFIED							= "Bnd-LastModified";
	String		CREATED_BY									= "Created-By";
	String		TOOL										= "Tool";
	String		TESTCASES									= "Test-Cases";
	String		REPOSITORIES								= "Repositories";

	Set<String>	headers										= Sets.of(BUNDLE_ACTIVATOR, BUNDLE_CONTACTADDRESS,
		BUNDLE_COPYRIGHT, BUNDLE_DESCRIPTION, BUNDLE_DOCURL, BUNDLE_LOCALIZATION, BUNDLE_NATIVECODE, BUNDLE_VENDOR,
		BUNDLE_VERSION, BUNDLE_LICENSE, BUNDLE_CLASSPATH, SERVICE_COMPONENT, EXPORT_PACKAGE, IMPORT_PACKAGE,
		BUNDLE_MANIFESTVERSION, BUNDLE_NAME, BUNDLE_REQUIREDEXECUTIONENVIRONMENT, BUNDLE_SYMBOLICNAME, FRAGMENT_HOST,
		PRIVATE_PACKAGE, IGNORE_PACKAGE, INCLUDE_RESOURCE, REQUIRE_BUNDLE, IMPORT_SERVICE, EXPORT_SERVICE,
		CONDITIONAL_PACKAGE, BND_LASTMODIFIED, TESTCASES, REQUIRE_CAPABILITY, PROVIDE_CAPABILITY, BUNDLE_ICON,
		REPOSITORIES, META_PERSISTENCE, BUNDLE_DEVELOPERS, BUNDLE_CONTRIBUTORS, BUNDLE_SCM, LAUNCHER_PLUGIN,
		TESTER_PLUGIN, AUTOMATIC_MODULE_NAME, DYNAMICIMPORT_PACKAGE);

	Set<String>	OSGI_SYNTAX_HEADERS							= Sets.of(BUNDLE_ACTIVATIONPOLICY, BUNDLE_ACTIVATOR,
		BUNDLE_BLUEPRINT, BUNDLE_CLASSPATH, BUNDLE_DEVELOPERS, BUNDLE_ICON, BUNDLE_LICENSE, BUNDLE_NATIVECODE,
		BUNDLE_SCM, BUNDLE_SYMBOLICNAME, BUNDLE_UPDATELOCATION, DYNAMICIMPORT_PACKAGE, EXPORT_PACKAGE, EXPORT_SERVICE,
		FRAGMENT_HOST, IMPORT_PACKAGE, IMPORT_SERVICE, LAUNCHER_PLUGIN, PROVIDE_CAPABILITY, REQUIRE_BUNDLE,
		REQUIRE_CAPABILITY, SERVICE_COMPONENT, PRIVATE_PACKAGE, IGNORE_PACKAGE, TESTCASES);

	String		BASELINE									= "-baseline";
	String		BASELINEREPO								= "-baselinerepo";

	String		BNDDRIVER									= "-bnd-driver";
	String		BNDDRIVER_BND								= "bnd";
	String		BNDDRIVER_GRADLE							= "gradle";
	String		BNDDRIVER_GRADLE_NATIVE						= "gradle_native";
	String		BNDDRIVER_ANT								= "ant";
	String		BNDDRIVER_ECLIPSE							= "eclipse";
	String		BNDDRIVER_MAVEN								= "maven";
	String		BNDDRIVER_INTELLIJ							= "intellij";
	String		BNDDRIVER_SBT								= "sbt";
	String		BNDDRIVER_OSMORC							= "osmorc";

	String		BUILDERIGNORE								= "-builderignore";
	String		BUILDPATH									= "-buildpath";
	String		BUILDTOOL									= "-buildtool";
	String		BUMPPOLICY									= "-bumppolicy";
	String		BUNDLEANNOTATIONS							= "-bundleannotations";
	String		CDIANNOTATIONS								= "-cdiannotations";
	String		CHECK										= "-check";
	String		CONDUIT										= "-conduit";
	String		CONTRACT									= "-contract";
	String		DEFINE_CONTRACT								= "-define-contract";
	String		CONDITIONALPACKAGE							= "-conditionalpackage";
	String		CONNECTION_SETTINGS							= "-connection-settings";
	String		CONNECTION_LOG								= "-connection-log";

	String		COMPRESSION									= "-compression";
	String		DIFFIGNORE									= "-diffignore";
	String		DIFFPACKAGES								= "-diffpackages";
	String		DIFFPACKAGES_THRESHOLD						= "threshold";
	String		DEPENDSON									= "-dependson";
	String		DEPLOY										= "-deploy";
	String		DEPLOYREPO									= "-deployrepo";
	String		DIGESTS										= "-digests";
	String		DSANNOTATIONS								= "-dsannotations";
	String		DSANNOTATIONS_OPTIONS						= "-dsannotations-options";

	String		DONOTCOPY									= "-donotcopy";
	String		EXPORT_CONTENTS								= "-exportcontents";
	String		EXTENSION									= "-extension";
	String		LIBRARY										= "-library";
	String		EEPROFILE									= "-eeprofile";
	String		EXECUTABLE									= "-executable";
	String		EXPORT										= "-export";
	String		EXPORT_TYPE									= "type";
	String		EXPORT_BSN									= "bsn";
	String		EXPORT_NAME									= "name";
	String		EXPORTREPORT								= "-exportreport";
	String		EXPORT_VERSION								= "version";

	String		EXPORTTYPE									= "-exporttype";
	String		EXPORT_APIGUARDIAN							= "-export-apiguardian";
	String		FAIL_OK										= "-failok";
	String		FIXUPMESSAGES								= "-fixupmessages";
	String		GENERATE									= "-generate";

	String		GESTALT										= "-gestalt";
	String		GESTALT_INTERACTIVE							= "interactive";
	String		GESTALT_BATCH								= "batch";
	String		GESTALT_CI									= "ci";
	String		GESTALT_OFFLINE								= "offline";
	String		GESTALT_SHELL								= "shell";
	String		GROUPID										= "-groupid";
	String		INCLUDE										= "-include";
	String		INCLUDEZEROMAJOR							= "-includezeromajor";
	String		INCLUDERESOURCE								= "-includeresource";
	String		INCLUDEPACKAGE								= "-includepackage";
	String		INVALIDFILENAMES							= "-invalidfilenames";
	String		INIT										= "-init";
	String		BUILDREPO									= "-buildrepo";
	String		JAVAAGENT									= "-javaagent";
	String		JAVAC_ENCODING								= "javac.encoding";
	String		JAVAC_SOURCE								= "javac.source";
	String		JAVAC_TARGET								= "javac.target";
	String		JAVAC_PROFILE								= "javac.profile";
	String		JAVAC										= "javac";
	String		JAVA										= "java";
	String		JAVA_DEBUG									= "java.debug";
	String		LAUNCHER									= "-launcher";

	String		MAKE										= "-make";
	String		METATYPE_ANNOTATIONS						= "-metatypeannotations";
	String		METATYPE_ANNOTATIONS_OPTIONS				= "-metatypeannotations-options";
	String		MANIFEST									= "-manifest";
	String		MAVEN_RELEASE								= "-maven-release";
	String		MAVEN_RELEASE_CLASSIFIER					= "classifier";
	String		MAVEN_RELEASE_ARCHIVE						= "archive";
	String		MAVEN_RELEASE_PATH							= "path";
	String		MAVEN_DEPENDENCIES							= "-maven-dependencies";
	String		MAVEN_SCOPE									= "-maven-scope";
	String		PROFILE										= "-profile";
	String		SAVEMANIFEST								= "-savemanifest";
	String		NAMESECTION									= "-namesection";
	String		NOBUILDINCACHE								= "-nobuildincache";
	String		NODEFAULTVERSION							= "-nodefaultversion";
	String		NOEXTRAHEADERS								= "-noextraheaders";
	String		NOJUNIT										= "-nojunit";
	String		NOJUNITOSGI									= "-nojunitosgi";
	String		NOEE										= "-noee";

	String		NOMANIFEST									= "-nomanifest";
	String		MANIFEST_NAME								= "-manifest-name";
	String		NOUSES										= "-nouses";
	String		NOCLASSFORNAME								= "-noclassforname";
	String		NOPROXYINTERFACES							= "-noproxyinterfaces";
	String		NOIMPORTJAVA								= "-noimportjava";
	String		NOSUBSTITUTION								= "-nosubstitution";
	String		NOBUNDLES									= "-nobundles";
	String		NOPARALLEL									= "-noparallel";
	String		OUTPUTMASK									= "-outputmask";																																						// default
																																																									// ${@bsn}.jar

	String		PEDANTIC									= "-pedantic";
	String		PACKAGEINFOTYPE								= "-packageinfotype";
	String		PLUGIN										= "-plugin";
	String		PLUGINPATH									= "-pluginpath";
	String		PLUGINPATH_URL_ATTR							= "url";
	String		PLUGINPATH_SHA1_ATTR						= "sha1";
	String		POM											= "-pom";
	String		PREPARE										= "-prepare";
	String		PREPROCESSMATCHERS							= "-preprocessmatchers";
	String		PRIVATEPACKAGE								= "-privatepackage";
	String		RELEASEREPO									= "-releaserepo";
	String		REPORTCONFIG								= "-reportconfig";
	String		DISTRO										= "-distro";
	String		REMOVEHEADERS								= "-removeheaders";
	String		RESOURCEONLY								= "-resourceonly";
	String		SIGNATURE_TEST								= "-signaturetest";
	String		SOURCES										= "-sources";
	String		SOURCEPATH									= "-sourcepath";
	String		STRICT										= "-strict";
	String		SUB											= "-sub";
	String		REPRODUCIBLE								= "-reproducible";

	String		RESOLVE										= "-resolve";
	String		RESOLVEDEBUG								= "-resolvedebug";
	String		RESOLVE_REJECT								= "-resolve.reject";

	/**
	 * Exclude the system resource from the resulting wiring in resolve. The
	 * default is true
	 */
	String		RESOLVE_EXCLUDESYSTEM						= "-resolve.excludesystem";
	String		RESOLVE_EFFECTIVE							= "-resolve.effective";
	String		RESOLVE_PREFERENCES							= "-resolve.preferences";

	String		RUNNOREFERENCES								= "-runnoreferences";
	String		RUNFRAMEWORKRESTART							= "-runframeworkrestart";
	String		RUNOPTIONS									= "-runoptions";
	String		RUNOPTIONS_EAGER							= "eager";

	String		RUNPROPERTIES								= "-runproperties";
	String		RUNSYSTEMPACKAGES							= "-runsystempackages";
	String		RUNSYSTEMCAPABILITIES						= "-runsystemcapabilities";
	String		RUNPROVIDEDCAPABILITIES						= "-runprovidedcapabilities";

	String		RUNBUNDLES									= "-runbundles";
	String		RUNBUNDLES_DECORATOR						= RUNBUNDLES + "+";
	String		RUNBUNDLES_STARTLEVEL_ATTRIBUTE				= "startlevel";

	String		RUNSTARTLEVEL								= "-runstartlevel";
	String		RUNSTARTLEVEL_ORDER							= "order";
	String		RUNSTARTLEVEL_BEGIN							= "begin";
	String		RUNSTARTLEVEL_STEP							= "step";

	String		AUGMENT										= "-augment";
	String		AUGMENT_RANGE_ATTRIBUTE						= "version:";
	String		AUGMENT_CAPABILITY_DIRECTIVE				= "capability:";
	String		AUGMENT_REQUIREMENT_DIRECTIVE				= "requirement:";

	String		REMOTEWORKSPACE								= "-remoteworkspace";

	/**
	 * tag for repos which should be used for Resolving bundles. This is also
	 * the default tag for all repos which not have specified tags (also for bc
	 * reasons)
	 */
	String		REPOTAGS_RESOLVE							= "resolve";

	String		RUNBLACKLIST								= "-runblacklist";
	String		RUNREQUIRES									= "-runrequires";
	String		RUNEE										= "-runee";
	String		RUNKEEP										= "-runkeep";
	String		RUNPATH										= "-runpath";
	String		RUNSTORAGE									= "-runstorage";
	String		RUNBUILDS									= "-runbuilds";
	String		RUNVM										= "-runvm";
	String		RUNPROGRAMARGS								= "-runprogramargs";
	String		RUNTRACE									= "-runtrace";
	String		RUNFRAMEWORK								= "-runframework";
	String		RUNREPOS									= "-runrepos";
	String		RUNFW										= "-runfw";
	String		RUNTIMEOUT									= "-runtimeout";
	String		RUNJDB										= "-runjdb";
	String		RUNENV										= "-runenv";
	String		RUNREMOTE									= "-runremote";
	String		SYSTEMPROPERTIES							= "-systemproperties";
	String		SNAPSHOT									= "-snapshot";
	String		RUNFRAMEWORK_SERVICES						= "services";
	String		RUNFRAMEWORK_NONE							= "none";
	String		REPORTNEWER									= "-reportnewer";
	String		SIGN										= "-sign";
	String		STALECHECK									= "-stalecheck";
	String		STANDALONE									= "-standalone";
	String		IGNORE_STANDALONE							= "-ignore-standalone";
	String		TESTPACKAGES								= "-testpackages";
	String		TESTPATH									= "-testpath";
	String		TESTCONTINUOUS								= "-testcontinuous";
	String		TESTTERMINATE								= "-testterminate";
	String		TESTSOURCES									= "-testsources";
	String		TESTUNRESOLVED								= "-testunresolved";
	String		TESTER										= "-tester";
	String		UNDERTEST									= "-undertest";
	String		UPTO										= "-upto";
	String		PROVIDER_POLICY								= "-provider-policy";
	String		CONSUMER_POLICY								= "-consumer-policy";
	String		VERSIONDEFAULTS								= "-versiondefaults";
	String		WAB											= "-wab";
	String		WABLIB										= "-wablib";
	String		WORKINGSET									= "-workingset";
	String		WORKINGSET_MEMBER							= "member";
	String		WORKSPACE_TEMPLATES							= "-workspace-templates";

	String		REQUIRE_BND									= "-require-bnd";

	/*
	 * processing of META-INF/services folder section.
	 */
	String		METAINF_SERVICES							= "-metainf-services";
	String		METAINF_SERVICES_STRATEGY_ANNOTATION		= "annotation";
	String		METAINF_SERVICES_STRATEGY_AUTO				= "auto";
	String		METAINF_SERVICES_STRATEGY_NONE				= "none";

	// Deprecated
	String		CLASSPATH									= "-classpath";
	String		OUTPUT										= "-output";

	Set<String>	options										= Sets.of(BASELINE, BUILDPATH, BUMPPOLICY, CONDUIT,
		CLASSPATH, COMPRESSION, CONSUMER_POLICY, DEPENDSON, DONOTCOPY, EXPORT_CONTENTS, FAIL_OK, INCLUDE,
		INCLUDEZEROMAJOR, INCLUDERESOURCE, MAKE, MANIFEST, NOEXTRAHEADERS, NOUSES, NOBUNDLES, PEDANTIC, PLUGIN, POM, PROVIDER_POLICY,
		REMOVEHEADERS, RESOURCEONLY, SOURCES, SOURCEPATH, SUB, RUNBUNDLES, RUNPATH, RUNSYSTEMPACKAGES,
		RUNSYSTEMCAPABILITIES, RUNPROPERTIES, REPORTNEWER, UNDERTEST, TESTPATH, TESTPACKAGES, NOMANIFEST, DEPLOYREPO,
		RELEASEREPO, SAVEMANIFEST, RUNVM, RUNPROGRAMARGS, WAB, WABLIB, RUNFRAMEWORK, RUNFW, RUNKEEP, RUNTRACE,
		RUNBLACKLIST, TESTCONTINUOUS, SNAPSHOT, NAMESECTION, DIGESTS, DSANNOTATIONS, DSANNOTATIONS_OPTIONS,
		BASELINEREPO, PROFILE, EXECUTABLE, RUNNOREFERENCES, JAVAAGENT, STRICT, DIFFIGNORE, DIFFPACKAGES, CONTRACT,
		NOBUILDINCACHE, EXTENSION, NOJUNIT, NOJUNITOSGI, PREPROCESSMATCHERS, UPTO, INVALIDFILENAMES, FIXUPMESSAGES,
		PRIVATEPACKAGE, CONDITIONALPACKAGE, NOEE, OUTPUTMASK, TESTUNRESOLVED, RUNJDB, RUNENV, RUNEE, EEPROFILE,
		RUNREQUIRES, EXPORT, GESTALT, BNDDRIVER, CHECK, DISTRO, METATYPE_ANNOTATIONS, METATYPE_ANNOTATIONS_OPTIONS,
		PACKAGEINFOTYPE, JAVAC_SOURCE, JAVAC_TARGET, JAVAC_PROFILE, JAVAC, JAVA, JAVA_DEBUG, EXPORTTYPE, RUNREMOTE,
		TESTER, AUGMENT, REQUIRE_BND, GROUPID, STANDALONE, IGNORE_STANDALONE, RUNREPOS, INIT, MAVEN_RELEASE, BUILDREPO,
		CONNECTION_SETTINGS, RUNPROVIDEDCAPABILITIES, WORKINGSET, RUNSTORAGE, REPRODUCIBLE, INCLUDEPACKAGE,
		CDIANNOTATIONS, REMOTEWORKSPACE, MAVEN_DEPENDENCIES, BUILDERIGNORE, STALECHECK, MAVEN_SCOPE, RUNSTARTLEVEL,
		RUNOPTIONS, NOCLASSFORNAME, NOPROXYINTERFACES, EXPORT_APIGUARDIAN, RESOLVE, DEFINE_CONTRACT, GENERATE,
		RUNFRAMEWORKRESTART,
		NOIMPORTJAVA, NOSUBSTITUTION, VERSIONDEFAULTS, LIBRARY, METAINF_SERVICES);

	// Ignore bundle specific headers. These headers do not make a lot of sense
	// to inherit
	Set<String>	BUNDLE_SPECIFIC_HEADERS						= Sets.of(INCLUDE_RESOURCE, INCLUDERESOURCE,
		BUNDLE_ACTIVATOR, BUNDLE_CLASSPATH, BUNDLE_NAME, BUNDLE_NATIVECODE, BUNDLE_SYMBOLICNAME, IMPORT_PACKAGE,
		EXPORT_PACKAGE, DYNAMICIMPORT_PACKAGE, FRAGMENT_HOST, REQUIRE_BUNDLE, PRIVATE_PACKAGE, PRIVATEPACKAGE,
		INCLUDEPACKAGE, EXPORT_CONTENTS, TESTCASES, NOMANIFEST, WAB, WABLIB, REQUIRE_CAPABILITY, PROVIDE_CAPABILITY,
		DSANNOTATIONS, SERVICE_COMPONENT, SIGNATURE_TEST, METATYPE_ANNOTATIONS, CDIANNOTATIONS, AUTOMATIC_MODULE_NAME,
		EXPORT_APIGUARDIAN, DEFINE_CONTRACT);

	char		DUPLICATE_MARKER							= '~';
	String		INTERNAL_EXPORTED_DIRECTIVE					= "-internal-exported:";
	String		INTERNAL_BUNDLESYMBOLICNAME_DIRECTIVE		= "-internal-bundlesymbolicname:";
	String		INTERNAL_BUNDLEVERSION_DIRECTIVE			= "-internal-bundleversion:";
	String		INTERNAL_EXPORT_TO_MODULES_DIRECTIVE		= "-internal-export-to-modules:";
	String		INTERNAL_MODULE_DIRECTIVE					= "-internal-module:";
	String		INTERNAL_MODULE_VERSION_DIRECTIVE			= "-internal-module-version:";
	String		INTERNAL_OPEN_TO_MODULES_DIRECTIVE			= "-internal-open-to-modules:";
	String		INTERNAL_SOURCE_DIRECTIVE					= "-internal-source:";
	String		SPECIFICATION_VERSION						= "specification-version";
	String		SPLIT_PACKAGE_DIRECTIVE						= "-split-package:";
	String		EFFECTIVE_DIRECTIVE							= "effective:";
	String		IMPORT_DIRECTIVE							= "-import:";
	String		NO_IMPORT_DIRECTIVE							= "-noimport:";
	String		REMOVE_ATTRIBUTE_DIRECTIVE					= "-remove-attribute:";
	String		LIB_DIRECTIVE								= "lib:";
	String		NOANNOTATIONS								= "-noannotations";
	String		COMMAND_DIRECTIVE							= "command:";
	String		USES_DIRECTIVE								= "uses:";
	String		MANDATORY_DIRECTIVE							= "mandatory:";
	String		INCLUDE_DIRECTIVE							= "include:";
	String		OPTIONAL									= "optional";
	String		PROVIDE_DIRECTIVE							= "provide:";
	String		EXCLUDE_DIRECTIVE							= "exclude:";
	String		FILTER_DIRECTIVE							= "filter:";
	String		PRESENCE_DIRECTIVE							= "presence:";
	String		PRIVATE_DIRECTIVE							= "private:";
	String		SINGLETON_DIRECTIVE							= "singleton:";
	String		EXTENSION_DIRECTIVE							= "extension:";
	String		VISIBILITY_DIRECTIVE						= "visibility:";
	String		FRAGMENT_ATTACHMENT_DIRECTIVE				= "fragment-attachment:";
	String		RESOLUTION									= "resolution";
	String		RESOLUTION_DIRECTIVE						= "resolution:";
	String		CARDINALITY_DIRECTIVE						= "cardinality:";
	String		PATH_DIRECTIVE								= "path:";
	String		SIZE_ATTRIBUTE								= "size";
	String		LINK_ATTRIBUTE								= "link";
	String		LITERAL_ATTRIBUTE							= "literal";
	String		CLASS_ATTRIBUTE								= "class";
	String		NAME_ATTRIBUTE								= "name";
	String		RESOLUTION_DYNAMIC							= "dynamic";
	String		DESCRIPTION_ATTRIBUTE						= "description";
	String		OSNAME_ATTRIBUTE							= "osname";
	String		OSVERSION_ATTRIBUTE							= "osversion";
	String		PROCESSOR_ATTRIBUTE							= "processor";
	String		LANGUAGE_ATTRIBUTE							= "language";
	String		SELECTION_FILTER_ATTRIBUTE					= "selection-filter";
	String		BLUEPRINT_WAIT_FOR_DEPENDENCIES_ATTRIBUTE	= "blueprint.wait-for-dependencies";
	String		BLUEPRINT_TIMEOUT_ATTRIBUTE					= "blueprint.timeout";
	String		VERSION_ATTRIBUTE							= "version";
	String		BUNDLE_SYMBOLIC_NAME_ATTRIBUTE				= "bundle-symbolic-name";
	String		BUNDLE_VERSION_ATTRIBUTE					= "bundle-version";
	String		FROM_DIRECTIVE								= "from:";
	String		FROM_DIRECTIVE_PROJECT						= "project";

	String		KEYSTORE_LOCATION_DIRECTIVE					= "keystore:";
	String		KEYSTORE_PROVIDER_DIRECTIVE					= "provider:";
	String		KEYSTORE_PASSWORD_DIRECTIVE					= "password:";
	String		SIGN_PASSWORD_DIRECTIVE						= "sign-password:";
	String		FIXUPMESSAGES_RESTRICT_DIRECTIVE			= "restrict:";
	String		FIXUPMESSAGES_REPLACE_DIRECTIVE				= "replace:";
	String		FIXUPMESSAGES_IS_DIRECTIVE					= "is:";
	String		FIXUPMESSAGES_IS_ERROR						= "error";
	String		FIXUPMESSAGES_IS_WARNING					= "warning";
	String		FIXUPMESSAGES_IS_IGNORE						= "ignore";
	String		EEPROFILE_AUTO_ATTRIBUTE					= "auto";
	String		NONE										= "none";

	/*
	 * -includeresource directives for duplicate handling strategy
	 */
	String		DUP_STRATEGY								= "onduplicate:";

	Set<String>	directives									= Sets.of(SPLIT_PACKAGE_DIRECTIVE, NO_IMPORT_DIRECTIVE,
		IMPORT_DIRECTIVE, RESOLUTION_DIRECTIVE, INCLUDE_DIRECTIVE, USES_DIRECTIVE, EXCLUDE_DIRECTIVE,
		KEYSTORE_LOCATION_DIRECTIVE, KEYSTORE_PROVIDER_DIRECTIVE, KEYSTORE_PASSWORD_DIRECTIVE, SIGN_PASSWORD_DIRECTIVE,
		COMMAND_DIRECTIVE, NOANNOTATIONS, LIB_DIRECTIVE, FROM_DIRECTIVE, PRIVATE_DIRECTIVE, LITERAL_ATTRIBUTE,
		EFFECTIVE_DIRECTIVE, FILTER_DIRECTIVE, FIXUPMESSAGES_RESTRICT_DIRECTIVE, FIXUPMESSAGES_REPLACE_DIRECTIVE,
		FIXUPMESSAGES_IS_DIRECTIVE, BNDDRIVER_GRADLE, BNDDRIVER_GRADLE_NATIVE, BNDDRIVER_ANT, BNDDRIVER_ECLIPSE,
		BNDDRIVER_MAVEN, BNDDRIVER_INTELLIJ, BNDDRIVER_SBT, BNDDRIVER_OSMORC, AUGMENT_CAPABILITY_DIRECTIVE,
		AUGMENT_REQUIREMENT_DIRECTIVE, DUP_STRATEGY);

	String		USES_USES									= "<<USES>>";
	String		CURRENT_USES								= "@uses";
	String		IMPORT_REFERENCE							= "reference";
	String		IMPORT_PRIVATE								= "private";
	Set<String>	importDirectives							= Sets.of(IMPORT_REFERENCE, IMPORT_PRIVATE);

	Pattern		VALID_PROPERTY_TYPES						= Pattern
		.compile("(String|Long|Double|Float|Integer|Byte|Character|Boolean|Short)");

	String		DEFAULT_BND_EXTENSION						= ".bnd";
	String		DEFAULT_JAR_EXTENSION						= ".jar";
	String		DEFAULT_BAR_EXTENSION						= ".bar";
	String		DEFAULT_BNDRUN_EXTENSION					= ".bndrun";
	Set<String>	METAPACKAGES								= Sets.of("META-INF", "OSGI-INF", "OSGI-OPT");

	String		MIME_TYPE_BUNDLE							= "application/vnd.osgi.bundle";
	String		MIME_TYPE_JAR								= "application/java-archive";

	String		CURRENT_VERSION								= "@";
	String		CURRENT_PACKAGE								= "@package";
	String		CURRENT_BUNDLESYMBOLICNAME					= "@bundlesymbolicname";
	String		CURRENT_BUNDLEVERSION						= "@bundleversion";

	String		BUILDFILES									= "buildfiles";

	String		EMPTY_HEADER								= "<<EMPTY>>";

	String		EMBEDDED_REPO								= "/embedded-repo.jar";

	String		DEFAULT_LAUNCHER_BSN						= "biz.aQute.launcher";
	String		DEFAULT_TESTER_BSN							= "biz.aQute.junit";

	String		DEFAULT_DO_NOT_COPY							= "CVS|\\.svn|\\.git|\\.DS_Store|\\.gitignore";

	Charset		DEFAULT_CHARSET								= StandardCharsets.UTF_8;
	String		VERSION_FILTER								= "version";
	String		PROVIDER_TYPE_DIRECTIVE						= "x-provider-type:";
	/**
	 * Component constants
	 */
	String		NAMESPACE_STEM								= "http://www.osgi.org/xmlns/scr";
	String		JIDENTIFIER									= "<<identifier>>";
	String		COMPONENT_NAME								= "name:";
	String		COMPONENT_FACTORY							= "factory:";
	String		COMPONENT_SERVICEFACTORY					= "servicefactory:";
	String		COMPONENT_IMMEDIATE							= "immediate:";
	String		COMPONENT_ENABLED							= "enabled:";
	String		COMPONENT_DYNAMIC							= "dynamic:";
	String		COMPONENT_MULTIPLE							= "multiple:";
	String		COMPONENT_GREEDY							= "greedy:";
	String		COMPONENT_PROVIDE							= "provide:";
	String		COMPONENT_OPTIONAL							= "optional:";
	String		COMPONENT_PROPERTIES						= "properties:";
	String		COMPONENT_IMPLEMENTATION					= "implementation:";
	String		COMPONENT_DESIGNATE							= "designate:";
	String		COMPONENT_DESIGNATEFACTORY					= "designateFactory:";
	String		COMPONENT_DESCRIPTORS						= ".descriptors:";

	// v1.1.0
	String		COMPONENT_VERSION							= "version:";
	String		COMPONENT_CONFIGURATION_POLICY				= "configuration-policy:";
	String		COMPONENT_MODIFIED							= "modified:";
	String		COMPONENT_ACTIVATE							= "activate:";
	String		COMPONENT_DEACTIVATE						= "deactivate:";

	String		COMPONENT_NAMESPACE							= "xmlns:";

	Set<String>	COMPONENT_DIRECTIVES						= Sets.of(COMPONENT_FACTORY, COMPONENT_IMMEDIATE,
		COMPONENT_ENABLED, COMPONENT_DYNAMIC, COMPONENT_MULTIPLE, COMPONENT_PROVIDE, COMPONENT_OPTIONAL,
		COMPONENT_PROPERTIES, COMPONENT_IMPLEMENTATION, COMPONENT_SERVICEFACTORY, COMPONENT_VERSION,
		COMPONENT_CONFIGURATION_POLICY, COMPONENT_MODIFIED, COMPONENT_ACTIVATE, COMPONENT_DEACTIVATE, COMPONENT_NAME,
		COMPONENT_DESCRIPTORS, COMPONENT_DESIGNATE, COMPONENT_DESIGNATEFACTORY, COMPONENT_GREEDY, COMPONENT_NAMESPACE);

	Set<String>	COMPONENT_DIRECTIVES_1_1					=																																										//
		Sets.of(COMPONENT_VERSION, COMPONENT_CONFIGURATION_POLICY, COMPONENT_MODIFIED, COMPONENT_ACTIVATE,
			COMPONENT_DEACTIVATE);

	Set<String>	COMPONENT_DIRECTIVES_1_2					= Sets.of(COMPONENT_GREEDY);

	String		VERSION_ATTR_LATEST							= "latest";
	String		VERSION_ATTR_SNAPSHOT						= "snapshot";
	String		VERSION_ATTR_PROJECT						= "project";
	String		VERSION_ATTR_HASH							= "hash";

	/**
	 * List of standard matchers for preprocessing
	 */
	String		DEFAULT_PREPROCESSS_MATCHERS				= "!*.(ico|jpg|jpeg|jif|jfif|jp2|jpx|j2k|j2c|fpx|png|gif|swf|doc|pdf|tiff|tif|raw|bmp|ppm|pgm|pbm|pnm|pfm|webp|zip|jar|gz|tar|tgz|exe|com|bin|mp[0-9]|mpeg|mov|):i, *";

	/**
	 * Headers that if **all** are absent will trigger the -includepackage
	 * `*;from:=project` (include all packages from the project's output.
	 */
	Set<String>	EXPAND_HEADERS								= Sets.of(Constants.RESOURCEONLY, Constants.INCLUDEPACKAGE,
		Constants.PRIVATE_PACKAGE, Constants.PRIVATEPACKAGE, Constants.EXPORT_PACKAGE);

	/**
	 * Marker resource set by the ProjectBuilder to mark a JAR as the project
	 * output's entry in the classpath. Used for the {@link #EXPAND_HEADERS}
	 * processing.
	 */
	String		PROJECT_MARKER								= "META-INF/.project";

	/**
	 * Value for a *package instruction to include everything from the bin
	 * directory
	 */
	String		ALL_FROM_PROJECT							= "*;" + FROM_DIRECTIVE + "=" + FROM_DIRECTIVE_PROJECT;

	/**
	 * Default properties as listed in defaults.bnd
	 */

	String		DEFAULT_PROP_SRC_DIR						= "src";
	String		DEFAULT_PROP_BIN_DIR						= "bin";
	String		DEFAULT_PROP_TESTSRC_DIR					= "testsrc";
	String		DEFAULT_PROP_TESTBIN_DIR					= "testbin";
	String		DEFAULT_PROP_TARGET_DIR						= "target-dir";

	/**
	 * If set to a long (from epoch time), overrides the real time in the macro
	 * processor for tstamp
	 */
	String		TSTAMP										= "_@tstamp";

	/*
	 * Module Section
	 */
	String		JPMS_MODULE_INFO							= "-jpms-module-info";
	String		JPMS_MODULE_INFO_OPTIONS					= "-jpms-module-info-options";
	String		ACCESS_ATTRIBUTE							= "access";
	String		EE_ATTRIBUTE								= "ee";
	String		EXPORTS_ATTRIBUTE							= "exports";
	String		IGNORE_ATTRIBUTE							= "ignore";
	String		MODULES_ATTRIBUTE							= "modules";
	String		TRANSITIVE_ATTRIBUTE						= "transitive";
	String		STATIC_ATTRIBUTE							= "static";
	String		SUBSTITUTE_ATTRIBUTE						= "substitute";
	String		MODULE_INFO_CLASS							= "module-info.class";
	String		JPMS_MULTI_RELEASE							= "-jpms-multi-release";

	/*
	 * Service Loader Section
	 */

	String		SERVICELOADER_REGISTER_DIRECTIVE			= "register:";
	String		SERVICELOADER_NAMESPACE						= "osgi.serviceloader";

	/**
	 * Launch constants that should be shared by launchers
	 */
	String		LAUNCH_TRACE								= "launch.trace";

	/**
	 * Specifies the default bundle start level but it has more meanings. If it
	 * is 0, no start levels are handled. If is > 0, it manages the startlevels
	 * of all installed bundles, also from previous installations are ensure to
	 * have this start level if they do not appear in the set of runbundles. If
	 * it less than 0, its negated value is the default start level but _only_
	 * the bundles listed in the run bundles are managed. See -launcher
	 * instruction.
	 */
	String		LAUNCH_STARTLEVEL_DEFAULT					= "launch.startlevel.default";
	String		LAUNCH_RUNBUNDLES_ATTRS						= "launch.runbundles.attrs";
	String		LAUNCH_ACTIVATORS							= "launch.activators";
	String		LAUNCH_ACTIVATION_EAGER						= "launch.activation.eager";

	/**
	 * A list of headers that use merged properties
	 */
	Set<String>	MERGED_HEADERS								= Set.of(																																								//
		AUGMENT,																																																					//
		BUILDERIGNORE,																																																				//
		BUILDREPO,																																																					//
		BUILDPATH,																																																					//
		CONDITIONAL_PACKAGE,																																																		//
		CONNECTION_SETTINGS,																																																		//
		DEFINE_CONTRACT,																																																			//
		DEFAULT_PROP_SRC_DIR,																																																		//
		DEPENDSON,																																																					//
		DONOTCOPY,																																																					//
		DSANNOTATIONS_OPTIONS,																																																		//
		EXPORT_PACKAGE,																																																				//
		EXTENSION,																																																					//
		FIXUPMESSAGES,																																																				//
		GESTALT,																																																					//
		IMPORT_PACKAGE,																																																				//
		INCLUDERESOURCE,																																																			//
		MAKE,																																																						//
		MAVEN_DEPENDENCIES,																																																			//
		PLUGIN,																																																						//
		PLUGINPATH,																																																					//
		PREPROCESSMATCHERS,																																																			//
		PRIVATE_PACKAGE,																																																			//
		PROVIDE_CAPABILITY,																																																			//
		RELEASEREPO,																																																				//
		REMOVEHEADERS,																																																				//
		REQUIRE_CAPABILITY,																																																			//
		RESOLVE_REJECT,																																																				//
		RUNBLACKLIST,																																																				//
		RUNBUNDLES,																																																					//
		RUNBUNDLES_DECORATOR,																																																		//
		RUNPATH,																																																					//
		RUNPROGRAMARGS,																																																				//
		RUNPROPERTIES,																																																				//
		RUNPROVIDEDCAPABILITIES,																																																	//
		RUNREPOS,																																																					//
		RUNREQUIRES,																																																				//
		RUNSYSTEMCAPABILITIES,																																																		//
		RUNSYSTEMPACKAGES,																																																			//
		RUNVM,																																																						//
		SOURCEPATH,																																																					//
		STANDALONE,																																																					//
		SYSTEMPROPERTIES,																																																			//
		TESTPACKAGES,																																																				//
		TESTPATH,																																																					//
		WORKINGSET,																																																					//
		"-pomaugment");

	/**
	 * Any attributes that should be removed from the attributes before
	 * printing.
	 */
	Set<String>	BND_USE_ATTRIBUTES							= Sets.of(
	//@formatter:off
		FROM_DIRECTIVE,
		NO_IMPORT_DIRECTIVE,
		PROVIDE_DIRECTIVE,
		SPLIT_PACKAGE_DIRECTIVE
		//@formatter:on
	);

	String		INTERNAL_PREFIX								= "-internal-";

	/*
	 * Deprecated Section
	 */

}
