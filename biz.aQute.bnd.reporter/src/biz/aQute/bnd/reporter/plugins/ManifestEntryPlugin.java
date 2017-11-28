package biz.aQute.bnd.reporter.plugins;

import java.util.Map;
import java.util.Objects;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.reporter.BundleEntryPlugin;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import biz.aQute.bnd.reporter.plugins.headers.HeaderExtractors;

public class ManifestEntryPlugin implements BundleEntryPlugin {

	final private static String MAIN_HEADERS_TAG = "manifest";

	@Override
	public Object extract(final Jar jar, final String locale, final Processor reporter) {
		Objects.requireNonNull(jar, "jar");
		Objects.requireNonNull(locale, "locale");
		Objects.requireNonNull(reporter, "reporter");

		Object result = null;
		final ManifestHelper manifest = ManifestHelper.get(jar, locale);
		if (manifest != null) {
			final Map<String, Object> headers = HeaderExtractors.extract(manifest, jar, reporter);
			result = headers;
		}
		return result;
	}

	@Override
	public String getEntryName() {
		return MAIN_HEADERS_TAG;
	}
}
