package aQute.bnd.help.instructions;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.osgi.annotation.versioning.ProviderType;

import aQute.bnd.help.SyntaxAnnotation;
import aQute.bnd.osgi.Jar;

/**
 * Instructions for the launcher
 */
@ProviderType
public interface LauncherInstructions {

	@ProviderType
	interface Executable {
		@SyntaxAnnotation(lead = "Re-jar the -runpath and -runbundles to the given compression. If "
			+ "not set, bundles are not touched. This should not change the signatures", example = "rejar=STORE")
		Optional<Jar.Compression> rejar();

		@SyntaxAnnotation(lead = "Strips files from embedded JARs. The syntax JARPATHMATCH ':' RESOURCEPATHMATCH, both globs.", example = "*:OSGI-OPT/*")
		List<String> strip();

		/**
		 * By default, the name inside the executable JAR is based on the file
		 * name in the repository. This name is also used as the location by by
		 * the launcher. If the environment is not cleaned at startup, this can
		 * cause problems since a change in this name install the same bundle
		 * under two different locations. This will create a horrible conflict
		 * during install that is hard to recover from.
		 * <p>
		 * This configuration allows you to calculate the location from the bsn
		 * and version.
		 *
		 * @return a pattern or null
		 */
		@SyntaxAnnotation(lead = "A pattern to form the location for the bundle. This pattern "
			+ "is processed by the macro processor. @bsn is the bsn, and @version is "
			+ "the version, and @name is the file name. If no pattern is given, the file name is used to make a "
			+ "unique name in the /jar directory. "
			+ "If multiple bundles end up with the same name then the last one wins.The expansion may not contain file separators like /."
			+ "If the storage area is not cleaned, use the example pattern", example = "location='${@bsn}-${version;=;${@version}}.jar'")
		String location();

	}

	@SyntaxAnnotation(lead = "Options for the export of an executable", example = "rejar=STORE,strip=*:OSGI-OPT/*")
	Executable executable();

	enum RunOption {
		eager
	}

	@SyntaxAnnotation(lead = "Options for the launch", example = "-runoptions eager")
	Set<RunOption> runoptions();
}
