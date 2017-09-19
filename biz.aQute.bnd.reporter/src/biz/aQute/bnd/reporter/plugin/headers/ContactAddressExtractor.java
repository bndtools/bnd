package biz.aQute.bnd.reporter.plugin.headers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.lib.tag.Tag;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.ManifestHelper;

public class ContactAddressExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "bundle-contact-address";
	final private static String TYPE_ATTR = "type";

	@Override
	public List<Tag> extract(final ManifestHelper manifest, final Jar jar, final Reporter reporter) {
		final List<Tag> result = new LinkedList<>();
		final Parameters header = manifest.getHeader(Constants.BUNDLE_CONTACTADDRESS, false);
		if (!header.isEmpty()) {
			final String contact = cleanKey(header.keySet().iterator().next());
			final Tag adress = new Tag(HEADER_TAG, contact);
			if (isUrl(contact)) {
				adress.addAttribute(TYPE_ATTR, "url");
			} else if (isEmail(contact)) {
				adress.addAttribute(TYPE_ATTR, "email");
			} else {
				adress.addAttribute(TYPE_ATTR, "postal");
			}
			result.add(adress);
		}
		return result;
	}

	private boolean isUrl(final String value) {
		try {
			new URL(value);
			return true;
		} catch (final MalformedURLException e) {
			return false;
		}
	}

	private boolean isEmail(final String value) {
		if (!value.contains(" ") && value.matches(".+@.+")) {
			return true;
		} else {
			return false;
		}
	}
}
