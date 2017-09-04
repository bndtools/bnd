package aQute.bnd.metadata;

import java.util.Map;

import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.metadata.dto.VersionDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.version.Version;

public class VersionExtractor extends HeaderExtractor {

	public VersionExtractor() {
		super(Constants.BUNDLE_VERSION, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {

			Version version = null;

			try {

				String v = header.keySet().iterator().next();

				if (v != null && !v.isEmpty()) {

					version = Version.parseVersion(v);
				}

			} catch (Exception expected) {

				// Nothing to do
			}

			if (version != null) {

				dto.version = new VersionDTO();
				dto.version.major = version.getMajor();
				dto.version.minor = version.getMinor();
				dto.version.micro = version.getMicro();
				dto.version.qualifier = version.getQualifier();

			} else {

				dto.version = new VersionDTO();
				dto.version.major = 0;
				dto.version.minor = 0;
				dto.version.micro = 0;
			}

		} else {

			dto.version = new VersionDTO();
			dto.version.major = 0;
			dto.version.minor = 0;
			dto.version.micro = 0;
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		if (dto.version == null) {

			error("the bundle does not declare a version");
		}

		if (dto.version.major == null) {

			error("the bundle version does not declare major part");
		}
	}
}
