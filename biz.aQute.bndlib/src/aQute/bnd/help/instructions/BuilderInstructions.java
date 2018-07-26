package aQute.bnd.help.instructions;

import java.util.Optional;

import aQute.bnd.header.Parameters;
import aQute.bnd.help.SyntaxAnnotation;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

/**
 * Instructions for the Builder
 */
public interface BuilderInstructions {

	@SyntaxAnnotation(name = Constants.COMPRESSION, lead = "Compression to use for outputed JARs. The default is DEFLATE", example = "STORE")
	Optional<Jar.Compression> compression();

	@SyntaxAnnotation(name = Constants.INCLUDEPACKAGE, lead = "Include packages from the class path that are in limbo. They are not forced private and can be exported by the analyzer, for example the @Export annotation. The syntax is identical to the Private-Package", example = "-includepackage com.example.foo.*")
	Parameters includepackage();

	@SyntaxAnnotation(name = Constants.UNDERTEST)
	boolean undertest();
}
