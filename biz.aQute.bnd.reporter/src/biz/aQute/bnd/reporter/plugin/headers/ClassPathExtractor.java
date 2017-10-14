package biz.aQute.bnd.reporter.plugin.headers;

import java.util.LinkedList;
import java.util.List;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.lib.tag.Tag;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.ManifestHelper;

public class ClassPathExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "bundle-classpath";

	@Override
	public List<Tag> extract(final ManifestHelper manifest, final Jar jar, final Reporter reporter) {
		final List<Tag> result = new LinkedList<>();
		final Parameters header = manifest.getHeader(Constants.BUNDLE_CLASSPATH, false);
		for (final String classpath : cleanKey(header.keySet())) {
			result.add(new Tag(HEADER_TAG, classpath));
		}
		if (result.isEmpty()) {
			result.add(new Tag(HEADER_TAG, "."));
		}
		return result;
	}
}
