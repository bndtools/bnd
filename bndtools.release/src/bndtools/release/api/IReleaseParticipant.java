/*******************************************************************************
 * Copyright (c) 2010 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package bndtools.release.api;

import aQute.bnd.osgi.Jar;

/**
 * IReleaseParticipant contains methods for extending the release process
 * 
 * The methods are called in the following order:
 * 1. preUpdateProjectVersions 
 * 2. preRelease
 * 3. preJarRelease
 * 4. postJarRelease
 * 5. postRelease
 */
public interface IReleaseParticipant {

	public enum Scope {
		PRE_UPDATE_VERSIONS,
		PRE_RELEASE,
		PRE_JAR_RELEASE,
		POST_JAR_RELEASE,
		POST_RELEASE
	}
	
	/**
	 * Gives a possibility to rank the registered IReleaseParticipants, are set in plugin.xml
	 */
	void setRanking(int ranking);
	
	/**
	 * @return the ranking
	 */
	int getRanking();

	/**
	 * Called just before the the '*.bnd' and 'packageinfo' files in the project are updated. 
	 * Can be used to override the suggested versions in JarDiff's
	 * @param context
	 * @return if false, the release is canceled and postRelease is called with success = false
	 */
	boolean preUpdateProjectVersions(ReleaseContext context);

	/**
	 * Called just before the build operation
	 * @param context
	 * @return if false, the release is canceled and postRelease is called with success = false
	 */
	boolean preRelease(ReleaseContext context);
	
	/**
	 * Called after successful build and prior release to the specified RepsoitoryPlugin
	 * @param context
	 * @param jar
	 * @return if false, the release is canceled and postRelease is called with success = false
	 */
	boolean preJarRelease(ReleaseContext context, Jar jar);
	
	/**
	 * Called after release to the specified RepsoitoryPlugin
	 * @param context
	 * @param jar
	 */
	void postJarRelease(ReleaseContext context, Jar jar);

	/**
	 * Always called at the end of the release
	 * @param context
	 * @param success All Jar's released successfully
	 */
	void postRelease(ReleaseContext context, boolean success);

}
