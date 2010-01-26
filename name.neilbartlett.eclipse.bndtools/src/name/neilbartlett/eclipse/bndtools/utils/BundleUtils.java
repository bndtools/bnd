package name.neilbartlett.eclipse.bndtools.utils;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
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
		IPath installPath = new Path(Platform.getInstallLocation().getURL().getFile());
		IPath configPath = new Path(Platform.getConfigurationLocation().getURL().getFile());
		
		Bundle bundle= findBundle(symbolicName, range);
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
		IPath installedBundlePath = installPath.append(bundlePath);
		if(installedBundlePath.toFile().exists())
			return installedBundlePath;
		
		// Try config location
		IPath configuredBundlePath = configPath.append(bundlePath);
		if(configuredBundlePath.toFile().exists())
			return configuredBundlePath;
		
		return null;
	}
}
