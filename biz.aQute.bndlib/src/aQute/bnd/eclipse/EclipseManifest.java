package aQute.bnd.eclipse;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Attrs.Type;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Verifier;

public class EclipseManifest {
	public static final String	HEADER_FORMAT		= "%-40s: %s\n";
	public static String		REMOVE_HEADERS[]	= {
			"Built-By", "Created-By", "Bundle-RequiredExecutionEnvironment", "Build-Jdk", "Bundle-ManifestVersion",
			"ManifestVersion", "Archiver-Version", Constants.BUNDLE_CLASSPATH, Constants.EXPORT_PACKAGE,
			"Manifest-Version", Constants.BUNDLE_SYMBOLICNAME, Constants.SERVICE_COMPONENT, Constants.IMPORT_PACKAGE,
	        "Originally-Created-By", Constants.PRIVATE_PACKAGE, Constants.IGNORE_PACKAGE, Constants.REQUIRE_BUNDLE
	};

	public static String		COMMENT_HEADERS[]	= {
	        "Built-By", "Created-By", "Bundle-RequiredExecutionEnvironment", "Build-Jdk", "Bundle-ManifestVersion",
	        "ManifestVersion", "Archiver-Version", Constants.BUNDLE_CLASSPATH, Constants.EXPORT_PACKAGE,
	        "Manifest-Version", Constants.BUNDLE_SYMBOLICNAME, Constants.SERVICE_COMPONENT, Constants.IMPORT_PACKAGE,
	        "Originally-Created-By", Constants.PRIVATE_PACKAGE, Constants.IGNORE_PACKAGE
	};
	static String[]				PARAMETER_HEADERS	= {
			Constants.BUNDLE_ACTIVATIONPOLICY, Constants.BUNDLE_ACTIVATOR, Constants.BUNDLE_CATEGORY,
			Constants.BUNDLE_DEVELOPERS, Constants.BUNDLE_LICENSE, Constants.BUNDLE_LOCALIZATION,
			Constants.BUNDLE_NATIVECODE, Constants.EXPORT_SERVICE, Constants.FRAGMENT_HOST, Constants.IMPORT_SERVICE,
			Constants.PROVIDE_CAPABILITY, Constants.EXPORT_CONTENTS,
			Constants.EXPORT_PACKAGE
	};

	private final Processor		properties;
	private Domain				manifest;
	private String				bsn;

	EclipseManifest(Processor properties, String manifest) throws IOException {
		this.properties = properties;
		File file = properties.getFile(manifest);
		if (!file.isFile())
			this.properties.error("Manifest not found %s", file);

		this.manifest = Domain.domain(file);

		bsn = this.manifest.getBundleSymbolicName().getKey();
		if (bsn == null)
			bsn = properties.getBase().getName();
		assert bsn != null;
	}

	public String toBndFile(Set<String> sourcePackages, String workingset) throws IOException {
		try (Formatter model = new Formatter()) {

			Attrs attrs = manifest.getBundleSymbolicName().getValue();
			if (!attrs.isEmpty()) {
				// TODO not clear what to do with bsn, bnd does not like it
				// model.format(HEADER_FORMAT, Constants.BUNDLE_SYMBOLICNAME,
				// manifest.getBundleSymbolicName().toString());
			}

			Parameters bcpin = manifest.getBundleClasspath();
			if (!bcpin.isEmpty()) {
				boolean hasOnlyDefault = bcpin.size() == 1 && bcpin.keySet().iterator().next().equals(".");
				if (!hasOnlyDefault) {
					model.format(HEADER_FORMAT, Constants.BUNDLE_CLASSPATH, format(bcpin));
				}
			}

			Set<String> headers = new HashSet<>();

			for (String key : manifest)
				headers.add(key);

			for (String header : REMOVE_HEADERS) {
				if (headers.remove(header)) {
					// model.format(HEADER_FORMAT, "#" + header,
					// manifest.get(header));
				}
			}

			for (String name : PARAMETER_HEADERS) {
				headers.remove(name);
				String value = manifest.get(name);
				if (value != null) {
					if (!value.equals(properties.getProperty(name))) {
						Parameters parameters = new Parameters(value);
						model.format(HEADER_FORMAT, name, format(parameters));
					}
				}
			}

			Parameters requireBundle = manifest.getRequireBundle();
			if (requireBundle != null && !requireBundle.isEmpty()) {
				model.format(HEADER_FORMAT, "# Require-Bundle", requireBundle);
				headers.remove(Constants.REQUIRE_BUNDLE);
			}

			Parameters exports = manifest.getExportContents();
			exports.putAll(manifest.getExportPackage());
			Parameters privates = new Parameters(sourcePackages);
			privates.keySet().removeAll(exports.keySet());
			privates.keySet().removeIf(pname -> !Verifier.PACKAGEPATTERN.matcher(pname).matches());

			if (!privates.isEmpty())
				model.format(HEADER_FORMAT, Constants.PRIVATE_PACKAGE, format(privates));

			for (String header : headers) {

				String value = manifest.get(header).trim();
				if (!value.equals(properties.getProperty(header))) {
					model.format(HEADER_FORMAT, header, value);
				}

			}

			if (workingset != null) {
				model.format(HEADER_FORMAT, "-workingset", workingset);
			}
			return model.toString();
		}

	}

	static String format(Parameters parameters) throws IOException {
		if (parameters.isEmpty())
			return "";

		if (parameters.size() == 1) {
			return parameters.toString();
		}

		StringBuilder sb = new StringBuilder();

		String del = "\\\n    ";
		for (Map.Entry<String,Attrs> e : parameters.entrySet()) {
			sb.append(del).append(e.getKey());
			Attrs value = e.getValue();
			for (Entry<String,String> a : value.entrySet()) {
				sb.append("; \\\n        ");
				sb.append(a.getKey().trim());
				Type type = value.getType(e.getKey());
				if (type != null && !type.equals(Type.STRING)) {
					sb.append(":").append(type);
				}
				sb.append("=");
				OSGiHeader.quote(sb, a.getValue());
			}
			del = ", \\\n    ";
		}
		sb.append("\n");
		return sb.toString();
	}

	public String getBsn() {
		return bsn;
	}
}
