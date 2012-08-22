package org.osgi.service.indexer.impl;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.osgi.framework.Version;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.indexer.Builder;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Namespaces;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.impl.types.VersionKey;
import org.osgi.service.indexer.impl.types.VersionRange;
import org.osgi.service.log.LogService;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SCRAnalyzer implements ResourceAnalyzer {
	
	public static final String NS_1_0 = "http://www.osgi.org/xmlns/scr/v1.0.0";
	public static final String NS_1_1 = "http://www.osgi.org/xmlns/scr/v1.1.0";
	public static final String NS_1_2 = "http://www.osgi.org/xmlns/scr/v1.2.0";
	
	private LogService log;

	public SCRAnalyzer(LogService log) {
		this.log = log;
	}

	public void analyzeResource(Resource resource, List<Capability> caps, List<Requirement> reqs) throws Exception {
		String header = resource.getManifest().getMainAttributes().getValue(ComponentConstants.SERVICE_COMPONENT);
		if (header == null)
			return;
		
		StringTokenizer tokenizer = new StringTokenizer(header, ",");
		Version highest = null;
		while (tokenizer.hasMoreTokens()) {
			String pattern = tokenizer.nextToken().trim();
			List<String> paths = Util.findMatchingPaths(resource, pattern);
			if (paths != null) for (String path : paths) {
				Version version = processScrXml(resource, path);
				if (highest == null || (version.compareTo(highest) > 0))
					highest = version;
			}
		}

		if (highest!= null) {
			Requirement requirement = createRequirement(new VersionRange("[" + highest + ",2.0)"));
			reqs.add(requirement);
		}
	}
	
	private Version processScrXml(Resource resource, String path) throws IOException {
		Resource childResource = resource.getChild(path);
		if (childResource == null) {
			if (log != null) log.log(LogService.LOG_WARNING, MessageFormat.format("Cannot analyse SCR requirement version: resource {0} does not contain path {1} referred from Service-Component header.", resource.getLocation(), path));
			return null;
		}
		
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		
		try {
			SAXParser parser = spf.newSAXParser();
			SCRContentHandler handler = new SCRContentHandler();
			parser.parse(childResource.getStream(), handler);
			
			return handler.highest;
		} catch (Exception e) {
			if (log != null)
				log.log(LogService.LOG_ERROR, MessageFormat.format("Processing error: failed to parse child resource {0} in resource {1}.", path, resource.getLocation()), e);
			return null;
		}
	}

	private static Requirement createRequirement(VersionRange range) {
		Builder builder = new Builder().setNamespace(Namespaces.NS_EXTENDER);
		
		StringBuilder filter = new StringBuilder();
		filter.append('(').append(Namespaces.NS_EXTENDER).append('=').append(Namespaces.EXTENDER_SCR).append(')');
		
		filter.insert(0,  "(&");
		Util.addVersionFilter(filter, range, VersionKey.PackageVersion);
		filter.append(')');
		
		builder.addDirective(Namespaces.DIRECTIVE_FILTER, filter.toString());
		Requirement requirement = builder.buildRequirement();
		return requirement;
	}

	private static class SCRContentHandler extends DefaultHandler {
		
		private Version highest = null;
		private boolean beforeRoot = true;
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attribs) throws SAXException {
			if (uri == null || "".equals(uri)) {
				if (beforeRoot) {
					beforeRoot = false;
					setVersion(new Version(1,0,0));
				}
			} else {
				if (NS_1_2.equals(uri))
					setVersion(new Version(1,2,0));
				else if (NS_1_1.equals(uri))
					setVersion(new Version(1,1,0));
				else if (NS_1_0.equals(uri))
					setVersion(new Version(1,2,0));
			}
		}
		
		private void setVersion(Version version) {
			if (highest == null || (version.compareTo(highest) > 0))
				highest = version;
		}
	}
	
}
