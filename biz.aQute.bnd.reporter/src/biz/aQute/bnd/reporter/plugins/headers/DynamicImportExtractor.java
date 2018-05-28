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
import biz.aQute.bnd.reporter.plugins.headers.dto.DynamicImportPackageDTO;

public class DynamicImportExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "dynamicImportPackages";

	@Override
	public Object extract(final ManifestHelper manifest, final Jar jar, final Processor reporter) {
		Object result = null;
		final Parameters header = manifest.getHeader(Constants.DYNAMICIMPORT_PACKAGE, false);
		final List<DynamicImportPackageDTO> imports = new LinkedList<>();
		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final DynamicImportPackageDTO myImport = new DynamicImportPackageDTO();

			myImport.packageName = cleanKey(entry.getKey());

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
