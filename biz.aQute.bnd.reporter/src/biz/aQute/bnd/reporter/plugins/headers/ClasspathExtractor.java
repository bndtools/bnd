package biz.aQute.bnd.reporter.plugins.headers;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import java.util.LinkedList;
import java.util.List;

public class ClasspathExtractor extends HeaderExtractor {
	
	final private static String HEADER_TAG = "bundleClasspaths";
	
	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		final Parameters header = manifest.getHeader(Constants.BUNDLE_CLASSPATH, false);
		
		final List<String> classpaths = new LinkedList<>();
		for (final String classpath : cleanKey(header.keySet())) {
			classpaths.add(classpath);
		}
		if (classpaths.isEmpty()) {
			classpaths.add(".");
		}
		result = classpaths;
		return result;
	}
	
	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
