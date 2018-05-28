package biz.aQute.bnd.reporter.plugins.headers;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import biz.aQute.bnd.reporter.helpers.ManifestHelper;
import biz.aQute.bnd.reporter.plugins.headers.dto.NativeCodeDTO;
import biz.aQute.bnd.reporter.plugins.headers.dto.NativeCodeEntryDTO;
import biz.aQute.bnd.reporter.plugins.headers.dto.VersionRangeDTO;

public class NativeCodeExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "bundleNativeCode";

	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		final Parameters header = manifest.getHeader(Constants.BUNDLE_NATIVECODE, true);
		if (!header.isEmpty()) {
			final Map<Attrs, NativeCodeEntryDTO> storedAttr = new HashMap<>();
			final NativeCodeDTO nativeCode = new NativeCodeDTO();

			for (final Entry<String, Attrs> entry : header.entrySet()) {
				if (entry.getKey().equals("*")) {
					nativeCode.optional = true;
				} else {
					NativeCodeEntryDTO nEntry = storedAttr.get(entry.getValue());
					if (nEntry == null) {
						nEntry = new NativeCodeEntryDTO();
						storedAttr.put(entry.getValue(), nEntry);

						String key = "osname";
						while (entry.getValue().get(key) != null) {
							nEntry.osnames.add(entry.getValue().get(key));
							key = key + "~";
						}

						key = "language";
						while (entry.getValue().get(key) != null) {
							nEntry.languages.add(entry.getValue().get(key));
							key = key + "~";
						}

						key = "processor";
						while (entry.getValue().get(key) != null) {
							nEntry.processors.add(entry.getValue().get(key));
							key = key + "~";
						}

						key = "selection-filter";
						while (entry.getValue().get(key) != null) {
							nEntry.selectionFilters.add(entry.getValue().get(key));
							key = key + "~";
						}

						key = "osversion";
						while (entry.getValue().get(key) != null) {
							final VersionRangeDTO r = toOsgiRange(entry.getValue().get(key, ""));
							if (r != null) {
								nEntry.osversions.add(r);
							}
							key = key + "~";
						}

						nativeCode.entries.add(nEntry);
					}
					nEntry.paths.add(cleanKey(entry.getKey()));
				}
			}
			result = nativeCode;
		}
		return result;
	}

	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
