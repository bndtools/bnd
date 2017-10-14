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

public class IconExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "bundle-icon";
	final private static String SIZE_TAG = "size";
	final private static String URL_TAG = "url";

	@Override
	public List<Tag> extract(final ManifestHelper manifest, final Jar jar, final Reporter reporter) {
		final List<Tag> result = new LinkedList<>();
		final Parameters header = manifest.getHeader(Constants.BUNDLE_ICON, false);
		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final Tag tag = new Tag(HEADER_TAG);

			tag.addContent(new Tag(URL_TAG, cleanKey(entry.getKey())));

			if (entry.getValue().containsKey("size")) {
				tag.addContent(new Tag(SIZE_TAG, entry.getValue().get("size")));
			}
			result.add(tag);
		}
		return result;
	}
}
