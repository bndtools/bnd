package aQute.bnd.help.instructions;

import java.util.Map;
import java.util.Optional;

import aQute.bnd.annotation.ProviderType;
import aQute.bnd.header.Attrs;
import aQute.bnd.help.SyntaxAnnotation;
import aQute.bnd.osgi.Constants;

@ProviderType
public interface ProjectInstructions {

	@ProviderType
	interface StaleTest {

		Attrs _attrs();

		@SyntaxAnnotation(lead = "Generate a warning when the check fails. The files out of date will be append to the message", example = "'generate openapi from commandline'")
		Optional<String> warning();

		@SyntaxAnnotation(lead = "Generate a warning when the check fails. The files out of date will be append to the message", example = "''out of date files'")
		Optional<String> error();

		@SyntaxAnnotation(lead = "If the source (the key) is newer than the directories the error/warning will be generated. Ant globbing can be used, if necessary with multiple specs separated by a colon (,)", example = "newer='gen-src/**.java,checksum.sha1'")
		String newer();

		@SyntaxAnnotation(lead = "Run the given command if the source is newer. If the command starts with a '-' then it is ok to fail")
		Optional<String> command();

	}

	/**
	 * STALECHECK
	 */
	@SyntaxAnnotation(name = Constants.STALECHECK, lead = "Check if directories/files are out of date with other files/directories using ant like globbing", example = "-stalecheck openapi.json;>gen-src/**.java;warning='OpenAPI file needs to be regenerated")
	Map<String, StaleTest> stalecheck();

	/**
	 * Generate source code
	 */
	@SyntaxAnnotation(name = Constants.GENERATE, lead = "Generate sources before compilation, the key is a File set", example = "-generate openapi.json;src-gen/;run='openapi -o src-gen --package com.example openapi.json'")
	Map<String, GeneratorSpec> generate();

	@ProviderType
	interface GeneratorSpec {

		Attrs _attrs();

		@SyntaxAnnotation(lead = "The output directory")
		String output();

		@SyntaxAnnotation(lead = "Run the given command if the source is newer. If the command starts with a '-' then it is ok to fail")
		Optional<String> system();

		@SyntaxAnnotation(lead = "Run a Generate plugin before compilation")
		Optional<String> generate();

		@SyntaxAnnotation(lead = "Specify a classpath for a Main class plugin (name in generate must be a fqn class name)")
		Optional<String> classpath();

		@SyntaxAnnotation(lead = "Specify a JAR version for a Main class plugin (name in generate must be a fqn class name)")
		Optional<String> version();

		@SyntaxAnnotation(lead = "Determines if the output directory needs to be cleared before the generator runs. The default is true.")
		Optional<Boolean> clear();
	}

	/**
	 * The -launcher function is intended to hold options for the runtime
	 * launcher.
	 */

	@SyntaxAnnotation(name = Constants.LAUNCHER, lead = "Return specific options for the launcher", example = "-launcher manage=all")
	LauncherOptions launcher();

	/**
	 * the -launcher instruction is a set of properties, represented in this
	 * interface
	 */
	interface LauncherOptions {

		/**
		 * When the framework is launched, the launcher might find bundles that
		 * were not part of the run bundles. This is valid if one of the managed
		 * bundles is a management agent that installs these bundles, for
		 * example via a remote management system. In that case, the launcher
		 * should not touch those other bundles. However, sometimes the bundles
		 * are installed manually. In that case they should be managed.
		 * <p>
		 * This was added because the launcher was originally always assuming a
		 * management agent and kept its hands off these bundles. However, a
		 * change was introduced that made the launcher set the start level of
		 * all bundles and this was not discovered for 3 versions. Therefore
		 * this option was introduced with a default of
		 * {@link LauncherManage#all}
		 */
		@SyntaxAnnotation(lead = "Option to set the bundles that need to be managed by the launcher. narrow means only "
			+ "the bundles that are defined or calculated in the run bundles, all means all installed bundles, and "
			+ "none means that no bundles should be managed. Aspects that fall under management are: startlevel", example = "-launcher manage=all")
		LauncherManage manage();
	}

	/**
	 * Possible values for for the `manage` option.
	 */
	enum LauncherManage {
		@SyntaxAnnotation(lead = "Only manage the bundles specified in -runbundles")
		narrow,
		@SyntaxAnnotation(lead = "Manage all bundles")
		all,
		@SyntaxAnnotation(lead = "Manage no bundles")
		none;
	}

}
