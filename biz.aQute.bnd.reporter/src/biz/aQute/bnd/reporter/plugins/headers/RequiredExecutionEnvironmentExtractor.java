package biz.aQute.bnd.reporter.plugins.headers;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import java.util.LinkedList;
import java.util.List;

public class RequiredExecutionEnvironmentExtractor extends HeaderExtractor {
	
	final private static String HEADER_TAG = "bundleRequiredExecutionEnvironments";
	
	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		final Parameters header = manifest.getHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, true);
		final List<String> execs = new LinkedList<>();
		for (final String e : cleanKey(header.keySet())) {
			execs.add(e);
		}
		if (!execs.isEmpty()) {
			result = execs;
		}
		return result;
	}
	
	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
