package aQute.bnd.metadata;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.IconDTO;
import aQute.bnd.metadata.dto.LocalizableManifestHeadersDTO;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class IconExtractor extends HeaderExtractor {

	public IconExtractor() {
		super(Constants.BUNDLE_ICON, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {

			dto.icons = new LinkedList<>();

			fillIcons(dto.icons, header);

		}

		for (Entry<String,Parameters> entry : localizedheaders.entrySet()) {
			if (!dto.localizations.containsKey(entry.getKey())) {
				dto.localizations.put(entry.getKey(), new LocalizableManifestHeadersDTO());
			}

			LocalizableManifestHeadersDTO ldto = dto.localizations.get(entry.getKey());

			ldto.icons = new LinkedList<>();

			fillIcons(ldto.icons, entry.getValue());
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		dto.icons = replaceNull(dto.icons);

		for (IconDTO e : dto.icons) {

			if (e.url == null) {

				error("the bundle declare an icon with no url: icon index = " + dto.icons.indexOf(e));
			}
		}

		dto.localizations = replaceNull(dto.localizations);

		for (Entry<String,LocalizableManifestHeadersDTO> l : dto.localizations.entrySet()) {

			LocalizableManifestHeadersDTO e = l.getValue();

			e.icons = replaceNull(e.icons);

			for (IconDTO i : e.icons) {

				if (i.url == null) {

					error("the bundle declare an icon with no url: locale = " + l.getKey() + ", icon index = "
							+ e.icons.indexOf(i));
				}
			}
		}
	}

	private void fillIcons(List<IconDTO> ldto, Parameters param) {

		for (Entry<String,Attrs> entry : param.entrySet()) {
			IconDTO ico = new IconDTO();

			ico.url = cleanKey(entry.getKey());

			try {

				ico.size = Integer.valueOf(entry.getValue().get("size"));

			} catch (Exception expected) {

				// Notinh to do
			}

			ldto.add(ico);
		}
	}
}
