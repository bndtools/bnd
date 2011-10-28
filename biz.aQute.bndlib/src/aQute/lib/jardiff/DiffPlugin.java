/*******************************************************************************
 * Copyright (c) 2011 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package aQute.lib.jardiff;

import java.util.*;

import aQute.lib.osgi.*;

public interface DiffPlugin {

	boolean canDiff(String name);
	
	Collection<Diff> diff(Diff container, String name, Resource newResource, Resource oldResource) throws Exception;
}
