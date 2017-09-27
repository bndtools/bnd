package aQute.bnd.metadata;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.ContactAddressDTO;
import aQute.bnd.metadata.dto.LocalizableManifestHeadersDTO;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class ContactAddressExtractor extends HeaderExtractor {

	public ContactAddressExtractor() {
		super(Constants.BUNDLE_CONTACTADDRESS, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {

			dto.contactAddress = new ContactAddressDTO();

			String contact = cleanKey(header.keySet().iterator().next());

			if (isUrl(contact)) {

				dto.contactAddress.type = "url";

			} else if (isEmail(contact)) {

				dto.contactAddress.type = "email";

			} else {

				dto.contactAddress.type = "postal";
			}

			dto.contactAddress.address = contact;
		}

		for (Entry<String,Parameters> entry : localizedheaders.entrySet()) {

			if (!dto.localizations.containsKey(entry.getKey())) {

				dto.localizations.put(entry.getKey(), new LocalizableManifestHeadersDTO());
			}

			dto.localizations.get(entry.getKey()).contactAddress = new ContactAddressDTO();

			String contact = cleanKey(entry.getValue().keySet().iterator().next());

			if (isUrl(contact)) {

				dto.localizations.get(entry.getKey()).contactAddress.type = "url";
			} else if (isEmail(contact)) {

				dto.localizations.get(entry.getKey()).contactAddress.type = "email";
			} else {

				dto.localizations.get(entry.getKey()).contactAddress.type = "postal";
			}

			dto.localizations.get(entry.getKey()).contactAddress.address = contact;
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		if (dto.contactAddress != null) {

			if (dto.contactAddress.type == null) {

				error("the contact address does not declare a type");
			}

			if (dto.contactAddress.address == null) {

				error("the contact address does not declare an address");
			}
		}

		dto.localizations = replaceNull(dto.localizations);

		for (Entry<String,LocalizableManifestHeadersDTO> l : dto.localizations.entrySet()) {

			LocalizableManifestHeadersDTO e = l.getValue();

			if (e.contactAddress != null) {

				if (e.contactAddress.type == null) {

					error("the contact address does not declare a type: locale = " + l.getKey());
				}

				if (e.contactAddress.address == null) {

					error("the contact address does not declare an address: locale = " + l.getKey());
				}
			}
		}
	}

	private boolean isUrl(String value) {
		
		try {

			new URL(value);

			return true;

		} catch (MalformedURLException e) {

			return false;
		}
	}

	private boolean isEmail(String value) {

		if (!value.contains(" ") && value.matches(".+@.+")) {

			return true;

		} else {

			return false;
		}
	}
}
