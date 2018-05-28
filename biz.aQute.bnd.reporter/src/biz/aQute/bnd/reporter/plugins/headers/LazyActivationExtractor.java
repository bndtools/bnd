package biz.aQute.bnd.reporter.plugins.headers;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import biz.aQute.bnd.reporter.plugins.headers.dto.ActivationPolicyDTO;

public class LazyActivationExtractor extends HeaderExtractor {
	
	final private static String HEADER_TAG = "bundleActivationPolicy";
	
	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		final Parameters header = manifest.getHeader(Constants.BUNDLE_ACTIVATIONPOLICY, false);
		if (!header.isEmpty()) {
			final Attrs attributes = header.values().iterator().next();
			final ActivationPolicyDTO act = new ActivationPolicyDTO();
			
			act.policy = "lazy";
			
			if (attributes.containsKey("exclude:")) {
				for (final String a : attributes.get("exclude:").split(",")) {
					act.excludes.add(a.trim());
				}
			}
			
			if (attributes.containsKey("include:")) {
				for (final String a : attributes.get("include:").split(",")) {
					act.includes.add(a.trim());
				}
			} else {
				for (final String a : jar.getPackages()) {
					act.includes.add(a);
				}
			}
			result = act;
		}
		return result;
	}
	
	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
