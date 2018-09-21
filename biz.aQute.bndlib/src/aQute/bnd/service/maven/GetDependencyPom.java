package aQute.bnd.service.maven;

import aQute.bnd.version.Version;

public interface GetDependencyPom {
	/**
	 * Searches a pom dependency for the supplied bundle identifier and version
	 * 
	 * @param bsn Bundle-SymbolicName of the searched bundle
	 * @param version Version requested
	 * @return A PomDependency for the bundle identifier and version or null if
	 *         not found
	 * @throws Exception when anything goes wrong
	 */
	PomDependency getPomDependency(String bsn, Version version) throws Exception;
}
