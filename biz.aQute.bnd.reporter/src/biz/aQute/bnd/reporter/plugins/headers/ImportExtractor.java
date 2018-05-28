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
import biz.aQute.bnd.reporter.plugins.headers.dto.ImportPackageDTO;

public class ImportExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "importPackages";

	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		final Parameters header = manifest.getHeader(Constants.IMPORT_PACKAGE, false);
		final List<ImportPackageDTO> imports = new LinkedList<>();
		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final ImportPackageDTO myImport = new ImportPackageDTO();

			myImport.packageName = cleanKey(entry.getKey());

			if (entry.getValue().containsKey("resolution:")) {
				myImport.resolution = entry.getValue().get("resolution:");
			} else {
				myImport.resolution = "mandatory";
			}

			if (entry.getValue().containsKey("bundle-symbolic-name")) {
				myImport.bundleSymbolicName = entry.getValue().get("bundle-symbolic-name");
			}

			myImport.version = toOsgiRange(entry.getValue().get("version", ""));
			if (myImport.version == null) {
				myImport.version = getDefaultRange();
			}

			myImport.bundleVersion = toOsgiRange(entry.getValue().get("bundle-version", ""));
			if (myImport.bundleVersion == null) {
				myImport.bundleVersion = getDefaultRange();
			}

			final Attrs attribute = new Attrs(entry.getValue());
			attribute.remove("bundle-symbolic-name");
			attribute.remove("version");
			attribute.remove("bundle-version");
			attribute.remove("resolution:");

			for (final Entry<String, String> a : attribute.entrySet()) {
				if (!a.getKey().endsWith(":")) {
					myImport.arbitraryAttributes.put(removeSpecial(a.getKey()), a.getValue());
				}
			}
			imports.add(myImport);
		}
		if (!imports.isEmpty()) {
			result = imports;
		}
		return result;
	}

	@Override
	public String getEntryName() {
		return HEADER_TAG;
	}
}
