package aQute.bnd.metadata;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.metadata.dto.NativeCodeDTO;
import aQute.bnd.metadata.dto.NativeCodeEntryDTO;
import aQute.bnd.metadata.dto.VersionRangeDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class NativeCodeExtractor extends HeaderExtractor {

	public NativeCodeExtractor() {
		super(Constants.BUNDLE_NATIVECODE, true);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {

			dto.nativeCode = new NativeCodeDTO();
			dto.nativeCode.entries = new LinkedList<>();

			Map<Attrs,NativeCodeEntryDTO> map = new HashMap<>();

			for (Entry<String,Attrs> entry : header.entrySet()) {

				if (entry.getKey().equals("*")) {

					dto.nativeCode.optional = true;

				} else {

					NativeCodeEntryDTO nativeCode = map.get(entry.getValue());

					if (nativeCode == null) {

						nativeCode = new NativeCodeEntryDTO();
						nativeCode.paths = new LinkedList<>();
						nativeCode.languages = new LinkedList<>();
						nativeCode.osnames = new LinkedList<>();
						nativeCode.osversions = new LinkedList<>();
						nativeCode.processors = new LinkedList<>();
						nativeCode.selectionFilters = new LinkedList<>();

						map.put(entry.getValue(), nativeCode);

						String key = "osname";
						while (entry.getValue().get(key) != null) {

							nativeCode.osnames.add(entry.getValue().get(key));
							key = key + "~";
						}

						key = "language";
						while (entry.getValue().get(key) != null) {

							nativeCode.languages.add(entry.getValue().get(key));
							key = key + "~";
						}

						key = "processor";
						while (entry.getValue().get(key) != null) {

							nativeCode.processors.add(entry.getValue().get(key));
							key = key + "~";
						}

						key = "selection-filter";
						while (entry.getValue().get(key) != null) {

							nativeCode.selectionFilters.add(entry.getValue().get(key));
							key = key + "~";
						}

						key = "osversion";
						while (entry.getValue().get(key) != null) {

							VersionRangeDTO rangedto = toOsgiRange(entry.getValue().get(key, ""));

							if (rangedto != null) {

								nativeCode.osversions.add(rangedto);
							}

							key = key + "~";
						}

						nativeCode.paths.add(cleanKey(entry.getKey()));

						dto.nativeCode.entries.add(nativeCode);

					} else {

						nativeCode.paths.add(cleanKey(entry.getKey()));
					}
				}
			}

			if (dto.nativeCode.optional == null) {

				dto.nativeCode.optional = false;
			}
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		if (dto.nativeCode != null) {

			dto.nativeCode.entries = replaceNull(dto.nativeCode.entries);

			if (dto.nativeCode.entries.isEmpty()) {

				error("the native code does not declare entries");
			}

			if (dto.nativeCode.optional == null) {

				error("the native code does not declare if it is optional");
			}

			for (NativeCodeEntryDTO e : dto.nativeCode.entries) {

				e.languages = replaceNull(e.languages);
				e.osnames = replaceNull(e.osnames);
				e.osversions = replaceNull(e.osversions);
				e.processors = replaceNull(e.processors);
				e.selectionFilters = replaceNull(e.selectionFilters);
				e.paths = replaceNull(e.paths);

				if (e.paths.isEmpty()) {

					error("the native code entry does not declare paths: entry index = "
							+ dto.nativeCode.entries.indexOf(e));
				}
			}
		}
	}
}
