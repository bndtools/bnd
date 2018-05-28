package biz.aQute.bnd.reporter.plugins.headers;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import biz.aQute.bnd.reporter.plugins.headers.dto.ContactAddressDTO;
import java.net.MalformedURLException;
import java.net.URL;

public class ContactAddressExtractor extends HeaderExtractor {
	
	final private static String HEADER_TAG = "bundleContactAddress";
	
	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		
		final String contact = manifest.getHeaderAsString(Constants.BUNDLE_CONTACTADDRESS);
		if (!contact.isEmpty()) {
			final ContactAddressDTO adress = new ContactAddressDTO();
			adress.address = contact;
			if (isUrl(contact)) {
				adress.type = "url";
			} else if (isEmail(contact)) {
				adress.type = "email";
			} else {
				adress.type = "postal";
			}
			result = adress;
		}
		
		return result;
	}
	
	@SuppressWarnings("unused")
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
	
	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
