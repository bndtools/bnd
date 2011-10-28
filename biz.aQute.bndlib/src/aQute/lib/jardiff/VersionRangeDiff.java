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

import aQute.libg.version.*;

public interface VersionRangeDiff {

	VersionRange getOldVersionRange();

	VersionRange getSuggestedVersionRange();
	
	void setNewVersionRange(VersionRange version);

	VersionRange getNewVersionRange();
}
