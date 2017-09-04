package aQute.bnd.metadata;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.Manifest;

import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.BundleMetadataDTO;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;

class ManifestExtractor extends MetadataExtractor {

	final private static String DEFAULT_LOCALIZATION_BASE = "OSGI-INF/l10n/bundle";

	@Override
	final public void extract(BundleMetadataDTO dto, Jar jar) {

		Manifest manifest = null;

		try {
			manifest = jar.getManifest();
		} catch (Exception e) {
			// Nothing to do, checked in verify method
		}

		if (manifest != null) {

			Map<String,Map<String,String>> manifestLocalization = getManifestLocalization(manifest, jar);
			dto.manifestHeaders = new ManifestHeadersDTO();
			dto.manifestHeaders.localizations = new HashMap<>();

			for (HeaderExtractor e : Extractors.HEADERS_EXTRACTORS) {
				Map<String,Parameters> headers = getHeader(e.getName(), e.allowDuplicateAttribute(), manifest,
						manifestLocalization);

				e.extract(dto.manifestHeaders, headers.remove(""), headers, jar);
			}
		}
	}

	@Override
	final public void verify(BundleMetadataDTO dto) throws Exception {

		if (dto.manifestHeaders == null) {

			error("the bundle does not declare a manifest");
		}

		for (HeaderExtractor e : Extractors.HEADERS_EXTRACTORS) {
			e.verify(dto.manifestHeaders);
		}
	}

	private Map<String,Parameters> getHeader(String headerName, boolean allowDuplicateAttributes, Manifest manifest,
			Map<String,Map<String,String>> localization) {

		Map<String,Parameters> result = new HashMap<>();

		String headerString = manifest.getMainAttributes().getValue(headerName);

		if (headerString != null) {
			if (headerString.startsWith("%")) {
				headerString = headerString.substring(1);

				for (String language : localization.keySet()) {
					if (localization.get(language) != null) {
						Parameters headerValue = new Parameters(allowDuplicateAttributes);
						OSGiHeader.parseHeader(localization.get(language).get(headerString), null, headerValue);
						if (!headerValue.isEmpty()) {
							result.put(language, headerValue);
						}
					}
				}
			} else {
				Parameters headerValue = new Parameters(allowDuplicateAttributes);
				OSGiHeader.parseHeader(headerString, null, headerValue);
				if (!headerValue.isEmpty()) {
					result.put("", headerValue);
				}
			}
		}

		return result;
	}

	private Map<String,Map<String,String>> getManifestLocalization(Manifest manifest, Jar jar) {

		String path = manifest.getMainAttributes().getValue(Constants.BUNDLE_LOCALIZATION);

		if (path == null) {

			path = DEFAULT_LOCALIZATION_BASE;
		}

		Map<String,Map<String,String>> result = new HashMap<>();

		for (Entry<String,Resource> entry : jar.getResources().entrySet()) {

			if (entry.getKey().startsWith(path)) {

				String lang = entry.getKey().substring(path.length()).replaceFirst("\\..*", "");

				if (lang.startsWith("_")) {

					lang = lang.substring(1);
				}

				try (InputStream inProp = entry.getValue().openInputStream()) {

					Properties prop = new Properties();

					prop.load(inProp);

					Map<String,String> properties = new HashMap<>();

					for (String key : prop.stringPropertyNames()) {

						properties.put(key, prop.getProperty(key));
					}

					result.put(lang, properties);

				} catch (Exception e) {

					throw new RuntimeException(e);
				}
			}
		}

		return result;
	}
}
