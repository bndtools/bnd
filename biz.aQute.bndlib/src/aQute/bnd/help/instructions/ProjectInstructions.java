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
	}

}
