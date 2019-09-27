package biz.aQute.bnd.reporter.plugins.entries.bndproject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Project;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;
import biz.aQute.bnd.reporter.helpers.HeadersHelper;
import biz.aQute.bnd.reporter.manifest.dto.CommonInfoDTO;

/**
 * This plugins extract common info from a project. Data are extracted from
 * headers and properties prefixed by "p-" defined in the bnd.bnd file.
 * Properties take precedence.
 */
@BndPlugin(name = "entry." + EntryNamesReference.COMMON_INFO)
public class CommonInfoProjectPlugin implements ReportEntryPlugin<Project>, Plugin {

	final private String				PROP_PREFIX	= "p-";

	private final Map<String, String>	_properties	= new HashMap<>();

	public CommonInfoProjectPlugin() {
		_properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.COMMON_INFO);
		_properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, Project.class.getCanonicalName());
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
	public CommonInfoDTO extract(final Project project, final Locale locale) throws Exception {
		Objects.requireNonNull(project, "project");

		final CommonInfoDTO commonInfo = new CommonInfoDTO();

		commonInfo.contactAddress = extractHeader(Constants.BUNDLE_CONTACTADDRESS, "contactAddress", project,
			HeadersHelper::convertBundleContactAddress);
		commonInfo.copyright = extractHeader(Constants.BUNDLE_COPYRIGHT, "copyright", project,
			HeadersHelper::convertBundleCopyright);
		commonInfo.description = extractHeader(Constants.BUNDLE_DESCRIPTION, "description", project,
			HeadersHelper::convertBundleDescription);
		commonInfo.developers = extractHeader(Constants.BUNDLE_DEVELOPERS, "developers", project,
			HeadersHelper::convertBundleDevelopers);
		commonInfo.docURL = extractHeader(Constants.BUNDLE_DOCURL, "docURL", project,
			HeadersHelper::convertBundleDocURL);
		commonInfo.icons = extractHeader(Constants.BUNDLE_ICON, "icons", project, HeadersHelper::convertBundleIcons);
		commonInfo.licenses = extractHeader(Constants.BUNDLE_LICENSE, "licenses", project,
			HeadersHelper::convertBundleLicenses);
		commonInfo.name = extractHeader(Constants.BUNDLE_NAME, "name", project, HeadersHelper::convertBundleName);
		commonInfo.scm = extractHeader(Constants.BUNDLE_SCM, "scm", project, HeadersHelper::convertBundleSCM);
		commonInfo.updateLocation = extractHeader(Constants.BUNDLE_UPDATELOCATION, "updateLocation", project,
			HeadersHelper::convertBundleUpdateLocation);
		commonInfo.vendor = extractHeader(Constants.BUNDLE_VENDOR, "vendor", project,
			HeadersHelper::convertBundleVendor);
		commonInfo.version = extractHeader(Constants.BUNDLE_VERSION, "version", project,
			HeadersHelper::convertBundleVersion);

		return commonInfo;
	}

	private <T> T extractHeader(final String header, final String propertyName, final Project project,
		final Function<Parameters, T> converter) {
		Parameters param = null;
		String headerValue = project.get(PROP_PREFIX + propertyName);

		if (headerValue == null) {
			headerValue = project.get(header);
		}

		if (headerValue != null) {
			param = new Parameters(headerValue, null, false);
		}

		return param != null ? converter.apply(param) : null;
	}
}
