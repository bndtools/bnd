package biz.aQute.bnd.reporter.plugins.headers;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import biz.aQute.bnd.reporter.plugins.headers.dto.DeveloperDTO;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

public class DeveloperExtractor extends HeaderExtractor {
	
	final private static String HEADER_TAG = "bundleDevelopers";
	
	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		final Parameters header = manifest.getHeader(Constants.BUNDLE_DEVELOPERS, false);
		final List<DeveloperDTO> developers = new LinkedList<>();
		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final DeveloperDTO developer = new DeveloperDTO();
			
			developer.identifier = cleanKey(entry.getKey());
			
			if (entry.getValue().containsKey("email")) {
				developer.email = entry.getValue().get("email");
			}
			
			if (entry.getValue().containsKey("name")) {
				developer.name = entry.getValue().get("name");
			}
			
			if (entry.getValue().containsKey("organization")) {
				developer.organization = entry.getValue().get("organization");
			}
			
			if (entry.getValue().containsKey("organizationUrl")) {
				developer.organizationUrl = entry.getValue().get("organizationUrl");
			}
			
			if (entry.getValue().containsKey("timezone")) {
				developer.timezone = Integer.valueOf(entry.getValue().get("timezone"));
			}
			
			if (entry.getValue().containsKey("roles")) {
				for (final String role : entry.getValue().get("roles").split(",")) {
					developer.roles.add(role.trim());
				}
			}
			developers.add(developer);
		}
		if (!developers.isEmpty()) {
			result = developers;
		}
		return result;
	}
	
	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
