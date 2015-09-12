package aQute.bnd.build;

public enum WorkspaceLayout {
	/**
	 * The classic layout of a bnd workspace, consisting of a parent directory
	 * with a cnf folder.
	 */
	BND

	,

	/**
	 * A standalone workspace based on a single bnd or bndrun file, which must
	 * contain the -standalone instruction.
	 */
	STANDALONE
}
