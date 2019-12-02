package aQute.bnd.help.instructions;

import org.osgi.annotation.versioning.ProviderType;

import aQute.bnd.help.SyntaxAnnotation;

/**
 * Instructions on the Resolution.
 */

@ProviderType
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
		 * Order the -runbundles sorted by name and merged with the existing
		 * value if it exists. This is the default since it was the classic
		 * behavior.
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

	enum ResolveMode {
		/**
		 * Resolve is done manually. This is the default
		 */
		@SyntaxAnnotation(lead = "Resolve is manually with the Resolve button")
		manual,
		/**
		 * Run the resolver automatically yon save
		 */
		@SyntaxAnnotation(lead = "A resolve will take place before saving")
		auto,
		/**
		 * Run the resolver before launching.
		 */
		@SyntaxAnnotation(lead = "A resolve will take place before launching")
		beforelaunch,

	}

	@SyntaxAnnotation(lead = "Resolve mode defines when resolving takes place. The default, manual, requires a manual step in bndtools. Auto will resolve on save, and beforelaunch runs the resolver before being launched", example = "'-resolve manual", pattern = "(manual|auto|beforelaunch)")
	ResolveMode resolve();
}
