package biz.aQute.bnd.reporter.plugin.headers;

import java.util.LinkedList;
import java.util.List;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.lib.tag.Tag;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.ManifestHelper;

public class VersionExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "bundle-version";

	@Override
	public List<Tag> extract(final ManifestHelper manifest, final Jar jar, final Reporter reporter) {
		final List<Tag> result = new LinkedList<>();
		final Parameters header = manifest.getHeader(Constants.BUNDLE_VERSION, false);
		if (!header.isEmpty()) {
			final Tag version = toVersion(cleanKey(header.keySet().iterator().next()), HEADER_TAG);
			if (version == null) {
				result.add(getDefaultVersion(HEADER_TAG));
			} else {
				result.add(version);
			}
		} else {
			result.add(getDefaultVersion(HEADER_TAG));
		}
		return result;
	}
}