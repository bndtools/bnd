package name.neilbartlett.eclipse.bndtools.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import aQute.libg.version.Version;
import aQute.libg.version.VersionRange;

public class BundleUtils {
	public static final Bundle findBundle(String symbolicName, VersionRange range) {
		Bundle matched = null;
		Version matchedVersion = null;
		Bundle[] bundles = Plugin.getDefault().getBundleContext().getBundles();
		for (Bundle bundle : bundles) {
			try {
				String name = bundle.getSymbolicName();
				String versionStr = (String) bundle.getHeaders().get(Constants.BUNDLE_VERSION);
				Version version = versionStr != null ? new Version(versionStr) : new Version();
				if(symbolicName.equals(name)) {
					if(matched == null || version.compareTo(matchedVersion) > 0) {
						matched = bundle;
						matchedVersion = version;
					}
				}
			} catch (Exception e) {
			}
		}
		return matched;
	}
	public static IPath getBundleLocation(String symbolicName, VersionRange range) {
		Bundle bundle= findBundle(symbolicName, range);
		String location = bundle.getLocation();
		if(location.startsWith("file:")) { //$NON-NLS-1$
			location = location.substring(5);
		} else if(location.startsWith("reference:file:")) { //$NON-NLS-1$
			location = location.substring(15);
		}
		return Path.fromOSString(location);
	}
}
