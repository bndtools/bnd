package aQute.bnd.eclipse;

import aQute.bnd.build.Project;
import aQute.bnd.build.BuildFacet;

/**
 * Functions that handle Eclipse projects based on bnd
 */
public class LibEclipse {

	/**
	 * Force the .classpath to be the same as the workspace settings
	 * 
	 * @param project the project to do this for
	 */
	public static void updateClasspath(Project project) {
		BuildFacet[] sets = BuildFacet.getBuildFacets(project);

	}
}
