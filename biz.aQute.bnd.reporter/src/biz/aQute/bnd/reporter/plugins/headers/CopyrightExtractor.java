package biz.aQute.bnd.reporter.plugins.headers;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;

public class CopyrightExtractor extends HeaderExtractor {
	
	final private static String HEADER_TAG = "bundleCopyright";
	
	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		
		Object result = null;
		final String copyright = manifest.getHeaderAsString(Constants.BUNDLE_COPYRIGHT);
		if (!copyright.isEmpty()) {
			result = copyright;
		}
		return result;
	}
	
	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
