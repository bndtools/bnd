package aQute.bnd.metadata;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.metadata.dto.RequireBundleDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class RequireBundleExtractor extends HeaderExtractor {

	public RequireBundleExtractor() {
		super(Constants.REQUIRE_BUNDLE, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {

			dto.requireBundles = new LinkedList<>();

			for (Entry<String,Attrs> entry : header.entrySet()) {

				RequireBundleDTO imp = new RequireBundleDTO();

				imp.bsn = entry.getKey();

				imp.resolution = entry.getValue().get("resolution:");

				if (imp.resolution == null) {

					imp.resolution = "mandatory";
				}

				imp.visibility = entry.getValue().get("visibility:");

				if (imp.visibility == null) {

					imp.visibility = "private";
				}

				imp.bundleVersion = toOsgiRange(entry.getValue().get("bundle-version", ""));

				if (imp.bundleVersion == null) {

					imp.bundleVersion = getDefaultRange();

				}

				imp.arbitraryAttributes = new HashMap<>();

				Attrs attribute = new Attrs(entry.getValue());

				attribute.remove("bundle-version");
				attribute.remove("resolution:");
				attribute.remove("visibility:");

				for (Entry<String,String> a : attribute.entrySet()) {

					// we ignore directives
					if (!a.getKey().endsWith(":")) {

						imp.arbitraryAttributes.put(a.getKey(), a.getValue());
					}
				}

				dto.requireBundles.add(imp);
			}
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		dto.requireBundles = replaceNull(dto.requireBundles);

		for (RequireBundleDTO e : dto.requireBundles) {

			if (e.bsn == null) {

				error("the require bundle clause does not declare a bundle symbolic name: clause index = "
						+ dto.requireBundles.indexOf(e));
			}

			if (e.bundleVersion == null) {

				error("the require bundle clause does not declare a bundle version: clause index = "
						+ dto.requireBundles.indexOf(e));
			}

			String rError = checkRange(e.bundleVersion);

			if (rError != null) {

				error("the bundle version of the require bundle clause does not declare a valid range: " + rError
						+ ": clause index = " + dto.requireBundles.indexOf(e));
			}

			if (e.resolution == null) {

				error("the require bundle clause does not declare a resolution: clause index = "
						+ dto.requireBundles.indexOf(e));
			}

			if (e.visibility == null) {

				error("the require bundle clause does not declare its transitivity: clause index = "
						+ dto.requireBundles.indexOf(e));
			}

			e.arbitraryAttributes = replaceNull(e.arbitraryAttributes);
		}
	}
}
