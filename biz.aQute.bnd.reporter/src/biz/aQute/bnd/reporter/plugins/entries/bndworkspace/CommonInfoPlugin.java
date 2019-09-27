package biz.aQute.bnd.reporter.plugins.entries.bndworkspace;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Workspace;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;
import biz.aQute.bnd.reporter.helpers.HeadersHelper;
import biz.aQute.bnd.reporter.manifest.dto.CommonInfoDTO;

/**
 * This plugins extract common info from a workspace. Data are extracted from
 * headers and properties prefixed by "ws-" defined in the build.bnd file.
 * Properties take precedence.
 */
@BndPlugin(name = "entry." + EntryNamesReference.COMMON_INFO)
public class CommonInfoPlugin implements ReportEntryPlugin<Workspace>, Plugin {

	final private String				PROP_PREFIX	= "ws-";

	private final Map<String, String>	_properties	= new HashMap<>();

	public CommonInfoPlugin() {
		_properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.COMMON_INFO);
		_properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, Workspace.class.getCanonicalName());
	}

	@Override
	public void setReporter(final Reporter processor) {
		// Nothing
	}

	@Override
	public void setProperties(final Map<String, String> map) throws Exception {
		_properties.putAll(map);
	}

	@Override
	public Map<String, String> getProperties() {
		return Collections.unmodifiableMap(_properties);
	}

	@Override
	public CommonInfoDTO extract(final Workspace workspace, final Locale locale) throws Exception {
		Objects.requireNonNull(workspace, "workspace");

		final CommonInfoDTO commonInfo = new CommonInfoDTO();

		commonInfo.contactAddress = extractHeader(Constants.BUNDLE_CONTACTADDRESS, "contactAddress", workspace,
			HeadersHelper::convertBundleContactAddress);
		commonInfo.copyright = extractHeader(Constants.BUNDLE_COPYRIGHT, "copyright", workspace,
			HeadersHelper::convertBundleCopyright);
		commonInfo.description = extractHeader(Constants.BUNDLE_DESCRIPTION, "description", workspace,
			HeadersHelper::convertBundleDescription);
		commonInfo.developers = extractHeader(Constants.BUNDLE_DEVELOPERS, "developers", workspace,
			HeadersHelper::convertBundleDevelopers);
		commonInfo.docURL = extractHeader(Constants.BUNDLE_DOCURL, "docURL", workspace,
			HeadersHelper::convertBundleDocURL);
		commonInfo.icons = extractHeader(Constants.BUNDLE_ICON, "icons", workspace, HeadersHelper::convertBundleIcons);
		commonInfo.licenses = extractHeader(Constants.BUNDLE_LICENSE, "licenses", workspace,
			HeadersHelper::convertBundleLicenses);
		commonInfo.name = extractHeader(Constants.BUNDLE_NAME, "name", workspace, HeadersHelper::convertBundleName);
		commonInfo.scm = extractHeader(Constants.BUNDLE_SCM, "scm", workspace, HeadersHelper::convertBundleSCM);
		commonInfo.updateLocation = extractHeader(Constants.BUNDLE_UPDATELOCATION, "updateLocation", workspace,
			HeadersHelper::convertBundleUpdateLocation);
		commonInfo.vendor = extractHeader(Constants.BUNDLE_VENDOR, "vendor", workspace,
			HeadersHelper::convertBundleVendor);
		commonInfo.version = extractHeader(Constants.BUNDLE_VERSION, "version", workspace,
			HeadersHelper::convertBundleVersion);

		return commonInfo;
	}

	private <T> T extractHeader(final String header, final String propertyName, final Workspace workspace,
		final Function<Parameters, T> converter) {
		Parameters param = null;
		String headerValue = workspace.get(PROP_PREFIX + propertyName);

		if (headerValue == null) {
			headerValue = workspace.get(header);
		}

		if (headerValue != null) {
			param = new Parameters(headerValue, null, false);
		}

		return param != null ? converter.apply(param) : null;
	}
}
