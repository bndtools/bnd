package biz.aQute.bnd.reporter.plugins.headers;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import java.util.LinkedList;
import java.util.List;

public class CategoryExtractor extends HeaderExtractor {
	
	final private static String HEADER_TAG = "bundleCategories";
	
	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		final Parameters header = manifest.getHeader(Constants.BUNDLE_CATEGORY, false);
		final List<String> categories = new LinkedList<>();
		for (final String category : cleanKey(header.keySet())) {
			categories.add(category);
		}
		if (!categories.isEmpty()) {
			result = categories;
		}
		return result;
	}
	
	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
