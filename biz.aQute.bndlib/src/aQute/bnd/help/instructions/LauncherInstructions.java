package aQute.bnd.help.instructions;

import java.util.List;
import java.util.Optional;

import aQute.bnd.help.SyntaxAnnotation;
import aQute.bnd.osgi.Jar;

/**
 * Instructions for the launcher
 */
public interface LauncherInstructions {

	interface Executable {
		@SyntaxAnnotation(lead = "Re-jar the -runpath and -runbundles to the given compression. If "
			+ "not set, bundles are not touched. This should not change the signatures", example = "STORE")
		Optional<Jar.Compression> rejar();

		@SyntaxAnnotation(lead = "Strip OSGI-OPT from all jars. Default is to not strip", example = "true")
		List<String> strip();
	}

	@SyntaxAnnotation(lead = "Options for the export of an executable", example = "rejar=STORE")
	Executable executable();
}
