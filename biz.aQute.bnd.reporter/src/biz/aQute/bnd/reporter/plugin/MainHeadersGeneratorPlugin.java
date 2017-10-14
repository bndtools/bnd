package biz.aQute.bnd.reporter.plugin;

import java.util.Objects;

import aQute.bnd.osgi.Jar;
import aQute.bnd.service.reporter.ReportGeneratorPlugin;
import aQute.bnd.service.reporter.XmlReportPart;
import aQute.lib.tag.Tag;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.ManifestHelper;
import biz.aQute.bnd.reporter.generator.ReportResult;
import biz.aQute.bnd.reporter.plugin.headers.HeaderExtractors;

public class MainHeadersGeneratorPlugin implements ReportGeneratorPlugin {

	final private static String MAIN_HEADERS_TAG = "main-headers";

	@Override
	public XmlReportPart report(final Jar jar, final String locale,Reporter reporter) {
		Objects.requireNonNull(jar, "jar");
		Objects.requireNonNull(locale, "locale");

		final ReportResult result = new ReportResult();
		final Tag top = new Tag(MAIN_HEADERS_TAG);

		final ManifestHelper manifest = ManifestHelper.get(jar, locale);
		if (manifest != null) {
			for (final Tag headerTag : HeaderExtractors.extract(manifest, jar,reporter)) {
				top.addContent(headerTag);
			}
		}
		if (!top.getContents().isEmpty()) {
			result.add(top);
		}
		return result;
	}
}
