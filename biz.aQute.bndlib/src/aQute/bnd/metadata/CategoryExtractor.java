package aQute.bnd.metadata;

import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.LocalizableManifestHeadersDTO;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class CategoryExtractor extends HeaderExtractor {

	public CategoryExtractor() {
		super(Constants.BUNDLE_CATEGORY, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {
			dto.categories = new LinkedList<>();
			dto.categories.addAll(header.keySet());
		}

		for (Entry<String,Parameters> entry : localizedheaders.entrySet()) {
			if (!dto.localizations.containsKey(entry.getKey())) {
				dto.localizations.put(entry.getKey(), new LocalizableManifestHeadersDTO());
			}

			dto.localizations.get(entry.getKey()).categories = new LinkedList<>();
			dto.localizations.get(entry.getKey()).categories.addAll(entry.getValue().keySet());
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		dto.categories = replaceNull(dto.categories);
		dto.localizations = replaceNull(dto.localizations);

		for (Entry<String,LocalizableManifestHeadersDTO> l : dto.localizations.entrySet()) {
			LocalizableManifestHeadersDTO e = l.getValue();
			e.categories = replaceNull(e.categories);
		}
	}
}
