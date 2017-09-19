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

public class FragmentHostExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "fragment-host";
	final private static String BSN_TAG = "bsn";
	final private static String EXTENSION_TAG = "extension";
	final private static String BUNDLE_VERSION_TAG = "bundle-version";
	final private static String ARBITRARY_ATTRIBUTE_TAG = "arbitrary-attribute";
	final private static String NAME_TAG = "name";
	final private static String VALUE_TAG = "value";

	@Override
	public List<Tag> extract(final ManifestHelper manifest, final Jar jar, final Reporter reporter) {
		final List<Tag> result = new LinkedList<>();
		final Parameters header = manifest.getHeader(Constants.FRAGMENT_HOST, false);
		if (!header.isEmpty()) {
			final Attrs attibutes = header.values().iterator().next();
			final Tag tag = new Tag(HEADER_TAG);

			tag.addContent(new Tag(BSN_TAG, header.keySet().iterator().next()));

			if (attibutes.containsKey("extension:")) {
				tag.addContent(new Tag(EXTENSION_TAG, attibutes.get("extension:")));
			} else {
				tag.addContent(new Tag(EXTENSION_TAG, "framework"));
			}

			final Tag bundleVersion = toOsgiRange(attibutes.get("bundle-version", ""), BUNDLE_VERSION_TAG);
			if (bundleVersion == null) {
				tag.addContent(getDefaultRange(BUNDLE_VERSION_TAG));
			} else {
				tag.addContent(bundleVersion);
			}

			attibutes.remove("bundle-version");
			attibutes.remove("extension:");

			for (final Entry<String, String> a : attibutes.entrySet()) {
				if (!a.getKey().endsWith(":")) {
					final Tag aTag = new Tag(ARBITRARY_ATTRIBUTE_TAG);
					aTag.addContent(new Tag(NAME_TAG, a.getKey()));
					aTag.addContent(new Tag(VALUE_TAG, a.getValue()));
					tag.addContent(aTag);
				}
			}
			result.add(tag);
		}
		return result;
	}
}
