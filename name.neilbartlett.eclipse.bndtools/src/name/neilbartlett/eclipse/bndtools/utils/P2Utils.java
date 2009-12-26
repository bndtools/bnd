package name.neilbartlett.eclipse.bndtools.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.ServiceOperation;

import org.osgi.framework.Version;

import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.resolver.VersionRange;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;

import aQute.bnd.plugin.Activator;

/**
 * Utilities to read and write bundle and source information files.
 * <p>
 * This class currently uses provisional p2 API for which official API has been requested:
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=269496
 * </p>
 * 
 * @since 3.5
 */
public class P2Utils {

	private static final String CONFIG_FOLDER = "configuration"; //$NON-NLS-1$
	
	private static final String SRC_INFO_FOLDER = "org.eclipse.equinox.source"; //$NON-NLS-1$
	private static final String SRC_INFO_PATH= SRC_INFO_FOLDER + File.separator + "source.info"; //$NON-NLS-1$

	private static final String BUNDLE_INFO_FOLDER= "org.eclipse.equinox.simpleconfigurator"; //$NON-NLS-1$
	private static final String BUNDLE_INFO_PATH = BUNDLE_INFO_FOLDER + File.separator + "bundles.info"; //$NON-NLS-1$


	/**
	 * Returns bundles defined by the 'bundles.info'.
	 * 
	 * @param isSourceBundle <code>true</code> if source bundles should be read <code>false</code>
	 *            otherwise
	 * @param useConfigArea <code>true</code> if the bundles are read from the config area, or
	 *            <code>false</code> if the install location should be used
	 * @return all bundles in the installation or <code>null</code> if not able to locate a
	 *         bundles.info
	 */
	private static BundleInfo[] readBundles(boolean isSourceBundle, boolean useConfigArea) {
		final Location bundlesLocation;
		final String bundleInfoPath;
		if (useConfigArea) {
			bundlesLocation= Platform.getConfigurationLocation();
			if (isSourceBundle)
				bundleInfoPath= SRC_INFO_PATH;
			else
				bundleInfoPath= BUNDLE_INFO_PATH;
		} else {
			bundlesLocation= Platform.getInstallLocation();
			if (isSourceBundle)
				bundleInfoPath= CONFIG_FOLDER + File.separator + SRC_INFO_PATH;
			else
				bundleInfoPath= CONFIG_FOLDER + File.separator + BUNDLE_INFO_PATH;
		}
		if (bundlesLocation == null)
			return null;

		URL bundlesLocationURL= bundlesLocation.getURL();
		if (bundleInfoPath == null)
			return null;

		try {
			URL bundlesTxt= new URL(bundlesLocationURL.getProtocol(), bundlesLocationURL.getHost(), new File(bundlesLocationURL.getFile(), bundleInfoPath).getAbsolutePath());
			BundleInfo bundles[]= getBundlesFromFile(bundlesLocationURL, bundlesTxt);
			if (bundles == null || bundles.length == 0) {
				return null;
			}
			return bundles;
		} catch (IOException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading bundles location", e));
			return null;
		}
	}

	/**
	 * Returns an array of {@link BundleInfo} for each bundle entry or <code>null</code> if there is a
	 * problem reading the file.
	 * 
	 * @param bundlesLocation the URL of the bundle location
	 * @param fileURL the URL of the info file
	 * @return array of bundle infos or <code>null</code>
	 * @throws IOException if loading the configuration fails
	 */
	private static BundleInfo[] getBundlesFromFile(URL bundlesLocation, final URL fileURL) throws IOException {
		final File home= new File(bundlesLocation.getFile());
		
		return Plugin.usingService(SimpleConfiguratorManipulator.class, new ServiceOperation<BundleInfo[], SimpleConfiguratorManipulator, IOException>() {
			public BundleInfo[] execute(SimpleConfiguratorManipulator service) throws IOException {
				return service.loadConfiguration(fileURL, home);
			}
		});
	}

	/**
	 * Finds the bundle info for the given arguments.
	 * <p>
	 * The first match will be returned if more than one bundle matches the arguments.
	 * </p>
	 * 
	 * @param symbolicName the symbolic name
	 * @param version the bundle version
	 * @param isSourceBundle <code>true</code> if it is a source bundle <code>false</code> otherwise
	 * @return the bundle info or <code>null</code> if not found
	 */
	public static BundleInfo findBundle(String symbolicName, Version version, boolean isSourceBundle) {
		Assert.isLegal(symbolicName != null);
		Assert.isLegal(version != null);

		return findBundle(symbolicName, new VersionRange(version, true, version, true), isSourceBundle);
	}

	/**
	 * Finds the bundle info for the given arguments.
	 * <p>
	 * The first match will be returned if more than one bundle matches the arguments.
	 * </p>
	 * 
	 * @param symbolicName the symbolic name
	 * @param versionRange the version range for the bundle version
	 * @param isSourceBundle <code>true</code> if it is a source bundle <code>false</code> otherwise
	 * @return the bundle info or <code>null</code> if not found
	 */
	public static BundleInfo findBundle(String symbolicName, VersionRange versionRange, boolean isSourceBundle) {
		Assert.isLegal(symbolicName != null);
		Assert.isLegal(versionRange != null);

		// First try to find the bundle in the config area
		BundleInfo bundleInfo= findBundle(symbolicName, versionRange, isSourceBundle, true);
		if (bundleInfo != null)
			return bundleInfo;

		// Use install location if not found in config area
		return findBundle(symbolicName, versionRange, isSourceBundle, false);
	}

	/**
	 * Finds the bundle info for the given arguments.
	 * 
	 * @param symbolicName the symbolic name
	 * @param versionRange the version range for the bundle version
	 * @param isSourceBundle <code>true</code> if source bundles should be read <code>false</code>
	 *            otherwise
	 * @param useConfigArea <code>true</code> if the bundles are read from the config area, or
	 *            <code>false</code> if the install location should be used
	 * @return the bundle info or <code>null</code> if not found
	 * @since 3.5
	 */
	private static BundleInfo findBundle(String symbolicName, VersionRange versionRange, boolean isSourceBundle, boolean useConfigArea) {
		BundleInfo[] bundles= readBundles(isSourceBundle, useConfigArea);
		if (bundles == null)
			return null;
		
		for (int i= 0; i < bundles.length; i++) {
			if (symbolicName.equals(bundles[i].getSymbolicName()) && versionRange.isIncluded(new Version(bundles[i].getVersion()))) {
				IPath path= getBundleLocationPath(bundles[i]);
				if (path.toFile().exists())
					return bundles[i];
			}
		}
		
		return null;
	}

	/**
	 * Returns the bundle location path.
	 * 
	 * @param bundleInfo the bundle info or <code>null</code>
	 * @return the bundle location or <code>null</code> if it is not possible to convert to a path
	 */
	public static IPath getBundleLocationPath(BundleInfo bundleInfo) {
		if (bundleInfo == null)
			return null;
	
		URI bundleLocation= bundleInfo.getLocation();
		if (bundleLocation == null)
			return null;
		
		try {
			String fileStr= FileLocator.toFileURL(URIUtil.toURL(bundleLocation)).getFile();
			fileStr= URLDecoder.decode(fileStr, "UTF-8"); //$NON-NLS-1$
			return new Path(fileStr);
		} catch (IOException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error getting bundle location path.", e));
			return null;
		}
	}

}
