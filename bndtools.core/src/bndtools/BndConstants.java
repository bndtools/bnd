/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools;

import aQute.bnd.osgi.Constants;

public interface BndConstants extends Constants {

	String	OUTPUT						= "-output";
	String	RUNFW						= "-runfw";
	String	BACKUP_RUNBUNDLES			= "-runbundles-old";

	/**
	 * The URI to which a resource was resolved by OBR
	 */
	String	RESOLUTION_URI_ATTRIBUTE	= "resolution";
	String	RESOLVE_MODE				= "-resolve";
}
