package biz.aQute.bnd.reporter.plugins.headers;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;

public class ActivatorExtractor extends HeaderExtractor {
	
	final private static String HEADER_TAG = "bundleActivator";
	
	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		final Parameters header = manifest.getHeader(Constants.BUNDLE_ACTIVATOR, false);
		if (!header.isEmpty()) {
			result = cleanKey(header.keySet().iterator().next());
		}
		return result;
	}
	
	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
