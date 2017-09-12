package aQute.bnd.metadata;

import java.util.Map;

import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class ManifestVersionExtractor extends HeaderExtractor {

	public ManifestVersionExtractor() {
		super(Constants.BUNDLE_MANIFESTVERSION, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {

			try {

				dto.manifestVersion = Integer.valueOf(cleanKey(header.keySet().iterator().next()));

			} catch (Exception e) {

				dto.manifestVersion = 1;
			}

		} else {

			dto.manifestVersion = 1;
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		if (dto.manifestVersion == null) {

			error("the bundle does not declare a manifest version");
		}
	}
}
