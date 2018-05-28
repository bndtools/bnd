package biz.aQute.bnd.reporter.plugins.headers;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;

public class DocUrlExtractor extends HeaderExtractor {
	
	final private static String HEADER_TAG = "bundleDocUrl";
	
	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		
		Object result = null;
		final String docUrl = manifest.getHeaderAsString(Constants.BUNDLE_DOCURL);
		if (!docUrl.isEmpty()) {
			result = docUrl;
		}
		return result;
	}
	
	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
