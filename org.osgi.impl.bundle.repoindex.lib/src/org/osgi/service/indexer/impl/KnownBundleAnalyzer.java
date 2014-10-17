package org.osgi.service.indexer.impl;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.osgi.framework.Version;
import org.osgi.service.indexer.Builder;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.impl.types.SymbolicName;
import org.osgi.service.indexer.impl.types.VersionRange;
import org.osgi.service.indexer.impl.util.OSGiHeader;

public class KnownBundleAnalyzer implements ResourceAnalyzer {

	private final Properties defaultProperties;
	private Properties extraProperties = null;

	public KnownBundleAnalyzer() {
		defaultProperties = new Properties();
		InputStream stream = KnownBundleAnalyzer.class.getResourceAsStream("known-bundles.properties");
		if (stream != null) {
			try {
				defaultProperties.load(stream);
			} catch (IOException e) {
				// ignore
			} finally {
				try {
					stream.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public KnownBundleAnalyzer(Properties properties) {
		this.defaultProperties = properties;
	}

	public void setKnownBundlesExtra(Properties extras) {
		this.extraProperties = extras;
	}

	public void analyzeResource(Resource resource, List<Capability> caps, List<Requirement> reqs) throws Exception {
		SymbolicName resourceName;
		try {
			resourceName = Util.getSymbolicName(resource);
		} catch (IllegalArgumentException e) {
			// not a bundle, so return without analyzing
			return;
		}

		for (Enumeration<?> names = defaultProperties.propertyNames(); names.hasMoreElements();) {
			String propName = (String) names.nextElement();
			processPropertyName(resource, caps, reqs, resourceName, propName, defaultProperties);
		}

		if (extraProperties != null)
			for (Enumeration<?> names = extraProperties.propertyNames(); names.hasMoreElements();) {
				String propName = (String) names.nextElement();
				processPropertyName(resource, caps, reqs, resourceName, propName, extraProperties, defaultProperties);
			}
	}

	private static void processPropertyName(Resource resource, List<Capability> caps, List<Requirement> reqs, SymbolicName resourceName, String name, Properties... propertiesList)
			throws IOException {
		String[] bundleRef = name.split(";");
		String bsn = bundleRef[0];

		if (resourceName.getName().equals(bsn)) {
			VersionRange versionRange = null;
			if (bundleRef.length > 1)
				versionRange = new VersionRange(bundleRef[1]);

			Version version = Util.getVersion(resource);
			if (versionRange == null || versionRange.match(version)) {
				processClause(name, Util.readProcessedProperty(name, propertiesList), caps, reqs);
			}
		}
	}

	private static enum IndicatorType {
		Capability("cap="), Requirement("req=");

		String prefix;

		IndicatorType(String prefix) {
			this.prefix = prefix;
		}

		public String getPrefix() {
			return prefix;
		}
	}

	private static void processClause(String bundleRef, String clauseStr, List<Capability> caps, List<Requirement> reqs) {
		Map<String, Map<String, String>> header = OSGiHeader.parseHeader(clauseStr);

		for (Entry<String, Map<String, String>> entry : header.entrySet()) {
			String indicator = OSGiHeader.removeDuplicateMarker(entry.getKey());
			IndicatorType type;

			String namespace;
			if (indicator.startsWith(IndicatorType.Capability.getPrefix())) {
				type = IndicatorType.Capability;
				namespace = indicator.substring(IndicatorType.Capability.getPrefix().length());
			} else if (indicator.startsWith(IndicatorType.Requirement.getPrefix())) {
				type = IndicatorType.Requirement;
				namespace = indicator.substring(IndicatorType.Requirement.getPrefix().length());
			} else {
				throw new IllegalArgumentException(MessageFormat.format(
						"Invalid indicator format in known-bundle parsing for bundle  \"{0}\", expected cap=namespace or req=namespace, found \"{1}\".", bundleRef, indicator));
			}

			Builder builder = new Builder().setNamespace(namespace);

			Map<String, String> attribs = entry.getValue();
			Util.copyAttribsToBuilder(builder, attribs);

			if (type == IndicatorType.Capability)
				caps.add(builder.buildCapability());
			else if (type == IndicatorType.Requirement)
				reqs.add(builder.buildRequirement());
		}
	}

}
