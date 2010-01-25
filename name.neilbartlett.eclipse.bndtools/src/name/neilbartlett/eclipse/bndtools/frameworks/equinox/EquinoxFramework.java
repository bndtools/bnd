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
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.utils.BundleUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.Bundle;

import aQute.libg.version.VersionRange;

public class EquinoxFramework implements IFramework {

	public IFrameworkInstance createFrameworkInstance(File resource) throws CoreException {
		return new EquinoxInstance(new Path(resource.getAbsolutePath()));
	}

	public Collection<File> getAutoConfiguredLocations() {
		List<File> result = new LinkedList<File>();
		
		Bundle systemBundle = Platform.getBundle("org.eclipse.osgi");

		@SuppressWarnings("restriction")
		BaseData bundleData = (BaseData) ((AbstractBundle) systemBundle).getBundleData();
		@SuppressWarnings("restriction")
		BundleFile bundleFile = bundleData.getBundleFile();
		
		result.add(bundleFile.getBaseFile());
		
		/*
		IPath bundleLoc = BundleUtils.getBundleLocation("org.eclipse.osgi", new VersionRange("3"));
		if(bundleLoc != null) {
			result.add(bundleLoc.toFile());
		}
		BundleInfo bundleInfo = P2Utils.findBundle("org.eclipse.osgi", new VersionRange(new Version(3,0,0), true, null, false), false);
		if(bundleInfo != null) {
			IPath path = P2Utils.getBundleLocationPath(bundleInfo);
			if(path != null) {
				result.add(path.toFile());
			}
		}
		*/
		return result;
	}
}
