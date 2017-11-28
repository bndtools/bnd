package biz.aQute.bnd.reporter.plugins.headers;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Attrs.Type;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import biz.aQute.bnd.reporter.plugins.headers.dto.RequireCapabilityDTO;
import biz.aQute.bnd.reporter.plugins.headers.dto.TypedAttributeDTO;

public class RequiredCapabilityExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "requireCapabilities";

	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		final Parameters header = manifest.getHeader(Constants.REQUIRE_CAPABILITY, false);
		final List<RequireCapabilityDTO> capas = new LinkedList<>();
		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final RequireCapabilityDTO capa = new RequireCapabilityDTO();

			capa.namespace = cleanKey(entry.getKey());

			if (entry.getValue().containsKey("filter:")) {
				capa.filter = entry.getValue().get("filter:");
			}

			if (entry.getValue().containsKey("resolution:")) {
				capa.resolution = entry.getValue().get("resolution:");
			}

			if (entry.getValue().containsKey("cardinality:")) {
				capa.cardinality = entry.getValue().get("cardinality:");
			}

			if (entry.getValue().containsKey("effective:")) {
				capa.effective = entry.getValue().get("effective:");
			}

			final Attrs attribute = new Attrs(entry.getValue());
			for (final Entry<String, String> a : attribute.entrySet()) {
				if (!a.getKey().endsWith(":")) {
					final Object val = attribute.getTyped(a.getKey());
					final TypedAttributeDTO ta = new TypedAttributeDTO();

					ta.name = a.getKey();

					if (attribute.getType(a.getKey()) == Type.DOUBLE) {
						ta.values.add(val.toString());
						ta.type = "Double";
					} else if (attribute.getType(a.getKey()) == Type.LONG) {
						ta.values.add(val.toString());
						ta.type = "Long";
					} else if (attribute.getType(a.getKey()) == Type.STRING) {
						ta.values.add(val.toString());
						ta.type = "String";
					} else if (attribute.getType(a.getKey()) == Type.VERSION) {
						ta.values.add(val.toString());
						ta.type = "Version";
					} else if (attribute.getType(a.getKey()) == Type.DOUBLES) {
						for (final Object v : (Collection<?>) val) {
							ta.values.add(v.toString());
						}
						ta.type = "Double";
						ta.multiValue = true;
					} else if (attribute.getType(a.getKey()) == Type.LONGS) {
						for (final Object v : (Collection<?>) val) {
							ta.values.add(v.toString());
						}
						ta.type = "Long";
						ta.multiValue = true;
					} else if (attribute.getType(a.getKey()) == Type.STRINGS) {
						for (final Object v : (Collection<?>) val) {
							ta.values.add(v.toString());
						}
						ta.type = "String";
						ta.multiValue = true;
					} else {
						for (final Object v : (Collection<?>) val) {
							ta.values.add(v.toString());
						}
						ta.type = "Version";
						ta.multiValue = true;
					}

					capa.typedAttributes.add(ta);
				}
			}
			capas.add(capa);
		}
		if (!capas.isEmpty()) {
			result = capas;
		}
		return result;
	}

	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
