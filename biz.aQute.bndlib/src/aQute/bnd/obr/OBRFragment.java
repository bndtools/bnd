package aQute.bnd.obr;

import java.io.File;
import java.util.Formatter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.AbstractWiringNamespace;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.service.repository.ContentNamespace;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.version.VersionRange;
import aQute.lib.io.IO;
import aQute.libg.cryptography.SHA1;
import aQute.libg.map.MAP;
import aQute.service.reporter.Reporter;

public class OBRFragment {

	// The mime-type of an OSGi bundle
	static final String				MIME_TYPE_OSGI_BUNDLE	= "application/vnd.osgi.bundle";
	private final static Pattern	EE_PATTERN				= Pattern
		.compile("[^.]-(\\d+(?:\\.\\d+(?:\\.\\d+(?:\\.)?)?)?)");

	@SuppressWarnings("deprecation")
	public static Reporter parse(Jar jar, ResourceBuilder resource) throws Exception {
		Manifest m = jar.getManifest();
		if (m == null)
			return null;

		Domain d = Domain.domain(m);
		d.setTranslation(jar);
		Entry<String, Attrs> bundleSymbolicName = d.getBundleSymbolicName();

		if (bundleSymbolicName == null)
			return null;

		boolean singleton = "true".equals(bundleSymbolicName.getValue()
			.get(Constants.SINGLETON_DIRECTIVE + ":"));
		boolean isFragment = d.get(Constants.FRAGMENT_HOST) != null;
		Version version = d.getBundleVersion() == null ? Version.emptyVersion : new Version(d.getBundleVersion());

		CapReqBuilder identity = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
		identity.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, bundleSymbolicName.getKey());
		identity.addAttribute(IdentityNamespace.CAPABILITY_COPYRIGHT_ATTRIBUTE,
			d.translate(Constants.BUNDLE_COPYRIGHT));
		identity.addAttribute(IdentityNamespace.CAPABILITY_DESCRIPTION_ATTRIBUTE,
			d.translate(Constants.BUNDLE_DESCRIPTION));
		identity.addAttribute(IdentityNamespace.CAPABILITY_DOCUMENTATION_ATTRIBUTE,
			d.translate(Constants.BUNDLE_DOCURL));
		identity.addAttribute(IdentityNamespace.CAPABILITY_LICENSE_ATTRIBUTE,
			d.translate(aQute.bnd.osgi.Constants.BUNDLE_LICENSE));
		if (singleton)
			identity.addAttribute(IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE, "true");
		identity.addAttribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
			isFragment ? IdentityNamespace.TYPE_FRAGMENT : IdentityNamespace.TYPE_BUNDLE);
		identity.addAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, new Version(d.getBundleVersion()));

		resource.addCapability(identity);

		if (isFragment) {

			//
			// Fragment-Host
			//

			Entry<String, Attrs> fragmentHost = d.getFragmentHost();
			CapReqBuilder fragment = new CapReqBuilder(HostNamespace.HOST_NAMESPACE);
			String v = fragmentHost.getValue()
				.get("version");
			if (v == null)
				v = "0";
			Version fragmentVersion = new Version(v);
			String filter = filter(PackageNamespace.PACKAGE_NAMESPACE, fragmentHost.getKey(), fragmentHost.getValue());
			fragment.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
			resource.addRequirement(fragment);
		} else {

			//
			// Bundle-SymbolicName
			//

			CapReqBuilder bundle = new CapReqBuilder(BundleNamespace.BUNDLE_NAMESPACE);
			CapReqBuilder host = new CapReqBuilder(HostNamespace.HOST_NAMESPACE);

			bundle.addAttribute("version", version);
			host.addAttribute("version", version);

			for (Entry<String, String> e : bundleSymbolicName.getValue()
				.entrySet()) {
				String key = e.getKey();
				if (key.endsWith(":")) {
					String directive = key.substring(0, key.length() - 1);
					if (Constants.FRAGMENT_ATTACHMENT_DIRECTIVE.equalsIgnoreCase(directive)) {
						if (Constants.FRAGMENT_ATTACHMENT_NEVER.equalsIgnoreCase(e.getValue()))
							host = null;

					} else if (!Constants.SINGLETON_DIRECTIVE.equalsIgnoreCase(directive)) {
						bundle.addDirective(directive, e.getValue());
					}
					if (host != null)
						host.addDirective(directive, e.getValue());
					bundle.addDirective(directive, e.getValue());
				} else {
					if (host != null)
						host.addAttribute(key, e.getValue());
					bundle.addAttribute(key, e.getValue());
				}
			}
			if (host != null)
				resource.addCapability(host);
			resource.addCapability(bundle);
		}

		//
		// Export-Package
		//

		Parameters exports = d.getExportPackage();
		for (Entry<String, Attrs> entry : exports.entrySet()) {
			CapReqBuilder exported = new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE);

			String pkgName = Processor.removeDuplicateMarker(entry.getKey());
			exported.addAttribute(PackageNamespace.PACKAGE_NAMESPACE, pkgName);

			String versionStr = entry.getValue()
				.get(Constants.VERSION_ATTRIBUTE);
			Version v = Version.parseVersion(entry.getValue()
				.get("version"));

			exported.addAttribute(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);

			for (Entry<String, String> attribEntry : entry.getValue()
				.entrySet()) {
				String key = attribEntry.getKey();
				if (key.endsWith(":")) {
					String directive = key.substring(0, key.length() - 1);
					exported.addDirective(directive, attribEntry.getValue());
				} else {
					if (key.equals("specification-version") || key.equals("version"))
						exported.addAttribute("version", Version.parseVersion(attribEntry.getValue()));
					else
						exported.addAttribute(key, attribEntry.getValue());
				}
			}

			exported.addAttribute(PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE,
				bundleSymbolicName.getKey());
			exported.addAttribute(AbstractWiringNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, version);

			resource.addCapability(exported);
		}

		//
		// Import-Package
		//

		Parameters imports = d.getImportPackage();
		for (Entry<String, Attrs> entry : imports.entrySet()) {
			CapReqBuilder imported = new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE);
			String name = Processor.removeDuplicateMarker(entry.getKey());
			String filter = filter(PackageNamespace.PACKAGE_NAMESPACE, Processor.removeDuplicateMarker(entry.getKey()),
				entry.getValue());
			imported.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
			resource.addRequirement(imported);
		}

		//
		// Require-Bundle
		//

		Parameters requires = d.getRequireBundle();
		for (Entry<String, Attrs> entry : requires.entrySet()) {
			CapReqBuilder req = new CapReqBuilder(BundleNamespace.BUNDLE_NAMESPACE);
			String bsn = Processor.removeDuplicateMarker(entry.getKey());
			String filter = filter(BundleNamespace.BUNDLE_NAMESPACE, bsn, entry.getValue());
			req.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
			resource.addRequirement(req);
		}

		//
		// Bundle-RequiredExecutionEnvironment
		//

		Parameters brees = d.getBundleRequiredExecutionEnvironment();
		try (Formatter formatter = new Formatter()) {
			formatter.format("(|");

			for (Entry<String, Attrs> bree : brees.entrySet()) {
				String name = Processor.removeDuplicateMarker(bree.getKey());
				Matcher matcher = EE_PATTERN.matcher(name);
				if (matcher.matches()) {
					name = matcher.group(1);
					Version v = Version.parseVersion(matcher.group(2));
					formatter.format("%s", filter(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, name,
						MAP.$("version", v.toString())));
				}
			}
			formatter.format(")");

			CapReqBuilder breeReq = new CapReqBuilder(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
			breeReq.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, formatter.toString());

			//
			// Export-Service (deprecated)
			//

			for (Entry<String, Attrs> export : d.getParameters(Constants.EXPORT_SERVICE)
				.entrySet()) {
				CapReqBuilder exportedService = new CapReqBuilder(ServiceNamespace.SERVICE_NAMESPACE);
				String service = Processor.removeDuplicateMarker(export.getKey());
				exportedService.addAttribute(ServiceNamespace.SERVICE_NAMESPACE, service);
				exportedService.addAttribute(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE, export.getValue()
					.get("objectclass"));
				resource.addCapability(exportedService);
			}

			//
			// Import-Service (deprecated)
			//

			for (Entry<String, Attrs> imported : d.getParameters(Constants.IMPORT_SERVICE)
				.entrySet()) {
				CapReqBuilder importedService = new CapReqBuilder(ServiceNamespace.SERVICE_NAMESPACE);
				String service = Processor.removeDuplicateMarker(imported.getKey());
				importedService.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE,
					filter(ServiceNamespace.SERVICE_NAMESPACE, service, imported.getValue()));
				resource.addRequirement(importedService);
			}

			//
			// Provide-Capability
			//

			for (Entry<String, Attrs> rc : d.getProvideCapability()
				.entrySet()) {
				resource.addCapability(toCapability(rc.getKey(), rc.getValue()));
			}

			//
			// Require-Capability
			//

			for (Entry<String, Attrs> rc : d.getRequireCapability()
				.entrySet()) {
				resource.addCapability(toRequirement(rc.getKey(), rc.getValue()));
			}
		}

		return null;
	}

	private static Capability toRequirement(String key, Attrs value) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Capability toCapability(String key, Attrs value) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Reporter parse(File file, ResourceBuilder resource, String base) throws Exception {
		try (Jar jar = new Jar(file)) {
			Reporter reporter = parse(jar, resource);
			if (!reporter.isOk())
				return reporter;

			CapReqBuilder content = new CapReqBuilder(ContentNamespace.CONTENT_NAMESPACE);
			String sha = SHA1.digest(file)
				.asHex();
			content.addAttribute(ContentNamespace.CONTENT_NAMESPACE, sha);
			content.addAttribute(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE, file.length());
			content.addAttribute(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE, MIME_TYPE_OSGI_BUNDLE);

			if (base != null) {
				base = IO.normalizePath(base);
				String path = IO.absolutePath(file);
				if (base.startsWith(path)) {
					content.addAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE, path.substring(base.length()));
				} else {
					reporter.error("Base path %s is not parent of file path: %s", base, path);
				}
			}

			resource.addCapability(content);
			return reporter;
		}
	}

	// TODO finish
	private static String filter(String ns, String primary, Map<String, String> value) {
		try (Formatter f = new Formatter()) {
			f.format("(&(%s=%s)", ns, primary);
			for (String key : value.keySet()) {
				if (key.equals("version") || key.equals("bundle-version")) {
					VersionRange vr = new VersionRange(value.get(key));
				} else {
					f.format("(%s=%s)", key, value.get(key));
				}
			}

			f.format(")");
		}
		return null;
	}

}
