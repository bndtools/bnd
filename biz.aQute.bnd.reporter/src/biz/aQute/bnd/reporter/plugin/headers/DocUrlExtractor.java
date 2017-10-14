package biz.aQute.bnd.reporter.plugin.headers;

import java.util.LinkedList;
import java.util.List;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.lib.tag.Tag;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.ManifestHelper;

public class DocUrlExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "bundle-doc-url";

	@Override
	public List<Tag> extract(final ManifestHelper manifest, final Jar jar, final Reporter reporter) {
		final List<Tag> result = new LinkedList<>();
		final Parameters header = manifest.getHeader(Constants.BUNDLE_DOCURL, false);
		String docUrl = mergeKey(header);
		if (!docUrl.isEmpty()) {
			result.add(new Tag(HEADER_TAG, docUrl));
		}
		return result;
	}
}
