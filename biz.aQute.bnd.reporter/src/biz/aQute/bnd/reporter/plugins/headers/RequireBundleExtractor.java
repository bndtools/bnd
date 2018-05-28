package biz.aQute.bnd.reporter.plugins.headers;

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import biz.aQute.bnd.reporter.plugins.headers.dto.RequireBundleDTO;

public class RequireBundleExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "requireBundles";

	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		final Parameters header = manifest.getHeader(Constants.REQUIRE_BUNDLE, false);
		final List<RequireBundleDTO> reqs = new LinkedList<>();
		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final RequireBundleDTO req = new RequireBundleDTO();

			req.bundleSymbolicName = cleanKey(entry.getKey());

			if (entry.getValue().containsKey("resolution:")) {
				req.resolution = entry.getValue().get("resolution:");
			}

			if (entry.getValue().containsKey("visibility:")) {
				req.visibility = entry.getValue().get("visibility:");
			}

			req.bundleVersion = toOsgiRange(entry.getValue().get("bundle-version", ""));
			if (req.bundleVersion == null) {
				req.bundleVersion = getDefaultRange();
			}

			final Attrs attribute = new Attrs(entry.getValue());
			attribute.remove("bundle-version");
			attribute.remove("resolution:");
			attribute.remove("visibility:");

			for (final Entry<String, String> a : attribute.entrySet()) {
				if (!a.getKey().endsWith(":")) {
					req.arbitraryAttributes.put(removeSpecial(a.getKey()), a.getValue());
				}
			}
			reqs.add(req);
		}
		if (!reqs.isEmpty()) {
			result = reqs;
		}
		return result;
	}

	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
