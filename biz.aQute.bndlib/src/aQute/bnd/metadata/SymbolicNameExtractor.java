package aQute.bnd.metadata;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.BundleSymbolicNameDTO;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class SymbolicNameExtractor extends HeaderExtractor {

	public SymbolicNameExtractor() {
		super(Constants.BUNDLE_SYMBOLICNAME, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {

			Attrs attibutes = header.values().iterator().next();

			dto.symbolicName = new BundleSymbolicNameDTO();

			dto.symbolicName.symbolicName = header.keySet().iterator().next();

			dto.symbolicName.mandatory = new LinkedList<>();

			if (attibutes.get("mandatory:") != null) {

				for (String c : attibutes.get("mandatory:").split(",")) {

					dto.symbolicName.mandatory.add(c.trim());
				}
			}

			dto.symbolicName.fragmentAttachment = attibutes.get("fragment-attachment:");

			if (dto.symbolicName.fragmentAttachment == null) {

				dto.symbolicName.fragmentAttachment = "always";
			}

			if (attibutes.get("singleton:") != null) {

				dto.symbolicName.singleton = Boolean.valueOf(attibutes.get("singleton:"));

			} else {

				dto.symbolicName.singleton = false;
			}

			dto.symbolicName.arbitraryAttributes = new HashMap<>();

			attibutes.remove("fragment-attachment:");
			attibutes.remove("mandatory:");
			attibutes.remove("singleton:");

			for (Entry<String,String> a : attibutes.entrySet()) {

				// we ignore directives
				if (!a.getKey().endsWith(":")) {

					dto.symbolicName.arbitraryAttributes.put(a.getKey(), a.getValue());
				}
			}
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		if (dto.symbolicName == null) {

			error("the bundle does not declare a symbolic name");
		}

		if (dto.symbolicName.symbolicName == null) {

			error("the bundle does not declare a symbolic name");
		}

		dto.symbolicName.arbitraryAttributes = replaceNull(dto.symbolicName.arbitraryAttributes);

		dto.symbolicName.mandatory = replaceNull(dto.symbolicName.mandatory);

		if (dto.symbolicName.singleton == null) {

			error("the bundle does not declare if it must be a singleton");
		}

		if (dto.symbolicName.fragmentAttachment == null) {

			error("the bundle does not declare a fragment attachment policy");
		}
	}
}
