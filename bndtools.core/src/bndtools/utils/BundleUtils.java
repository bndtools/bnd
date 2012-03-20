/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package bndtools.utils;


import java.io.File;
import java.util.jar.Attributes;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import aQute.libg.header.Parameters;
import aQute.libg.version.Version;
import aQute.libg.version.VersionRange;

public class BundleUtils {
	private static final String FILE_URL_PREFIX = "file:";

    public static final Bundle findBundle(BundleContext context, String symbolicName, VersionRange range) {
		Bundle matched = null;
		Version matchedVersion = null;
		Bundle[] bundles = context.getBundles();
		for (Bundle bundle : bundles) {
			try {
				String name = bundle.getSymbolicName();
				String versionStr = (String) bundle.getHeaders().get(Constants.BUNDLE_VERSION);
				Version version = versionStr != null ? new Version(versionStr) : new Version();
				if(range == null || range.includes(version)) {
					if(symbolicName.equals(name)) {
						if(matched == null || version.compareTo(matchedVersion) > 0) {
							matched = bundle;
							matchedVersion = version;
						}
					}
				}
			} catch (Exception e) {
			}
		}
		return matched;
	}
	public static IPath getBundleLocation(BundleContext context, String symbolicName, VersionRange range) {
		Location installLocation = Platform.getInstallLocation();
		Location configLocation = Platform.getConfigurationLocation();

		Bundle bundle= findBundle(context, symbolicName, range);
		if(bundle == null)
			return null;

		String location = bundle.getLocation();
		if(location.startsWith("file:")) { //$NON-NLS-1$
			location = location.substring(5);
		} else if(location.startsWith("reference:file:")) { //$NON-NLS-1$
			location = location.substring(15);
		}
		IPath bundlePath = new Path(location);
		if(bundlePath.isAbsolute())
			return bundlePath;

		// Try install location
		if(installLocation != null) {
		IPath installedBundlePath = new Path(installLocation.getURL().getFile()).append(bundlePath);
		if(installedBundlePath.toFile().exists())
			return installedBundlePath;
		}

		// Try config location
		if(configLocation != null) {
		IPath configuredBundlePath = new Path(configLocation.getURL().getFile()).append(bundlePath);
		if(configuredBundlePath.toFile().exists())
			return configuredBundlePath;
		}

		return null;
	}

    /**
     * Try to get the last modified time for the bundle, based on the modified
     * time of the file itself if the bundle was installed from a file. If the
     * bundle was not installed from a file (e.g. it may have been streamed from
     * the network or some other device) then use the time that the bundle was
     * last installed or updated in the OSGi framework.
     *
     * @param bundle
     *            The bundle
     * @return The last modified time of the bundle.
     */
	public static long getBundleLastModified(Bundle bundle) {
	    long result;
        String location = bundle.getLocation();
        if(location != null && location.startsWith(FILE_URL_PREFIX)) {
            File bundleFile = new File(location.substring(FILE_URL_PREFIX.length()));
            result = bundleFile.lastModified();
        } else {
            result = bundle.getLastModified();
        }
        return result;
	}

	public static String getBundleSymbolicName(Attributes attribs) {
	    Parameters header = new Parameters(attribs.getValue(Constants.BUNDLE_SYMBOLICNAME));
	    if(header == null || header.size() != 1)
	        return null;

	    return header.keySet().iterator().next();
	}
}
