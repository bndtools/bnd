package aQute.bnd.metadata;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.DynamicImportPackageDTO;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class DynamicImportExtractor extends HeaderExtractor {

	public DynamicImportExtractor() {
		super(Constants.DYNAMICIMPORT_PACKAGE, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {

			dto.dynamicImportPackages = new LinkedList<>();

			for (Entry<String,Attrs> entry : header.entrySet()) {

				DynamicImportPackageDTO imp = new DynamicImportPackageDTO();

				imp.packageName = cleanKey(entry.getKey());

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

				for (Entry<String,String> a : attribute.entrySet()) {

					// we ignore directives
					if (!a.getKey().endsWith(":")) {

						imp.arbitraryAttributes.put(a.getKey(), a.getValue());
					}
				}

				dto.dynamicImportPackages.add(imp);
			}
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		dto.dynamicImportPackages = replaceNull(dto.dynamicImportPackages);

		for (DynamicImportPackageDTO e : dto.dynamicImportPackages) {

			if (e.bundleVersion == null) {

				error("the dynamic import clause does not declare a bundle version: clause index = "
						+ dto.dynamicImportPackages.indexOf(e));
			}

			String rError = checkRange(e.bundleVersion);

			if (rError != null) {

				error("the bundle version of the dynamic import clause does not declare a valid range: " + rError
						+ ": clause index = " + dto.dynamicImportPackages.indexOf(e));
			}

			if (e.version == null) {

				error("the dynamic import clause does not declare a version: clause index = "
						+ dto.dynamicImportPackages.indexOf(e));
			}

			rError = checkRange(e.version);

			if (rError != null) {

				error("the version of the dynamic import clause does not declare a valid range: " + rError
						+ ": clause index = " + dto.dynamicImportPackages.indexOf(e));
			}

			e.arbitraryAttributes = replaceNull(e.arbitraryAttributes);

			if (e.packageName == null) {

				error("the dynamic import clause does not declare a package name: clause index = "
						+ dto.dynamicImportPackages.indexOf(e));
			}
		}
	}
}
