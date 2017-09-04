package aQute.bnd.metadata;

import java.util.Map;

import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class ActivatorExtractor extends HeaderExtractor {

	public ActivatorExtractor() {
		super(Constants.BUNDLE_ACTIVATOR, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {
			dto.activator = header.keySet().iterator().next();
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {
		// Nothing to do
	}
}
