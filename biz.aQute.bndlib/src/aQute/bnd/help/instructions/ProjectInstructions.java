package aQute.bnd.help.instructions;

import java.util.Map;
import java.util.Optional;

import aQute.bnd.help.SyntaxAnnotation;
import aQute.bnd.osgi.Constants;

public interface ProjectInstructions {

	interface StaleTest {
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
}
