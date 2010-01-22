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
package name.neilbartlett.eclipse.bndtools.frameworks.felix;

import java.io.File;
import java.util.Collection;

import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

public class FelixFramework implements IFramework {

	public IFrameworkInstance createFrameworkInstance(File resource)
			throws CoreException {
		return new FelixInstance(new Path(resource.getAbsolutePath()));
	}

	public Collection<File> getAutoConfiguredLocations() {
		// Not supported
		return null;
	}

}
