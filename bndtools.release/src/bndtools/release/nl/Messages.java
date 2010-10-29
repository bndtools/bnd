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
package bndtools.release.nl;

import org.eclipse.osgi.util.NLS;


public class Messages extends NLS {

	private final static String RESOURCE_BUNDLE = Messages.class.getName();
	
	public static String version;
	public static String newVersion;
	public static String versionRange;
	public static String newRange;
	public static String showAllPackages;
	public static String symbNamePackage;
	public static String releaseToRepo;
	public static String cleaningProject;
	public static String releasing;
	public static String checkingExported;
	public static String releaseJob;
	public static String project;

	static {
		NLS.initializeMessages(RESOURCE_BUNDLE, Messages.class);
	}
}

