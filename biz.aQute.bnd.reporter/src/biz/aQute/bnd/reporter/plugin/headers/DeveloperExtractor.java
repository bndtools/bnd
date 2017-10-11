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

public class DeveloperExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "bundle-developer";
	final private static String EMAIL_TAG = "email";
	final private static String NAME_TAG = "name";
	final private static String INDENTIFIER_TAG = "identifier";
	final private static String ORGANIZATION_TAG = "organization";
	final private static String ORGANIZATION_URL_TAG = "organization-url";
	final private static String TIMEZONE_TAG = "timezone";
	final private static String ROLE_TAG = "role";

	@Override
	public List<Tag> extract(final ManifestHelper manifest, final Jar jar, final Reporter reporter) {
		final List<Tag> result = new LinkedList<>();
		final Parameters header = manifest.getHeader(Constants.BUNDLE_DEVELOPERS, false);
		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final Tag tag = new Tag(HEADER_TAG);

			tag.addContent(new Tag(INDENTIFIER_TAG, cleanKey(entry.getKey())));

			if (entry.getValue().containsKey("email")) {
				tag.addContent(new Tag(EMAIL_TAG, entry.getValue().get("email")));
			}

			if (entry.getValue().containsKey("name")) {
				tag.addContent(new Tag(NAME_TAG, entry.getValue().get("name")));
			}

			if (entry.getValue().containsKey("organization")) {
				tag.addContent(new Tag(ORGANIZATION_TAG, entry.getValue().get("organization")));
			}

			if (entry.getValue().containsKey("organizationUrl")) {
				tag.addContent(new Tag(ORGANIZATION_URL_TAG, entry.getValue().get("organizationUrl")));
			}

			if (entry.getValue().containsKey("timezone")) {
				tag.addContent(new Tag(TIMEZONE_TAG, entry.getValue().get("timezone")));
			}

			if (entry.getValue().containsKey("roles")) {
				for (final String role : entry.getValue().get("roles").split(",")) {
					tag.addContent(new Tag(ROLE_TAG, role.trim()));
				}
			}
			result.add(tag);
		}
		return result;
	}
}
