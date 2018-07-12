package aQute.bnd.help.instructions;

import java.util.Optional;

import aQute.bnd.help.SyntaxAnnotation;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

/**
 * Instructions for the Builder
 */
public interface BuilderInstructions {

	@SyntaxAnnotation(name = Constants.COMPRESSION, lead = "Compression to use for outputed JARs. The default is DEFLATE", example = "STORE")
	Optional<Jar.Compression> compression();
}
