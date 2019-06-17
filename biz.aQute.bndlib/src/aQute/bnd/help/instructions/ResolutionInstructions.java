package aQute.bnd.help.instructions;

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
	MERGESORTBYNAMEVERSION;
	}

	@interface RunStartLevel {
		Runorder order() default Runorder.MERGESORTBYNAMEVERSION;

		int begin() default 100;

		int step() default 10;
	}

	@SyntaxAnnotation(lead = "Specify the runorder and startlevel behavior of the resolved bundles", example = "'-runstartlevel order=leastdependenciesfirst, begin=1, step=1")
	RunStartLevel runstartlevel(RunStartLevel deflt);

}
