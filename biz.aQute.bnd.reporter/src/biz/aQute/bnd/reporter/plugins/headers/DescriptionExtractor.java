package biz.aQute.bnd.reporter.plugins.headers;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;

public class DescriptionExtractor extends HeaderExtractor {
	
	final private static String HEADER_TAG = "bundleDescription";
	
	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		
		Object result = null;
		final String description = manifest.getHeaderAsString(Constants.BUNDLE_DESCRIPTION);
		if (!description.isEmpty()) {
			result = description;
		}
		return result;
	}
	
	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
