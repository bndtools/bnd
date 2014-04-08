package org.bndtools.api;

import java.io.File;
import java.io.IOException;

import aQute.bnd.annotation.ConsumerType;

/**
 * <p>
 * The interface of a headless build plugin.
 * </p>
 * <p>
 * A headless build plugin can generate files in the cnf project and all bnd
 * projects so that they can be built using the corresponding headless build
 * tooling.
 * </p>
 */
@ConsumerType
public interface HeadlessBuildPlugin {
	/**
	 * @return generic plugin information
	 */
	NamedPlugin getInformation();

	/**
	 * Setup/remove files enabling headless build of a project.
	 * 
	 * @param cnf
	 *            true when the project directory is that of the cnf project
	 * @param projectDir
	 *            the project directory
	 * @param add
	 *            true to add/create the files, false to remove them
	 * @throws IOException
	 *             upon error(s)
	 */
	void setup(boolean cnf, File projectDir, boolean add) throws IOException;

	// Future expansion of the interface: automatic management of template files
	//
	// public enum HeadlessBuildFileStatus {
	// 	NONE, UPDATE, ADD, REMOVE;
	// }
	//
	// public interface HeadlessBuildFileInfo {
	// 	String getBuildFile();
	//
	// 	File getFile();
	//
	// 	HeadlessBuildFileStatus getFileStatus();
	// }
	//
	// List<HeadlessBuildFileInfo> check(boolean cnf, File projectDir) throws IOException;
	//
	// InputStream getBuildFile(boolean cnf, File projectDir, HeadlessBuildFileInfo fileInfo) throws IOException;
	//
	// void update(boolean cnf, File projectDir, List<HeadlessBuildFileInfo> fileInfos) throws IOException;
}