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

public class ExportExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "export-package";
	final private static String PACKAGE_NAME_TAG = "package-name";
	final private static String EXCLUDE_TAG = "exclude";
	final private static String INCLUDE_TAG = "include";
	final private static String MANDATORY_TAG = "mandatory";
	final private static String USE_TAG = "use";
	final private static String VERSION_TAG = "version";
	final private static String ARBITRARY_ATTRIBUTE_TAG = "arbitrary-attribute";
	final private static String NAME_TAG = "name";
	final private static String VALUE_TAG = "value";

	@Override
	public List<Tag> extract(final ManifestHelper manifest, final Jar jar, final Reporter reporter) {
		final List<Tag> result = new LinkedList<>();
		final Parameters header = manifest.getHeader(Constants.EXPORT_PACKAGE, false);
		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final Tag tag = new Tag(HEADER_TAG);

			tag.addContent(new Tag(PACKAGE_NAME_TAG, cleanKey(entry.getKey())));

			final Tag version = toVersion(entry.getValue().get("version", ""), VERSION_TAG);
			if (version == null) {
				tag.addContent(getDefaultVersion(VERSION_TAG));
			} else {
				tag.addContent(version);
			}

			if (entry.getValue().containsKey("exclude:")) {
				for (final String c : entry.getValue().get("exclude:").split(",")) {
					tag.addContent(new Tag(EXCLUDE_TAG, c.trim()));
				}
			}

			if (entry.getValue().containsKey("include:")) {
				for (final String c : entry.getValue().get("include:").split(",")) {
					tag.addContent(new Tag(INCLUDE_TAG, c.trim()));
				}
			}

			if (entry.getValue().containsKey("mandatory:")) {
				for (final String c : entry.getValue().get("mandatory:").split(",")) {
					tag.addContent(new Tag(MANDATORY_TAG, c.trim()));
				}
			}

			if (entry.getValue().containsKey("uses:")) {
				for (final String c : entry.getValue().get("uses:").split(",")) {
					tag.addContent(new Tag(USE_TAG, c.trim()));
				}
			}

			final Attrs attribute = new Attrs(entry.getValue());
			attribute.remove("version");
			attribute.remove("exclude:");
			attribute.remove("include:");
			attribute.remove("mandatory:");
			attribute.remove("uses:");

			for (final Entry<String, String> a : attribute.entrySet()) {
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
