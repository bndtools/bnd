package aQute.bnd.help.instructions;

import java.util.Optional;

import aQute.bnd.help.SyntaxAnnotation;

/**
 * Instructions on the Resolution.
 */
public interface ResolutionInstructions {

	/**
	 * Specifies the values of the -runorder instruction
	 */

	enum Runorder {
	/**
	 * Order the -runbundles by having the least dependent first.
	 */
	LEASTDEPENDENCIESFIRST,
	/**
	 * Order the -runbundles by having the least dependent last.
	 */
	LEASTDEPENDENCIESLAST,
	/**
	 * Order the -runbundles randomly using the Collections#shuffle.
	 */
	RANDOM,
	/**
	 * Order the -runbundles sorted by name.
	 */
	SORTBYNAMEVERSION,
	/**
	 * Order the -runbundles sorted by name and merged with the existing value
	 * if it exists. This is the default since it was the classic behavior.
	 */
	MERGESORTBYNAMEVERSION {
		public boolean isMerge() {
			return true;
		}
	};

		public boolean isMerge() {
			return false;
		}
	}

	@SyntaxAnnotation(lead = "Specify the runorder of the resolved bundles", example = "'-runorder leastdependentfirst")
	Optional<Runorder> runorder();

}
