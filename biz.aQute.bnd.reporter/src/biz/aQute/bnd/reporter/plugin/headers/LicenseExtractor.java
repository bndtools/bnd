package biz.aQute.bnd.reporter.plugin.headers;

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.lib.tag.Tag;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.ManifestHelper;

public class LicenseExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "bundle-license";
	final private static String NAME_TAG = "name";
	final private static String DESCRIPTION_TAG = "description";
	final private static String LINK_TAG = "link";

	@Override
	public List<Tag> extract(final ManifestHelper manifest, final Jar jar, final Reporter reporter) {
		final List<Tag> result = new LinkedList<>();
		final Parameters header = manifest.getHeader(Constants.BUNDLE_LICENSE, false);
		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final Tag tag = new Tag(HEADER_TAG);

			tag.addContent(new Tag(NAME_TAG, cleanKey(entry.getKey())));

			if (entry.getValue().containsKey("description")) {
				tag.addContent(new Tag(DESCRIPTION_TAG, entry.getValue().get("description")));
			}

			if (entry.getValue().containsKey("link")) {
				tag.addContent(new Tag(LINK_TAG, entry.getValue().get("link")));
			}
			result.add(tag);
		}
		return result;
	}
}
