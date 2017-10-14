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

public class ImportExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "import-package";
	final private static String PACKAGE_NAME_TAG = "package-name";
	final private static String BSN_TAG = "bsn";
	final private static String RESOLUTION_TAG = "resolution";
	final private static String VERSION_TAG = "version";
	final private static String BUNDLE_VERSION_TAG = "bundle-version";
	final private static String ARBITRARY_ATTRIBUTE_TAG = "arbitrary-attribute";
	final private static String NAME_TAG = "name";
	final private static String VALUE_TAG = "value";

	@Override
	public List<Tag> extract(final ManifestHelper manifest, final Jar jar, final Reporter reporter) {
		final List<Tag> result = new LinkedList<>();
		final Parameters header = manifest.getHeader(Constants.IMPORT_PACKAGE, false);
		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final Tag tag = new Tag(HEADER_TAG);

			tag.addContent(new Tag(PACKAGE_NAME_TAG, cleanKey(entry.getKey())));

			if (entry.getValue().containsKey("resolution:")) {
				tag.addContent(new Tag(RESOLUTION_TAG, entry.getValue().get("resolution:")));
			} else {
				tag.addContent(new Tag(RESOLUTION_TAG, "mandatory"));
			}

			if (entry.getValue().containsKey("bundle-symbolic-name")) {
				tag.addContent(new Tag(BSN_TAG, entry.getValue().get("bundle-symbolic-name")));
			}

			final Tag version = toOsgiRange(entry.getValue().get("version", ""), VERSION_TAG);
			if (version == null) {
				tag.addContent(getDefaultRange(VERSION_TAG));
			} else {
				tag.addContent(version);
			}

			final Tag bundleVersion = toOsgiRange(entry.getValue().get("bundle-version", ""), BUNDLE_VERSION_TAG);
			if (bundleVersion == null) {
				tag.addContent(getDefaultRange(BUNDLE_VERSION_TAG));
			} else {
				tag.addContent(bundleVersion);
			}

			final Attrs attribute = new Attrs(entry.getValue());
			attribute.remove("bundle-symbolic-name");
			attribute.remove("version");
			attribute.remove("bundle-version");
			attribute.remove("resolution:");

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
