package biz.aQute.bnd.reporter.plugins.headers;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import biz.aQute.bnd.reporter.plugins.headers.dto.LicenseDTO;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

public class LicenseExtractor extends HeaderExtractor {
	
	final private static String HEADER_TAG = "bundleLicenses";
	
	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		final Parameters header = manifest.getHeader(Constants.BUNDLE_LICENSE, false);
		final List<LicenseDTO> licences = new LinkedList<>();
		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final LicenseDTO licence = new LicenseDTO();
			
			licence.name = cleanKey(entry.getKey());
			
			if (entry.getValue().containsKey("description")) {
				licence.description = entry.getValue().get("description");
			}
			
			if (entry.getValue().containsKey("link")) {
				licence.link = entry.getValue().get("link");
			}
			licences.add(licence);
		}
		if (!licences.isEmpty()) {
			result = licences;
		}
		return result;
	}
	
	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
