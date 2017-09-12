package aQute.bnd.metadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Attrs.Type;
import aQute.bnd.header.Parameters;
import aQute.bnd.metadata.dto.ManifestHeadersDTO;
import aQute.bnd.metadata.dto.ProvideCapabilityDTO;
import aQute.bnd.metadata.dto.TypedPropertyDTO;
import aQute.bnd.metadata.dto.VersionDTO;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class ProvideCapabilityExtractor extends HeaderExtractor {

	public ProvideCapabilityExtractor() {
		super(Constants.PROVIDE_CAPABILITY, false);

	}

	@Override
	public void extract(ManifestHeadersDTO dto, Parameters header, Map<String,Parameters> localizedheaders, Jar jar) {

		if (header != null) {

			dto.provideCapabilities = new LinkedList<>();

			for (Entry<String,Attrs> entry : header.entrySet()) {

				ProvideCapabilityDTO pcap = new ProvideCapabilityDTO();

				pcap.nameSpace = cleanKey(entry.getKey());

				pcap.uses = new LinkedList<>();

				if (entry.getValue().get("uses:") != null) {

					for (String c : entry.getValue().get("uses:").split(",")) {

						pcap.uses.add(c.trim());
					}
				}

				pcap.effective = entry.getValue().get("effective:");

				if (pcap.effective == null) {

					pcap.effective = "resolve";
				}

				pcap.arbitraryDirectives = new HashMap<>();

				Attrs attribute = new Attrs(entry.getValue());
				attribute.remove("uses:");
				attribute.remove("effective:");

				for (Entry<String,String> a : attribute.entrySet()) {

					if (a.getKey().endsWith(":")) {

						pcap.arbitraryDirectives.put(a.getKey().substring(0, a.getKey().length() - 1), a.getValue());
					}
				}

				pcap.typedAttributes = new LinkedList<>();

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

							pcap.typedAttributes.add(prop);
						}
					}
				}

				dto.provideCapabilities.add(pcap);
			}
		}
	}

	@Override
	public void verify(ManifestHeadersDTO dto) throws Exception {

		dto.provideCapabilities = replaceNull(dto.provideCapabilities);

		for (ProvideCapabilityDTO e : dto.provideCapabilities) {

			e.arbitraryDirectives = replaceNull(e.arbitraryDirectives);

			if (e.effective == null) {

				error("the provide capability clause does not declare when it will be effective: clause index = "
						+ dto.provideCapabilities.indexOf(e));
			}

			if (e.nameSpace == null) {

				error("the provide capability clause does not declare a name space: clause index = "
						+ dto.provideCapabilities.indexOf(e));
			}

			e.typedAttributes = replaceNull(e.typedAttributes);

			for (TypedPropertyDTO p : e.typedAttributes) {

				if (p.name == null) {

					error("the provide capability clause does not declare a property name: property index = "
							+ e.typedAttributes.indexOf(p) + ",clause index = " + dto.provideCapabilities.indexOf(e));
				}

				if (p.multipleValues == null) {

					error("the provide capability clause does not declare a property cardinality: property index = "
							+ e.typedAttributes.indexOf(p) + ",clause index = " + dto.provideCapabilities.indexOf(e));
				}

				if (p.type == null) {

					error("the provide capability clause does not declare a property type: property index = "
							+ e.typedAttributes.indexOf(p) + ",clause index = " + dto.provideCapabilities.indexOf(e));
				}

				p.values = replaceNull(p.values);

				for (Object v : p.values) {

					if (v instanceof VersionDTO) {

						if (((VersionDTO) v).major == null) {

							error("the provide capability clause does not declare a valid property version: property index = "
									+ e.typedAttributes.indexOf(p) + ",clause index = "
									+ dto.provideCapabilities.indexOf(e));
						}
					}
				}

				if (!p.multipleValues && p.values.size() > 1) {

					error("the provide capability clause does not declare a valid number of property values: property index = "
							+ e.typedAttributes.indexOf(p) + ",clause index = " + dto.provideCapabilities.indexOf(e));
				}
			}

			e.uses = replaceNull(e.uses);
		}
	}
}
