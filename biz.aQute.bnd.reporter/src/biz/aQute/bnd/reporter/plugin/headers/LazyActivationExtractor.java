package biz.aQute.bnd.reporter.plugin.headers;

import java.util.LinkedList;
import java.util.List;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.lib.tag.Tag;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.ManifestHelper;

public class LazyActivationExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "bundle-activation-policy";
	final private static String POLICY_TAG = "policy";
	final private static String EXCLUDE_TAG = "exclude";
	final private static String INCLUDE_TAG = "include";

	@Override
	public List<Tag> extract(final ManifestHelper manifest, final Jar jar, final Reporter reporter) {
		final List<Tag> result = new LinkedList<>();
		final Parameters header = manifest.getHeader(Constants.BUNDLE_ACTIVATIONPOLICY, false);
		if (!header.isEmpty()) {
			final Tag tag = new Tag(HEADER_TAG, new Tag(POLICY_TAG, "lazy"));
			final Attrs attributes = header.values().iterator().next();

			if (attributes.containsKey("exclude:")) {
				for (final String a : attributes.get("exclude:").split(",")) {
					tag.addContent(new Tag(EXCLUDE_TAG, a.trim()));
				}
			}

			boolean found = false;
			if (attributes.containsKey("include:")) {
				for (final String a : attributes.get("include:").split(",")) {
					found = true;
					tag.addContent(new Tag(INCLUDE_TAG, a.trim()));
				}
			}

			if (!found) {
				for (final String a : jar.getPackages()) {
					tag.addContent(new Tag(INCLUDE_TAG, a));
				}
			}
			result.add(tag);
		}
		return result;
	}
}
