package biz.aQute.bnd.reporter.plugin.headers;

import java.util.LinkedList;
import java.util.List;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.lib.tag.Tag;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.ManifestHelper;

public class SCMExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "bundle-scm";
	final private static String URL_TAG = "url";
	final private static String CONNECTION_TAG = "connection";
	final private static String DEVELOPER_CONNECTION_TAG = "developer-connection";
	final private static String TAG_TAG = "tag";

	@Override
	public List<Tag> extract(final ManifestHelper manifest, final Jar jar, final Reporter reporter) {
		final List<Tag> result = new LinkedList<>();
		final Parameters header = manifest.getHeader(Constants.BUNDLE_SCM, false);

		final String scm = header.toString();
		if (scm.length() > 0) {
			final Tag tag = new Tag(HEADER_TAG);
			final Attrs attrs = OSGiHeader.parseProperties(scm);

			for (final String key : attrs.keySet()) {
				if (key.equals("url")) {
					tag.addContent(new Tag(URL_TAG, attrs.get(key)));
				} else if (key.equals("connection")) {
					tag.addContent(new Tag(CONNECTION_TAG, attrs.get(key)));
				} else if (key.equals("developerConnection")) {
					tag.addContent(new Tag(DEVELOPER_CONNECTION_TAG, attrs.get(key)));
				} else if (key.equals("tag")) {
					tag.addContent(new Tag(TAG_TAG, attrs.get(key)));
				}
			}

			if (!attrs.containsKey("tag")) {
				tag.addContent(new Tag(TAG_TAG, "HEAD"));
			}

			result.add(tag);
		}

		return result;
	}
}
