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
import biz.aQute.bnd.reporter.plugins.headers.dto.ExportPackageDTO;

public class ExportExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "exportPackages";

	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		final Parameters header = manifest.getHeader(Constants.EXPORT_PACKAGE, false);
		final List<ExportPackageDTO> exports = new LinkedList<>();
		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final ExportPackageDTO myExport = new ExportPackageDTO();

			myExport.packageName = cleanKey(entry.getKey());

			myExport.version = toVersion(entry.getValue().get("version", ""));
			if (myExport.version == null) {
				myExport.version = getDefaultVersion();
			}

			if (entry.getValue().containsKey("exclude:")) {
				for (final String c : entry.getValue().get("exclude:").split(",")) {
					myExport.excludes.add(c.trim());
				}
			}

			if (entry.getValue().containsKey("include:")) {
				for (final String c : entry.getValue().get("include:").split(",")) {
					myExport.includes.add(c.trim());
				}
			}

			if (entry.getValue().containsKey("mandatory:")) {
				for (final String c : entry.getValue().get("mandatory:").split(",")) {
					myExport.mandatories.add(c.trim());
				}
			}

			if (entry.getValue().containsKey("uses:")) {
				for (final String c : entry.getValue().get("uses:").split(",")) {
					myExport.uses.add(c.trim());
				}
			}

			final Attrs attribute = new Attrs(entry.getValue());
			attribute.remove("version");
			attribute.remove("exclude:");
			attribute.remove("include:");
			attribute.remove("mandatory:");
			attribute.remove("uses:");

			for (final Entry<String, String> a : attribute.entrySet()) {
				if (!a.getKey().endsWith(":")) {
					myExport.arbitraryAttributes.put(removeSpecial(a.getKey()), a.getValue());
				}
			}
			exports.add(myExport);
		}
		if (!exports.isEmpty()) {
			result = exports;
		}
		return result;
	}

	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
