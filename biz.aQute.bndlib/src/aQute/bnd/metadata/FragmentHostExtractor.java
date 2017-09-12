package aQute.bnd.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.FragmentHostDTO;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class FragmentHostExtractor extends HeaderExtractor {

	public FragmentHostExtractor() {
		super(Constants.FRAGMENT_HOST, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {

			dto.fragmentHost = new FragmentHostDTO();

			dto.fragmentHost.bsn = cleanKey(header.keySet().iterator().next());

			Attrs attibutes = header.values().iterator().next();

			dto.fragmentHost.extension = attibutes.get("extension:");

			if (dto.fragmentHost.extension == null) {

				dto.fragmentHost.extension = "framework";
			}

			dto.fragmentHost.bundleVersion = toOsgiRange(attibutes.get("bundle-version", ""));

			if (dto.fragmentHost.bundleVersion == null) {

				dto.fragmentHost.bundleVersion = getDefaultRange();
			}

			dto.fragmentHost.arbitraryAttributes = new HashMap<>();

			attibutes.remove("bundle-version");
			attibutes.remove("extension:");

			for (Entry<String,String> a : attibutes.entrySet()) {

				// we ignore directives
				if (!a.getKey().endsWith(":")) {

					dto.fragmentHost.arbitraryAttributes.put(a.getKey(), a.getValue());
				}
			}
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		if (dto.fragmentHost != null) {

			dto.fragmentHost.arbitraryAttributes = replaceNull(dto.fragmentHost.arbitraryAttributes);

			if (dto.fragmentHost.bsn == null) {

				error("the fragment host clause does not declare a bundle synmbolic name");
			}

			if (dto.fragmentHost.bundleVersion == null) {

				error("the fragment host clause does not declare a bundle version");
			}

			String rError = checkRange(dto.fragmentHost.bundleVersion);

			if (rError != null) {

				error("the fragment host clause does not declare a valid bundle version: " + rError);
			}

			if (dto.fragmentHost.extension == null) {

				error("the fragment host clause does not declare an extension type");

			}
		}
	}
}
