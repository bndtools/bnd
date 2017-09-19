package biz.aQute.bnd.reporter.plugin.headers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.lib.tag.Tag;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.ManifestHelper;

public class NativeCodeExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "bundle-native-codes";
	final private static String NATIVE_CODE_TAG = "native-code";
	final private static String OPTIONAL_TAG = "optional";
	final private static String PATH_TAG = "path";
	final private static String OS_NAME_TAG = "os-name";
	final private static String OS_VERSION_TAG = "os-version";
	final private static String LANGUAGE_TAG = "language";
	final private static String PROCESSOR_TAG = "processor";
	final private static String SELECTION_FILTER_TAG = "selection-filter";

	@Override
	public List<Tag> extract(final ManifestHelper manifest, final Jar jar, final Reporter reporter) {
		final List<Tag> result = new LinkedList<>();
		final Parameters header = manifest.getHeader(Constants.BUNDLE_NATIVECODE, true);
		if (!header.isEmpty()) {
			final Tag tag = new Tag(HEADER_TAG);
			final Map<Attrs, Tag> storedAttr = new HashMap<>();

			boolean optional = false;
			for (final Entry<String, Attrs> entry : header.entrySet()) {
				if (entry.getKey().equals("*")) {
					optional = true;
					tag.addContent(new Tag(OPTIONAL_TAG, true));
				} else {
					Tag nTag = storedAttr.get(entry.getValue());
					if (nTag == null) {
						nTag = new Tag(NATIVE_CODE_TAG);
						storedAttr.put(entry.getValue(), nTag);

						String key = "osname";
						while (entry.getValue().get(key) != null) {
							nTag.addContent(new Tag(OS_NAME_TAG, entry.getValue().get(key)));
							key = key + "~";
						}

						key = "language";
						while (entry.getValue().get(key) != null) {
							nTag.addContent(new Tag(LANGUAGE_TAG, entry.getValue().get(key)));
							key = key + "~";
						}

						key = "processor";
						while (entry.getValue().get(key) != null) {
							nTag.addContent(new Tag(PROCESSOR_TAG, entry.getValue().get(key)));
							key = key + "~";
						}

						key = "selection-filter";
						while (entry.getValue().get(key) != null) {
							nTag.addContent(new Tag(SELECTION_FILTER_TAG, entry.getValue().get(key)));
							key = key + "~";
						}

						key = "osversion";
						while (entry.getValue().get(key) != null) {
							final Tag vTag = toOsgiRange(entry.getValue().get(key, ""), OS_VERSION_TAG);
							if (vTag != null) {
								nTag.addContent(vTag);
							}
							key = key + "~";
						}
						
						tag.addContent(nTag);
					}
					
					nTag.addContent(new Tag(PATH_TAG, cleanKey(entry.getKey())));
				}
			}
			
			if (!optional) {
				tag.addContent(new Tag(OPTIONAL_TAG, false));
			}
			
			result.add(tag);
		}
		return result;
	}
}
