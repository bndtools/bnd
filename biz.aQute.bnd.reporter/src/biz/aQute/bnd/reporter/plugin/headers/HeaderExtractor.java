package biz.aQute.bnd.reporter.plugin.headers;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import aQute.bnd.osgi.Jar;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.tag.Tag;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.ManifestHelper;

abstract class HeaderExtractor {

	final private static String MAJOR_TAG = "major";
	final private static String MINOR_TAG = "minor";
	final private static String MICRO_TAG = "micro";
	final private static String QUALIFIER_TAG = "qualifier";
	final private static String FLOOR_TAG = "version-floor";
	final private static String CEILING_TAG = "version-ceiling";
	final private static String INCLUDE_ATTR = "include";

	abstract public List<Tag> extract(final ManifestHelper manifest, final Jar jar, final Reporter reporter);

	protected Tag toOsgiRange(final String value, final String tagName) {
		if (value != null && VersionRange.isOSGiVersionRange(value)) {
			return toRange(VersionRange.parseOSGiVersionRange(value), tagName);
		} else {
			return null;
		}
	}

	protected Tag toRange(final String value, final String tagName) {
		if (value != null && VersionRange.isVersionRange(value)) {
			return toRange(VersionRange.parseVersionRange(value), tagName);
		} else {
			return null;
		}
	}

	protected Tag toRange(final VersionRange range, final String tagName) {
		final Tag result = new Tag(tagName);
		final Tag floor = new Tag(FLOOR_TAG);

		floor.addAttribute(INCLUDE_ATTR, range.includeLow());
		floor.addContent(new Tag(MAJOR_TAG, range.getLow().getMajor()));
		floor.addContent(new Tag(MINOR_TAG, range.getLow().getMinor()));
		floor.addContent(new Tag(MICRO_TAG, range.getLow().getMicro()));
		if (range.getLow().getQualifier() != null && !range.getLow().getQualifier().isEmpty()) {
			floor.addContent(new Tag(QUALIFIER_TAG, range.getLow().getQualifier()));
		}
		result.addContent(floor);

		if (!range.isSingleVersion()) {
			final Tag ceiling = new Tag(CEILING_TAG);

			ceiling.addAttribute(INCLUDE_ATTR, range.includeHigh());
			ceiling.addContent(new Tag(MAJOR_TAG, range.getHigh().getMajor()));
			ceiling.addContent(new Tag(MINOR_TAG, range.getHigh().getMinor()));
			ceiling.addContent(new Tag(MICRO_TAG, range.getHigh().getMicro()));
			if (range.getHigh().getQualifier() != null && !range.getHigh().getQualifier().isEmpty()) {
				ceiling.addContent(new Tag(QUALIFIER_TAG, range.getHigh().getQualifier()));
			}
			result.addContent(ceiling);
		}

		return result;
	}

	protected Tag toVersion(final String value, final String tagName) {
		if (value != null && Version.isVersion(value)) {
			final Version version = Version.parseVersion(value);
			final Tag result = new Tag(tagName);

			result.addContent(new Tag(MAJOR_TAG, version.getMajor()));
			result.addContent(new Tag(MINOR_TAG, version.getMinor()));
			result.addContent(new Tag(MICRO_TAG, version.getMicro()));
			if (version.getQualifier() != null && !version.getQualifier().isEmpty()) {
				result.addContent(new Tag(QUALIFIER_TAG, version.getQualifier()));
			}

			return result;
		} else {
			return null;
		}
	}

	protected Tag getDefaultRange(final String tagName) {
		final Tag range = new Tag(tagName);
		final Tag floor = new Tag(FLOOR_TAG);

		floor.addAttribute(INCLUDE_ATTR, true);
		floor.addContent(new Tag(MAJOR_TAG, 0));
		floor.addContent(new Tag(MINOR_TAG, 0));
		floor.addContent(new Tag(MICRO_TAG, 0));
		range.addContent(floor);

		return range;
	}

	protected Tag getDefaultVersion(final String tagName) {
		final Tag version = new Tag(tagName);

		version.addContent(new Tag(MAJOR_TAG, 0));
		version.addContent(new Tag(MINOR_TAG, 0));
		version.addContent(new Tag(MICRO_TAG, 0));

		return version;
	}

	protected String cleanKey(final String key) {
		String result = key;
		if (key != null) {
			while (result.endsWith("~")) {
				result = result.substring(0, result.length() - 1);
			}
		}
		return result;
	}

	protected List<String> cleanKey(final Set<String> keys) {
		final List<String> result = new LinkedList<>();
		if (keys != null) {
			for (final String key : keys) {
				result.add(cleanKey(key));
			}
		}
		return result;
	}
}
