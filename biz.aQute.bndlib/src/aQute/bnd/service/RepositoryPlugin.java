package aQute.bnd.service;

import java.io.*;
import java.util.*;

import aQute.lib.osgi.*;
import aQute.libg.version.*;

public interface RepositoryPlugin {
	public enum Strategy {
		LOWEST, HIGHEST, EXACT
	}

	/**
	 * Return a URL to a matching version of the given bundle.
	 * 
	 * @param bsn
	 *            Bundle-SymbolicName of the searched bundle
	 * @param range
	 *            Version range for this bundle,"latest" if you only want the
	 *            latest, or null when you want all.
	 * @return A list of URLs sorted on version, lowest version is at index 0.
	 *         null is returned when no files with the given bsn ould be found.
	 * @throws Exception
	 *             when anything goes wrong
	 */
	@Deprecated
	File[] get(String bsn, String range) throws Exception;

	/**
	 * Return a URL to a matching version of the given bundle.
	 * 
	 * @param bsn
	 *            Bundle-SymbolicName of the searched bundle
	 * @param range
	 *            Version range for this bundle,"latest" if you only want the
	 *            latest, or null when you want all.
	 * @param strategy
	 *            Get the highest or the lowest
	 * @return A list of URLs sorted on version, lowest version is at index 0.
	 *         null is returned when no files with the given bsn ould be found.
	 * @throws Exception
	 *             when anything goes wrong
	 */
	File get(String bsn, String range, Strategy strategy, Map<String,String> properties) throws Exception;

	/**
	 * Answer if this repository can be used to store files.
	 * 
	 * @return true if writable
	 */
	boolean canWrite();

	/**
	 * Put a JAR file in the repository.
	 * 
	 * @param jar
	 * @throws Exception
	 */
	File put(Jar jar) throws Exception;

	/**
	 * Return a list of bsns that are present in the repository.
	 * 
	 * @param regex
	 *            if not null, match against the bsn and if matches, return
	 *            otherwise skip
	 * @return A list of bsns that match the regex parameter or all if regex is
	 *         null
	 * @throws Exception
	 */
	List<String> list(String regex) throws Exception;

	/**
	 * Return a list of versions.
	 * 
	 * @throws Exception
	 */

	List<Version> versions(String bsn) throws Exception;

	/**
	 * @return The name of the repository
	 */
	String getName();

	/**
	 * Return a location identifier of this repository
	 */

	String getLocation();
}
