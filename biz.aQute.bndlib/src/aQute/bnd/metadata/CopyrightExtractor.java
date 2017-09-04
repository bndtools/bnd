package aQute.bnd.metadata;

import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.LocalizableManifestHeadersDTO;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class CopyrightExtractor extends HeaderExtractor {

	public CopyrightExtractor() {
		super(Constants.BUNDLE_COPYRIGHT, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {

			dto.copyright = header.keySet().iterator().next();
		}

		for (Entry<String,Parameters> entry : localizedheaders.entrySet()) {
			if (!dto.localizations.containsKey(entry.getKey())) {
				dto.localizations.put(entry.getKey(), new LocalizableManifestHeadersDTO());
			}

			dto.localizations.get(entry.getKey()).copyright = entry.getValue().keySet().iterator().next();
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		// Nothing to do
	}
}
