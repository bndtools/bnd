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

public class SymbolicNameExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "bundle-symbolic-name";
	final private static String SYMBOLIC_NAME_TAG = "symbolic-name";
	final private static String MANDATORY_TAG = "mandatory";
	final private static String FRAGMENT_ATTACHMENT_TAG = "fragment-attachment";
	final private static String SINGLETON_TAG = "singleton";
	final private static String ARBITRARY_ATTRIBUTE_TAG = "arbitrary-attribute";
	final private static String NAME_TAG = "name";
	final private static String VALUE_TAG = "value";

	@Override
	public List<Tag> extract(final ManifestHelper manifest, final Jar jar, final Reporter reporter) {
		final List<Tag> result = new LinkedList<>();
		final Parameters header = manifest.getHeader(Constants.BUNDLE_SYMBOLICNAME, false);
		if (!header.isEmpty()) {
			final Attrs attributes = header.values().iterator().next();
			final Tag tag = new Tag(HEADER_TAG);

			tag.addContent(new Tag(SYMBOLIC_NAME_TAG, cleanKey(header.keySet().iterator().next())));

			if (attributes.containsKey("mandatory:")) {
				for (final String c : attributes.get("mandatory:").split(",")) {
					tag.addContent(new Tag(MANDATORY_TAG, c.trim()));
				}
			}

			if (attributes.containsKey("fragment-attachment:")) {
				tag.addContent(new Tag(FRAGMENT_ATTACHMENT_TAG, attributes.get("fragment-attachment:")));
			} else {
				tag.addContent(new Tag(FRAGMENT_ATTACHMENT_TAG, "mandatory"));
			}

			if (attributes.containsKey("singleton:")) {
				tag.addContent(new Tag(SINGLETON_TAG, attributes.get("singleton:")));
			} else {
				tag.addContent(new Tag(SINGLETON_TAG, false));
			}

			attributes.remove("fragment-attachment:");
			attributes.remove("mandatory:");
			attributes.remove("singleton:");

			for (final Entry<String, String> a : attributes.entrySet()) {
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
