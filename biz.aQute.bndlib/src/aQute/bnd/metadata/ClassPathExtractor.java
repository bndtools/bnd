package aQute.bnd.metadata;

import java.util.LinkedList;
import java.util.Map;

import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class ClassPathExtractor extends HeaderExtractor {

	public ClassPathExtractor() {
		super(Constants.BUNDLE_CLASSPATH, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		dto.classPaths = new LinkedList<>();

		if (header != null) {

			// we ignore parameters, no one specified in spec and no mention for arbitrary
			// attributes
			dto.classPaths.addAll(cleanKey(header.keySet()));

		} else {

			dto.classPaths.add(".");
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		dto.classPaths = replaceNull(dto.classPaths);

		if (dto.classPaths.isEmpty()) {

			error("the bundle does not declare a class path");
		}
	}
}
