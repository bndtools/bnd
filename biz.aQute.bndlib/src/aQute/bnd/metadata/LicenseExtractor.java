package aQute.bnd.metadata;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.LicenseDTO;
import aQute.bnd.metadata.dto.LocalizableManifestHeadersDTO;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class LicenseExtractor extends HeaderExtractor {

	public LicenseExtractor() {
		super(Constants.BUNDLE_LICENSE, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {

			dto.licenses = new LinkedList<>();

			fillLicenses(dto.licenses, header);

		}

		for (Entry<String,Parameters> entry : localizedheaders.entrySet()) {
			if (!dto.localizations.containsKey(entry.getKey())) {
				dto.localizations.put(entry.getKey(), new LocalizableManifestHeadersDTO());
			}

			LocalizableManifestHeadersDTO ldto = dto.localizations.get(entry.getKey());

			ldto.licenses = new LinkedList<>();

			fillLicenses(ldto.licenses, entry.getValue());
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		dto.licenses = replaceNull(dto.licenses);

		for (LicenseDTO e : dto.licenses) {

			if (e.name == null) {

				error("the licence does not declare a name: licence index = " + dto.licenses.indexOf(e));
			}
		}

		dto.localizations = replaceNull(dto.localizations);

		for (Entry<String,LocalizableManifestHeadersDTO> l : dto.localizations.entrySet()) {

			LocalizableManifestHeadersDTO e = l.getValue();

			e.licenses = replaceNull(e.licenses);

			for (LicenseDTO li : dto.licenses) {

				if (li.name == null) {

					error("the licence does not declare a name: locale = " + l.getKey() + ",licence index = "
							+ e.licenses.indexOf(li));
				}
			}
		}
	}

	private void fillLicenses(List<LicenseDTO> ldto, Parameters param) {

		for (Entry<String,Attrs> entry : param.entrySet()) {
			LicenseDTO lic = new LicenseDTO();

			lic.name = entry.getKey();
			lic.description = entry.getValue().get("description");
			lic.link = entry.getValue().get("link");

			ldto.add(lic);
		}
	}
}
