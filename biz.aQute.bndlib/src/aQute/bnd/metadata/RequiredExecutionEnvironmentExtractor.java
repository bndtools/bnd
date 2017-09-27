package aQute.bnd.metadata;

import java.util.LinkedList;
import java.util.Map;

import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class RequiredExecutionEnvironmentExtractor extends HeaderExtractor {

	public RequiredExecutionEnvironmentExtractor() {
		super(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, true);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {

			dto.requiredExecutionEnvironments = new LinkedList<>();
			dto.requiredExecutionEnvironments.addAll(cleanKey(header.keySet()));
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		dto.requiredExecutionEnvironments = replaceNull(dto.requiredExecutionEnvironments);
	}
}
