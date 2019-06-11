package biz.aQute.bnd.reporter.plugins.resource.converter;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.jar.Manifest;

import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import biz.aQute.bnd.reporter.service.resource.converter.ResourceConverterPlugin;

public class ManifestConverterPlugin implements ResourceConverterPlugin {

	static private final String[] _ext = {
		"mf"
	};

	@Override
	public String[] getHandledExtensions() {
		return _ext;
	}

	@Override
	public Object extract(final InputStream input) throws Exception {
		Objects.requireNonNull(input, "input");

		final Map<String, Object> manifestDto = new LinkedHashMap<>();

		final Manifest manifest = new Manifest(input);
		for (final Object key : manifest.getMainAttributes()
			.keySet()) {
			final Parameters param = new Parameters(true);
			OSGiHeader.parseHeader(manifest.getMainAttributes()
				.getValue(key.toString()), null, param);

			final List<Map<String, Object>> clauses = new LinkedList<>();
			for (final Entry<String, ? extends Map<String, String>> entry : param.asMapMap()
				.entrySet()) {
				String clauseValue = entry.getKey();
				while (clauseValue.endsWith("~")) {
					clauseValue = clauseValue.substring(0, clauseValue.length() - 1);
				}

				final Map<String, Object> clause = new LinkedHashMap<>();
				clause.put("value", clauseValue);
				clauses.add(clause);

				final List<Map<String, Object>> attributes = new LinkedList<>();
				final List<Map<String, Object>> directives = new LinkedList<>();
				for (final Entry<String, String> entryAttr : entry.getValue()
					.entrySet()) {
					String attrKey = entryAttr.getKey();
					while (attrKey.endsWith("~")) {
						attrKey = attrKey.substring(0, attrKey.length() - 1);
					}

					if (attrKey.endsWith(":")) {
						attrKey = attrKey.substring(0, attrKey.length() - 1);
						final Map<String, Object> d = new LinkedHashMap<>();
						d.put("key", attrKey);
						d.put("value", entryAttr.getValue());
						directives.add(d);
					} else {
						final Map<String, Object> a = new LinkedHashMap<>();
						a.put("key", attrKey);
						a.put("value", entryAttr.getValue());
						attributes.add(a);
					}
				}
				if (!attributes.isEmpty()) {
					clause.put("attributes", attributes);
				}
				if (!directives.isEmpty()) {
					clause.put("directives", directives);
				}
			}

			if (!clauses.isEmpty()) {
				manifestDto.put(key.toString(), Collections.singletonMap("clauses", clauses));
			}
		}
		if (!manifestDto.isEmpty()) {
			return manifestDto;
		} else {
			return null;
		}
	}
}
