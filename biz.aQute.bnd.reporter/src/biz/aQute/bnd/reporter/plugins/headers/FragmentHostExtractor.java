package biz.aQute.bnd.reporter.plugins.headers;

import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import biz.aQute.bnd.reporter.plugins.headers.dto.FragmentHostDTO;

public class FragmentHostExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "fragmentHost";

	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		final Parameters header = manifest.getHeader(Constants.FRAGMENT_HOST, false);
		if (!header.isEmpty()) {
			final Attrs attibutes = header.values().iterator().next();
			final FragmentHostDTO frag = new FragmentHostDTO();

			frag.bundleSymbolicName = header.keySet().iterator().next();

			if (attibutes.containsKey("extension:")) {
				frag.extension = attibutes.get("extension:");
			} else {
				frag.extension = "framework";
			}

			frag.bundleVersion = toOsgiRange(attibutes.get("bundle-version", ""));
			if (frag.bundleVersion == null) {
				frag.bundleVersion = getDefaultRange();
			}

			attibutes.remove("bundle-version");
			attibutes.remove("extension:");

			for (final Entry<String, String> a : attibutes.entrySet()) {
				if (!a.getKey().endsWith(":")) {
					frag.arbitraryAttributes.put(removeSpecial(a.getKey()), a.getValue());

				}
			}
			result = frag;
		}
		return result;
	}

	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
