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
package name.neilbartlett.eclipse.bndtools.frameworks.equinox;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.utils.P2Utils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class EquinoxFramework implements IFramework {

	public IFrameworkInstance createFrameworkInstance(File resource) throws CoreException {
		return new EquinoxInstance(new Path(resource.getAbsolutePath()));
	}

	public Collection<File> getAutoConfiguredLocations() {
		List<File> result = new LinkedList<File>();
		
		BundleInfo bundleInfo = P2Utils.findBundle("org.eclipse.osgi", new VersionRange(new Version(3,0,0), true, null, false), false);
		if(bundleInfo != null) {
			IPath path = P2Utils.getBundleLocationPath(bundleInfo);
			if(path != null) {
				result.add(path.toFile());
			}
		}
		return result;
	}
}
