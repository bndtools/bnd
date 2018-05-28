package biz.aQute.bnd.reporter.plugins.headers;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;

public class NameExtractor extends HeaderExtractor {
	
	final private static String HEADER_TAG = "bundleName";
	
	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		
		Object result = null;
		final String name = manifest.getHeaderAsString(Constants.BUNDLE_NAME);
		if (!name.isEmpty()) {
			result = name;
		}
		return result;
	}
	
	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
