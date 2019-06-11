package biz.aQute.bnd.reporter.plugins.entries.bundle;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;
import biz.aQute.bnd.reporter.helpers.HeadersHelper;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import biz.aQute.bnd.reporter.manifest.dto.OSGiHeadersDTO;

/**
 * This plugin allows to add some of the bundle manifest headers to the report.
 */
@BndPlugin(name = "entry." + EntryNamesReference.MANIFEST)
public class ManifestPlugin implements ReportEntryPlugin<Jar>, Plugin {

	private final Map<String, String> _properties = new HashMap<>();

	public ManifestPlugin() {
		_properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.MANIFEST);
		_properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, Jar.class.getCanonicalName());
	}

	@Override
	public OSGiHeadersDTO extract(final Jar jar, final Locale locale) {
		Objects.requireNonNull(jar, "jar");
		Objects.requireNonNull(locale, "locale");

		final ManifestHelper manifest = ManifestHelper.createIfPresent(jar, locale);
		if (manifest != null) {
			final OSGiHeadersDTO headers = new OSGiHeadersDTO();

			headers.bundleName = HeadersHelper.convertBundleName(manifest.getHeader(Constants.BUNDLE_NAME, false));
			headers.bundleDescription = HeadersHelper
				.convertBundleDescription(manifest.getHeader(Constants.BUNDLE_DESCRIPTION, false));
			headers.bundleVersion = HeadersHelper
				.convertBundleVersion(manifest.getHeader(Constants.BUNDLE_VERSION, false));
			headers.bundleCategories = HeadersHelper
				.convertBundleCategories(manifest.getHeader(Constants.BUNDLE_CATEGORY, false));
			headers.bundleIcons = HeadersHelper.convertBundleIcons(manifest.getHeader(Constants.BUNDLE_ICON, false));
			headers.bundleDocURL = HeadersHelper
				.convertBundleDocURL(manifest.getHeader(Constants.BUNDLE_DOCURL, false));
			headers.bundleUpdateLocation = HeadersHelper
				.convertBundleUpdateLocation(manifest.getHeader(Constants.BUNDLE_UPDATELOCATION, false));
			headers.bundleLocalization = HeadersHelper
				.convertBundleLocalization(manifest.getHeader(Constants.BUNDLE_LOCALIZATION, false));
			headers.bundleLicenses = HeadersHelper
				.convertBundleLicenses(manifest.getHeader(Constants.BUNDLE_LICENSE, false));
			headers.bundleDevelopers = HeadersHelper
				.convertBundleDevelopers(manifest.getHeader(Constants.BUNDLE_DEVELOPERS, false));
			headers.bundleSCM = HeadersHelper.convertBundleSCM(manifest.getHeader(Constants.BUNDLE_SCM, false));
			headers.bundleCopyright = HeadersHelper
				.convertBundleCopyright(manifest.getHeader(Constants.BUNDLE_COPYRIGHT, false));
			headers.bundleVendor = HeadersHelper
				.convertBundleVendor(manifest.getHeader(Constants.BUNDLE_VENDOR, false));
			headers.bundleContactAddress = HeadersHelper
				.convertBundleContactAddress(manifest.getHeader(Constants.BUNDLE_CONTACTADDRESS, false));
			headers.bundleSymbolicName = HeadersHelper
				.convertBundleSymbolicName(manifest.getHeader(Constants.BUNDLE_SYMBOLICNAME, false));
			headers.importPackages = HeadersHelper
				.convertImportPackages(manifest.getHeader(Constants.IMPORT_PACKAGE, false));
			headers.dynamicImportPackages = HeadersHelper
				.convertDynamicImportPackages(manifest.getHeader(Constants.DYNAMICIMPORT_PACKAGE, false));
			headers.exportPackages = HeadersHelper
				.convertExportPackages(manifest.getHeader(Constants.EXPORT_PACKAGE, false));
			headers.provideCapabilities = HeadersHelper
				.convertProvideCapabilities(manifest.getHeader(Constants.PROVIDE_CAPABILITY, false));
			headers.requireCapabilities = HeadersHelper
				.convertRequireCapabilities(manifest.getHeader(Constants.REQUIRE_CAPABILITY, false));
			headers.requireBundles = HeadersHelper
				.convertRequireBundles(manifest.getHeader(Constants.REQUIRE_BUNDLE, false));
			headers.bundleRequiredExecutionEnvironments = HeadersHelper.convertBundleRequiredExecutionEnvironments(
				manifest.getHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, false));
			headers.bundleActivationPolicy = HeadersHelper.convertBundleActivationPolicy(
				manifest.getHeader(Constants.BUNDLE_ACTIVATIONPOLICY, false), jar.getPackages());
			headers.fragmentHost = HeadersHelper
				.convertFragmentHost(manifest.getHeader(Constants.FRAGMENT_HOST, false));
			headers.bundleActivator = HeadersHelper
				.convertBundleActivator(manifest.getHeader(Constants.BUNDLE_ACTIVATOR, false));
			headers.bundleClassPaths = HeadersHelper
				.convertBundleClassPaths(manifest.getHeader(Constants.BUNDLE_CLASSPATH, false));
			headers.bundleNativeCode = HeadersHelper
				.convertBundleNativeCode(manifest.getHeader(Constants.BUNDLE_NATIVECODE, true));
			headers.bundleManifestVersion = HeadersHelper
				.convertBundleManifestVersion(manifest.getHeader(Constants.BUNDLE_MANIFESTVERSION, false));

			return headers;
		} else {
			return null;
		}
	}

	@Override
	public Map<String, String> getProperties() {
		return Collections.unmodifiableMap(_properties);
	}

	@Override
	public void setProperties(final Map<String, String> map) throws Exception {
		_properties.putAll(map);
	}

	@Override
	public void setReporter(final Reporter processor) {
		// Nothing
	}
}
