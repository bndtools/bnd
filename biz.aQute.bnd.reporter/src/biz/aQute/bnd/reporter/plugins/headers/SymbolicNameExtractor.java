package biz.aQute.bnd.reporter.plugins.headers;

import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import biz.aQute.bnd.reporter.plugins.headers.dto.BundleSymbolicNameDTO;

public class SymbolicNameExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "bundleSymbolicName";

	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		final Parameters header = manifest.getHeader(Constants.BUNDLE_SYMBOLICNAME, false);
		if (!header.isEmpty()) {
			final Attrs attributes = header.values().iterator().next();
			final BundleSymbolicNameDTO bsn = new BundleSymbolicNameDTO();

			bsn.symbolicName = cleanKey(header.keySet().iterator().next());

			if (attributes.containsKey("mandatory:")) {
				for (final String c : attributes.get("mandatory:").split(",")) {
					bsn.mandatories.add(c.trim());
				}
			}

			if (attributes.containsKey("fragment-attachment:")) {
				bsn.fragmentAttachment = attributes.get("fragment-attachment:");
			}

			if (attributes.containsKey("singleton:")) {
				bsn.singleton = Boolean.valueOf(attributes.get("singleton:"));
			}

			attributes.remove("fragment-attachment:");
			attributes.remove("mandatory:");
			attributes.remove("singleton:");

			for (final Entry<String, String> a : attributes.entrySet()) {
				if (!a.getKey().endsWith(":")) {
					bsn.arbitraryAttributes.put(removeSpecial(a.getKey()), a.getValue());
				}
			}
			result = bsn;
		} else {
			final BundleSymbolicNameDTO bsn = new BundleSymbolicNameDTO();
			bsn.symbolicName = "!! MISSING !!";
			reporter.warning("the bundle does not declare a symbolic name");
			result = bsn;
		}
		return result;
	}

	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
