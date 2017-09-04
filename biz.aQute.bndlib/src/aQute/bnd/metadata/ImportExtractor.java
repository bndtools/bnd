package aQute.bnd.metadata;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.ImportPackageDTO;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class ImportExtractor extends HeaderExtractor {

	public ImportExtractor() {
		super(Constants.IMPORT_PACKAGE, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {

			dto.importPackages = new LinkedList<>();

			for (Entry<String,Attrs> entry : header.entrySet()) {

				ImportPackageDTO imp = new ImportPackageDTO();

				imp.packageName = entry.getKey();

				imp.resolution = entry.getValue().get("resolution:");

				if (imp.resolution == null) {

					imp.resolution = "mandatory";
				}

				imp.bsn = entry.getValue().get("bundle-symbolic-name");

				imp.version = toOsgiRange(entry.getValue().get("version", ""));

				if (imp.version == null) {

					imp.version = getDefaultRange();

				}

				imp.bundleVersion = toOsgiRange(entry.getValue().get("bundle-version", ""));

				if (imp.bundleVersion == null) {

					imp.bundleVersion = getDefaultRange();

				}

				imp.arbitraryAttributes = new HashMap<>();

				Attrs attribute = new Attrs(entry.getValue());

				attribute.remove("bundle-symbolic-name");
				attribute.remove("version");
				attribute.remove("bundle-version");
				attribute.remove("resolution:");

				for (Entry<String,String> a : attribute.entrySet()) {

					// we ignore directives
					if (!a.getKey().endsWith(":")) {

						imp.arbitraryAttributes.put(a.getKey(), a.getValue());
					}
				}

				dto.importPackages.add(imp);
			}
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		dto.importPackages = replaceNull(dto.importPackages);

		for (ImportPackageDTO e : dto.importPackages) {

			e.arbitraryAttributes = replaceNull(e.arbitraryAttributes);

			if (e.packageName == null) {

				error("the import clause does not declare a package name: clause index = "
						+ dto.importPackages.indexOf(e));
			}

			if (e.resolution == null) {

				error("the import clause does not declare a resolution: clause index = "
						+ dto.importPackages.indexOf(e));
			}

			if (e.bundleVersion == null) {

				error("the import clause does not declare a bundle version: clause index = "
						+ dto.importPackages.indexOf(e));
			}

			String rError = checkRange(e.bundleVersion);

			if (rError != null) {

				error("the bundle version of the import clause does not declare a valid range: " + rError
						+ ": clause index = " + dto.importPackages.indexOf(e));
			}

			if (e.version == null) {

				error("the import clause does not declare a version: clause index = " + dto.importPackages.indexOf(e));
			}

			rError = checkRange(e.version);

			if (rError != null) {

				error("the version of the import clause does not declare a valid range: " + rError + ": clause index = "
						+ dto.importPackages.indexOf(e));
			}
		}
	}
}
