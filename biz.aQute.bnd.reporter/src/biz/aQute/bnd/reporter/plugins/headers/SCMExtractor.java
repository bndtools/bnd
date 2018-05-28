package biz.aQute.bnd.reporter.plugins.headers;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import biz.aQute.bnd.reporter.plugins.headers.dto.ScmDTO;

public class SCMExtractor extends HeaderExtractor {
	
	final private static String HEADER_TAG = "bundleScm";
	
	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		final Parameters header = manifest.getHeader(Constants.BUNDLE_SCM, false);
		
		final String scm = header.toString();
		if (scm.length() > 0) {
			final ScmDTO scmDto = new ScmDTO();
			final Attrs attrs = OSGiHeader.parseProperties(scm);
			
			for (final String key : attrs.keySet()) {
				if (key.equals("url")) {
					scmDto.url = attrs.get(key);
				} else if (key.equals("connection")) {
					scmDto.connection = attrs.get(key);
				} else if (key.equals("developerConnection")) {
					scmDto.developerConnection = attrs.get(key);
				} else if (key.equals("tag")) {
					scmDto.tag = attrs.get(key);
				}
			}
			result = scmDto;
		}
		return result;
	}
	
	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
