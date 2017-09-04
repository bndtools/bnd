package aQute.bnd.metadata;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.Constants;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Attrs.Type;
import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.metadata.dto.RequireCapabilityDTO;
import aQute.bnd.metadata.dto.TypedPropertyDTO;
import aQute.bnd.metadata.dto.VersionDTO;
import aQute.bnd.osgi.Jar;

public class RequiredCapabilityExtractor extends HeaderExtractor {

	public RequiredCapabilityExtractor() {
		super(Constants.REQUIRE_CAPABILITY, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {

			dto.requireCapabilities = new LinkedList<>();

			for (Entry<String,Attrs> entry : header.entrySet()) {

				RequireCapabilityDTO rcap = new RequireCapabilityDTO();

				rcap.nameSpace = entry.getKey();

				rcap.filter = entry.getValue().get("filter:");
				rcap.resolution = entry.getValue().get("resolution:");

				if (rcap.resolution == null) {

					rcap.resolution = "mandatory";
				}

				rcap.cardinality = entry.getValue().get("cardinality:");

				if (rcap.cardinality == null) {

					rcap.cardinality = "single";
				}

				rcap.effective = entry.getValue().get("effective:");

				if (rcap.effective == null) {

					rcap.effective = "resolve";
				}


				Attrs attribute = new Attrs(entry.getValue());
				rcap.typedAttributes = new LinkedList<>();

				for (Entry<String,String> a : attribute.entrySet()) {

					if (!a.getKey().endsWith(":")) {

						TypedPropertyDTO prop = new TypedPropertyDTO();

						Object val = attribute.getTyped(a.getKey());

						prop.name = a.getKey();
						prop.values = new LinkedList<>();

						if (val != null) {

							if (attribute.getType(a.getKey()) == Type.DOUBLE) {

								prop.values.add(val.toString());
								prop.type = "Double";
								prop.multipleValues = false;

							} else if (attribute.getType(a.getKey()) == Type.LONG) {

								prop.values.add(val.toString());
								prop.type = "Long";
								prop.multipleValues = false;

							} else if (attribute.getType(a.getKey()) == Type.STRING) {

								prop.values.add(val.toString());
								prop.type = "String";
								prop.multipleValues = false;

							} else if (attribute.getType(a.getKey()) == Type.VERSION) {

								prop.values.add(val.toString());
								prop.type = "Version";
								prop.multipleValues = false;

							} else if (attribute.getType(a.getKey()) == Type.DOUBLES) {

								for (Object v : (Collection< ? >) val) {
									prop.values.add(v.toString());
								}
								prop.type = "Double";
								prop.multipleValues = true;

							} else if (attribute.getType(a.getKey()) == Type.LONGS) {

								for (Object v : (Collection< ? >) val) {
									prop.values.add(v.toString());
								}
								prop.type = "Long";
								prop.multipleValues = true;

							} else if (attribute.getType(a.getKey()) == Type.STRINGS) {

								for (Object v : (Collection< ? >) val) {
									prop.values.add(v.toString());
								}
								prop.type = "String";
								prop.multipleValues = true;

							} else if (attribute.getType(a.getKey()) == Type.VERSIONS) {

								for (Object v : (Collection< ? >) val) {
									prop.values.add(v.toString());
								}
								prop.type = "Version";
								prop.multipleValues = true;

							} else {
								prop.values.add(val.toString());
								prop.type = "String";
								prop.multipleValues = false;
							}

							rcap.typedAttributes.add(prop);
						}
					}
				}

				dto.requireCapabilities.add(rcap);
			}
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		dto.requireCapabilities = replaceNull(dto.requireCapabilities);

		for (RequireCapabilityDTO e : dto.requireCapabilities) {

			if (e.effective == null) {

				error("the require capability clause does not declare when it will be effective: clause index = "
						+ dto.requireCapabilities.indexOf(e));
			}

			if (e.nameSpace == null) {

				error("the require capability clause does not declare a name space: clause index = "
						+ dto.requireCapabilities.indexOf(e));
			}

			e.typedAttributes = replaceNull(e.typedAttributes);

			for (TypedPropertyDTO p : e.typedAttributes) {

				if (p.name == null) {

					error("the require capability clause does not declare a property name: property index = "
							+ e.typedAttributes.indexOf(p) + ",clause index = " + dto.requireCapabilities.indexOf(e));
				}

				if (p.multipleValues == null) {

					error("the require capability clause does not declare a property cardinality: property index = "
							+ e.typedAttributes.indexOf(p) + ",clause index = " + dto.requireCapabilities.indexOf(e));
				}

				if (p.type == null) {

					error("the require capability clause does not declare a property type: property index = "
							+ e.typedAttributes.indexOf(p) + ",clause index = " + dto.requireCapabilities.indexOf(e));
				}

				p.values = replaceNull(p.values);

				for (Object v : p.values) {

					if (v instanceof VersionDTO) {

						if (((VersionDTO) v).major == null) {

							error("the require capability clause does not declare a valid property version: property index = "
									+ e.typedAttributes.indexOf(p) + ",clause index = "
									+ dto.requireCapabilities.indexOf(e));
						}
					}
				}

				if (!p.multipleValues && p.values.size() > 1) {

					error("the require capability clause does not declare a valid number of property values: property index = "
							+ e.typedAttributes.indexOf(p) + ",clause index = " + dto.requireCapabilities.indexOf(e));
				}
			}

			if (e.resolution == null) {

				error("the require capability clause does not declare a resolution: clause index = "
						+ dto.requireCapabilities.indexOf(e));
			}

			if (e.cardinality == null) {

				error("the require capability clause does not declare a cardinality: clause index = "
						+ dto.requireCapabilities.indexOf(e));
			}
		}
	}
}
