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

		@SyntaxAnnotation(lead = "Ordering of -runbundles of the resolved bundles", example = "'order=leastdependencieslast")
		Runorder order() default Runorder.MERGESORTBYNAMEVERSION;

		@SyntaxAnnotation(lead = "Beginning automatic startlevel calculation,  -1 indicates no automatic calculation. When bdn calculates the startlevel, this will be the first assigned startlevel", example = "'begin=10")
		int begin() default 100;

		@SyntaxAnnotation(lead = "Start level step for each next bundle. Startlevel is 0 when < 1", example = "'begin=1", pattern = "\\d+")
		int step() default 10;
	}

	@SyntaxAnnotation(lead = "Specify the runorder and startlevel behavior of the resolved bundles", example = "'-runstartlevel order=leastdependenciesfirst, begin=1, step=1")
	RunStartLevel runstartlevel(RunStartLevel deflt);

}
