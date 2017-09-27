package aQute.bnd.metadata;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.ExportPackageDTO;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class ExportExtractor extends HeaderExtractor {

	public ExportExtractor() {
		super(Constants.EXPORT_PACKAGE, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {

			dto.exportPackages = new LinkedList<>();

			for (Entry<String,Attrs> entry : header.entrySet()) {

				ExportPackageDTO exp = new ExportPackageDTO();

				exp.packageName = cleanKey(entry.getKey());

				exp.version = toVersion(entry.getValue().get("version", "bad"));
				
				if (exp.version == null) {

					exp.version = getDefaultVersion();
				}

				exp.exclude = new LinkedList<>();

				if (entry.getValue().get("exclude:") != null) {

					for (String c : entry.getValue().get("exclude:").split(",")) {

						exp.exclude.add(c.trim());
					}
				}

				exp.include = new LinkedList<>();

				if (entry.getValue().get("include:") != null) {

					for (String c : entry.getValue().get("include:").split(",")) {

						exp.include.add(c.trim());
					}
				}

				exp.mandatory = new LinkedList<>();

				if (entry.getValue().get("mandatory:") != null) {

					for (String c : entry.getValue().get("mandatory:").split(",")) {

						exp.mandatory.add(c.trim());
					}
				}

				exp.uses = new LinkedList<>();

				if (entry.getValue().get("uses:") != null) {

					for (String c : entry.getValue().get("uses:").split(",")) {

						exp.uses.add(c.trim());
					}
				}

				exp.arbitraryAttributes = new HashMap<>();

				Attrs attribute = new Attrs(entry.getValue());
				attribute.remove("version");
				attribute.remove("exclude:");
				attribute.remove("include:");
				attribute.remove("mandatory:");
				attribute.remove("uses:");

				for (Entry<String,String> a : attribute.entrySet()) {

					// we ignore directives
					if (!a.getKey().endsWith(":")) {

						exp.arbitraryAttributes.put(a.getKey(), a.getValue());
					}
				}

				dto.exportPackages.add(exp);
			}
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		dto.exportPackages = replaceNull(dto.exportPackages);

		for (ExportPackageDTO e : dto.exportPackages) {

			e.arbitraryAttributes = replaceNull(e.arbitraryAttributes);
			e.exclude = replaceNull(e.exclude);
			e.include = replaceNull(e.include);
			e.uses = replaceNull(e.uses);
			e.mandatory = replaceNull(e.mandatory);

			if (e.packageName == null) {

				error("the export clause does not declare a package name: clause index = "
						+ dto.exportPackages.indexOf(e));
			}

			if (e.version == null) {

				error("the export clause does not declare a version: clause index = " + dto.exportPackages.indexOf(e));

			}

			if (e.version.major == null) {

				error("the version of the export clause does not declare a major part: clause index = "
						+ dto.exportPackages.indexOf(e));
			}
		}
	}
}
