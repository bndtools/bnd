package aQute.bnd.metadata;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.DeveloperDTO;
import aQute.bnd.metadata.dto.LocalizableManifestHeadersDTO;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class DeveloperExtractor extends HeaderExtractor {

	public DeveloperExtractor() {
		super(Constants.BUNDLE_DEVELOPERS, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {

			dto.developers = new LinkedList<>();

			fillDevelopers(dto.developers, header);

		}

		for (Entry<String,Parameters> entry : localizedheaders.entrySet()) {
			if (!dto.localizations.containsKey(entry.getKey())) {
				dto.localizations.put(entry.getKey(), new LocalizableManifestHeadersDTO());
			}

			LocalizableManifestHeadersDTO ldto = dto.localizations.get(entry.getKey());

			ldto.developers = new LinkedList<>();

			fillDevelopers(ldto.developers, entry.getValue());
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		dto.developers = replaceNull(dto.developers);

		for (DeveloperDTO e : dto.developers) {

			e.roles = replaceNull(e.roles);
		}

		dto.localizations = replaceNull(dto.localizations);

		for (Entry<String,LocalizableManifestHeadersDTO> l : dto.localizations.entrySet()) {

			LocalizableManifestHeadersDTO e = l.getValue();

			e.developers = replaceNull(e.developers);
		}
	}

	private void fillDevelopers(List<DeveloperDTO> ldto, Parameters param) {

		for (Entry<String,Attrs> entry : param.entrySet()) {
			DeveloperDTO dev = new DeveloperDTO();

			if (entry.getValue().get("email") == null) {

				dev.email = cleanKey(entry.getKey());
			} else {

				dev.email = entry.getValue().get("email");
			}

			dev.name = entry.getValue().get("name");
			dev.organization = entry.getValue().get("organization");
			dev.organizationUrl = entry.getValue().get("organizationUrl");

			dev.roles = new LinkedList<>();

			if (entry.getValue().get("roles") != null) {

				for (String role : entry.getValue().get("roles").split(",")) {

					dev.roles.add(role.trim());
				}
			}

			try {

				dev.timezone = Integer.valueOf(entry.getValue().get("timezone"));

			} catch (Exception expected) {
				// Nothing to do
			}

			ldto.add(dev);
		}
	}
}
