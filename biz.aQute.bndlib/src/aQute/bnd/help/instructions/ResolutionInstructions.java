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
		@SyntaxAnnotation(lead = "Order the -runbundles by having the least dependent first.")
		LEASTDEPENDENCIESFIRST,
		/**
		 * Order the -runbundles by having the least dependent last.
		 */
		@SyntaxAnnotation(lead = "Order the -runbundles by having the least dependent last.")
		LEASTDEPENDENCIESLAST,
		/**
		 * Order the -runbundles randomly using the Collections#shuffle.
		 */
		@SyntaxAnnotation(lead = "Order the -runbundles randomly using the Collections#shuffle.")
		RANDOM,
		/**
		 * Order the -runbundles sorted by name.
		 */
		@SyntaxAnnotation(lead = "Order the -runbundles sorted by name.")
		SORTBYNAMEVERSION,
		/**
		 * Order the -runbundles sorted by name and merged with the existing
		 * value if it exists. This is the default since it was the classic
		 * behavior.
		 */
		@SyntaxAnnotation(lead = "Order the -runbundles sorted by name and merged with the existing value if it exists. This is the default since it was the classic behavior.")
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
		 * Resolve is done manually. This is the default. The -runbundles list
		 * is managed by the user and is never modified automatically.
		 */
		@SyntaxAnnotation(lead = "Bundles are resolved manually using the Resolve button. The -runbundles list is never modified automatically.")
		manual,
		/**
		 * Run the resolver automatically on every save. The entire -runbundles
		 * list is replaced by the resolver output on each save. Any manual
		 * edits to -runbundles will be silently overwritten.
		 */
		@SyntaxAnnotation(lead = "The resolver runs automatically on every save, replacing -runbundles with the resolved result. Manual edits to -runbundles will be overwritten.")
		auto,
		/**
		 * Run the resolver automatically before each launch. The -runbundles
		 * list is updated with the resolved result before the application is
		 * started.
		 */
		@SyntaxAnnotation(lead = "The resolver runs automatically before each launch, updating -runbundles with the resolved result.")
		beforelaunch,

		/**
		 * Run the resolver before launching, but only in batch mode (e.g.
		 * Gradle). In IDE mode (e.g. Eclipse) the -runbundles list is not
		 * touched and must be managed manually or via an explicit resolve.
		 */
		@SyntaxAnnotation(lead = "The resolver runs before launching in batch mode (e.g. Gradle) but not in IDE mode (e.g. Eclipse). In IDE mode the -runbundles list must be managed manually.")
		batch,

		/**
		 * Run the resolver when -runbundles are needed, unless a cache file
		 * exists that is newer than the bndrun file, project, and workspace.
		 * The cache file has the same name as the project/bndrun file but
		 * starts with a '.'.
		 */
		@SyntaxAnnotation(lead = "The resolver runs when -runbundles are needed, unless a cache file (same name prefixed with '.') is newer than the project and workspace.")
		cache,

		/**
		 * Never run the resolver automatically. The -runbundles list must be
		 * managed entirely by the user. Attempting to resolve manually will
		 * result in an error.
		 */
		@SyntaxAnnotation(lead = "Resolving never takes place automatically. The -runbundles list must be managed manually. Attempting to resolve manually will result in an error.")
		never
	}

	@SyntaxAnnotation(lead = "Resolve mode defines when resolving takes place. The default, manual, requires a manual step in bndtools. Auto will resolve on save, and beforelaunch runs the resolver before being launched, batchlaunch is like beforelaunch but only in batch mode", example = "'-resolve manual", pattern = "(manual|auto|beforelaunch|batch)", helpurl = "https://bnd.bndtools.org/instructions/resolve.html")
	ResolveMode resolve();
}
