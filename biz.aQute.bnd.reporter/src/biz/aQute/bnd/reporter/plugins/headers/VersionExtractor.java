package biz.aQute.bnd.reporter.plugins.headers;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import biz.aQute.bnd.reporter.plugins.headers.dto.VersionDTO;

public class VersionExtractor extends HeaderExtractor {
	
	final private static String HEADER_TAG = "bundleVersion";
	
	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		final Parameters header = manifest.getHeader(Constants.BUNDLE_VERSION, false);
		if (!header.isEmpty()) {
			final VersionDTO version = toVersion(cleanKey(header.keySet().iterator().next()));
			if (version == null) {
				result = getDefaultVersion();
			} else {
				result = version;
			}
		} else {
			result = getDefaultVersion();
		}
		return result;
	}
	
	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
