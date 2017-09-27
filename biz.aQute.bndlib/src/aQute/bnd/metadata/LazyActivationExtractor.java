package aQute.bnd.metadata;

import java.util.LinkedList;
import java.util.Map;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.LazyActivationDTO;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class LazyActivationExtractor extends HeaderExtractor {

	public LazyActivationExtractor() {
		super(Constants.BUNDLE_ACTIVATIONPOLICY, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {

			Attrs attibutes = header.values().iterator().next();

			dto.lazyActivation = new LazyActivationDTO();

			dto.lazyActivation.exclude = new LinkedList<>();

			if (attibutes.get("exclude:") != null) {

				for (String a : attibutes.get("exclude:").split(",")) {

					dto.lazyActivation.exclude.add(a.trim());
				}
			}

			dto.lazyActivation.include = new LinkedList<>();

			if (attibutes.get("include:") != null) {

				for (String a : attibutes.get("include:").split(",")) {

					dto.lazyActivation.include.add(a.trim());
				}
			}

			if (dto.lazyActivation.include.isEmpty()) {

				dto.lazyActivation.include.addAll(jar.getPackages());
			}
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		if (dto.lazyActivation != null) {

			dto.lazyActivation.exclude = replaceNull(dto.lazyActivation.exclude);
			dto.lazyActivation.include = replaceNull(dto.lazyActivation.include);
		}
	}
}
