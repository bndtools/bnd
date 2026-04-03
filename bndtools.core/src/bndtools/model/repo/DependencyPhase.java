package bndtools.model.repo;

/**
 * This enum is used to describe the phase in which a dependency (e.g.
 * repobundle, repobundleversion) is used. This is usually used to distinguish
 * between wether a dependecy is added to -runrequires / -runbundles of a
 * .bndrun or -buildpath of a bnd.bnd file.
 */
public enum DependencyPhase {
	/**
	 * e.g. adding a bundle / bundleversion to the -runrequires section of a
	 * bnd.bnd file
	 */
	Req,
	/**
	 * e.g. adding a bundle / bundleversion to the -buildpath section of a
	 * bnd.bnd file
	 */
	Build,
	/**
	 * e.g. adding a bundle / bundleversion to -runbundles section of a .bndrun
	 * file
	 */
	Run
}
