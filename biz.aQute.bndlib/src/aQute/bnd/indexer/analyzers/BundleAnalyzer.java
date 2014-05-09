package aQute.bnd.indexer.analyzers;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.*;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.resource.*;

import aQute.bnd.header.*;
import aQute.bnd.header.Attrs.Type;
import aQute.bnd.indexer.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.resource.*;
import aQute.bnd.version.*;

public class BundleAnalyzer implements ResourceAnalyzer {
	// Filename suffix for JAR files
	private static final String	SUFFIX_JAR	= ".jar";

	public void analyzeResource(Jar resource, ResourceBuilder rb) throws Exception {
		Manifest manifest = resource.getManifest();
		if (manifest == null)
			return;

		Domain domain = Domain.domain(manifest);
		MimeType mimeType = MimeType.Jar;

		if (resource.getBsn() != null) {
			mimeType = MimeType.Bundle;

			if (domain.getFragmentHost() != null)
				mimeType = MimeType.Fragment;

			// doBundleIdentity(domain, mimeType, capabilities);
			// doBundleAndHost(domain, capabilities);
			// doExports(domain, capabilities);
			// doImports(domain, requirements);
			// doRequireBundles(domain, requirements);
			// doFragment(domain, requirements);
			// doExportService(domain, capabilities);
			// doImportService(domain, requirements);
			// doBREE(domain, requirements);
			// doCapabilities(domain, capabilities);
			// doRequirements(domain, requirements);
			// doBundleNativeCode(domain, requirements);
		} else {
			// doPlainJarIdentity(resource, capabilities);
		}
	}

	 void doBundleIdentity(Domain domain, MimeType mimeType, List< ? super Capability> caps) throws Exception {

		String type;
		switch (mimeType) {
			case Bundle :
				type = Namespaces.RESOURCE_TYPE_BUNDLE;
				break;
			case Fragment :
				type = Namespaces.RESOURCE_TYPE_FRAGMENT;
				break;
			default :
				type = Namespaces.RESOURCE_TYPE_PLAIN_JAR;
				break;
		}

		Entry<String,Attrs> bundleSymbolicName = domain.getBundleSymbolicName();
		String bsn = bundleSymbolicName.getKey();
		String s = bundleSymbolicName.getValue().get(Constants.SINGLETON_DIRECTIVE + ":");

		boolean singleton = Boolean.TRUE.toString().equalsIgnoreCase(s);

		Version version = Version.parseVersion(domain.getBundleVersion());

		CapReqBuilder builder = new CapReqBuilder(Namespaces.NS_IDENTITY).addAttribute(Namespaces.NS_IDENTITY, bsn)
				.addAttribute(Namespaces.ATTR_IDENTITY_TYPE, type).addAttribute(Namespaces.ATTR_VERSION, version);
		if (singleton)
			builder.addDirective(Namespaces.DIRECTIVE_SINGLETON, Boolean.TRUE.toString());
		caps.add(builder.buildCapability());
	}

	 void doPlainJarIdentity(Jar resource, List< ? super Capability> caps) {
		String name = resource.getName();
		if (name.toLowerCase().endsWith(SUFFIX_JAR))
			name = name.substring(0, name.length() - SUFFIX_JAR.length());

		Version version = null;
		int dashIndex = name.lastIndexOf('-');
		if (dashIndex > 0) {
			try {
				String versionStr = name.substring(dashIndex + 1);
				version = new Version(versionStr);
				name = name.substring(0, dashIndex);
			}
			catch (Exception e) {
				version = null;
			}
		}

		CapReqBuilder builder = new CapReqBuilder(Namespaces.NS_IDENTITY).addAttribute(Namespaces.NS_IDENTITY, name)
				.addAttribute(Namespaces.ATTR_IDENTITY_TYPE, Namespaces.RESOURCE_TYPE_PLAIN_JAR);
		if (version != null)
			builder.addAttribute(Namespaces.ATTR_VERSION, version);
		caps.add(builder.buildCapability());
	}

	 void doBundleAndHost(Domain domain, List< ? super Capability> caps) throws Exception {

		CapReqBuilder bundleBuilder = new CapReqBuilder(Namespaces.NS_WIRING_BUNDLE);
		CapReqBuilder hostBuilder = new CapReqBuilder(Namespaces.NS_WIRING_HOST);
		boolean allowFragments = true;

		if (domain.getFragmentHost() != null)
			return;

		Map.Entry<String,Attrs> bsn = domain.getBundleSymbolicName();
		Version version = Version.parseVersion(bsn.getValue().getVersion());

		bundleBuilder.addAttribute(Namespaces.NS_WIRING_BUNDLE, bsn.getKey()).addAttribute(
				Constants.BUNDLE_VERSION_ATTRIBUTE, version);
		hostBuilder.addAttribute(Namespaces.NS_WIRING_HOST, bsn.getKey()).addAttribute(
				Constants.BUNDLE_VERSION_ATTRIBUTE, version);

		for (Entry<String,String> entry : bsn.getValue().entrySet()) {
			String key = entry.getKey();
			if (key.endsWith(":")) {
				String directiveName = key.substring(0, key.length() - 1);
				if (Constants.FRAGMENT_ATTACHMENT_DIRECTIVE.equalsIgnoreCase(directiveName)) {
					if (Constants.FRAGMENT_ATTACHMENT_NEVER.equalsIgnoreCase(entry.getValue()))
						allowFragments = false;
				} else if (!Constants.SINGLETON_DIRECTIVE.equalsIgnoreCase(directiveName)) {
					bundleBuilder.addDirective(directiveName, entry.getValue());
				}
			} else {
				bundleBuilder.addAttribute(key, entry.getValue());
			}
		}

		caps.add(bundleBuilder.buildCapability());
		if (allowFragments)
			caps.add(hostBuilder.buildCapability());
	}

	 void doExports(Domain domain, List< ? super Capability> caps) throws Exception {
		Parameters exports = domain.getExportPackage();

		for (Entry<String,Attrs> entry : exports.entrySet()) {
			CapReqBuilder builder = new CapReqBuilder(Namespaces.NS_WIRING_PACKAGE);

			String pkgName = Processor.removeDuplicateMarker(entry.getKey());
			builder.addAttribute(Namespaces.NS_WIRING_PACKAGE, pkgName);

			String versionStr = entry.getValue().getVersion();
			Version version = Version.parseVersion(versionStr);

			builder.addAttribute(Namespaces.ATTR_VERSION, version);

			for (Entry<String,String> attribEntry : entry.getValue().entrySet()) {
				String key = attribEntry.getKey();
				if (!"specification-version".equalsIgnoreCase(key)
						&& !Constants.VERSION_ATTRIBUTE.equalsIgnoreCase(key)) {
					if (key.endsWith(":"))
						builder.addDirective(key.substring(0, key.length() - 1), attribEntry.getValue());
					else
						builder.addAttribute(key, attribEntry.getValue());
				}
			}

			Entry<String,Attrs> bsn = domain.getBundleSymbolicName();

			builder.addAttribute(Namespaces.ATTR_BUNDLE_SYMBOLIC_NAME, bsn.getKey());
			builder.addAttribute(Namespaces.ATTR_BUNDLE_VERSION, Version.parseVersion(bsn.getValue().getVersion()));

			caps.add(builder.buildCapability());
		}
	}

	 void doImports(Domain domain, List< ? super Requirement> reqs) throws Exception {
		Parameters imports = domain.getImportPackage();

		for (Entry<String,Attrs> entry : imports.entrySet()) {
			StringBuilder filter = new StringBuilder();

			String pkgName = Processor.removeDuplicateMarker(entry.getKey());
			filter.append("(osgi.wiring.package=").append(pkgName).append(")");

			String versionStr = entry.getValue().get(Constants.VERSION_ATTRIBUTE);
			if (versionStr != null) {
				VersionRange version = new VersionRange(versionStr);
				filter.insert(0, "(&");
				filter.append(version.toFilter());
				filter.append(")");
			}

			CapReqBuilder builder = new CapReqBuilder(Namespaces.NS_WIRING_PACKAGE).addDirective(
					Namespaces.DIRECTIVE_FILTER, filter.toString());

			copyAttribsAndDirectives(entry.getValue(), builder, Constants.VERSION_ATTRIBUTE, "specification-version");

			reqs.add(builder.buildRequirement());
		}
	}

	private void copyAttribsAndDirectives(Map<String,String> input, CapReqBuilder output, String... ignores) {
		Set<String> ignoreSet = new HashSet<String>(Arrays.asList(ignores));

		for (Entry<String,String> entry : input.entrySet()) {
			String key = entry.getKey();
			if (!ignoreSet.contains(key)) {
				if (key.endsWith(":")) {
					String directive = key.substring(0, key.length() - 1);
					output.addDirective(directive, entry.getValue());
				} else {
					output.addAttribute(key, entry.getValue());
				}
			}
		}
	}

	void doRequireBundles(Domain domain, List< ? super Requirement> reqs) throws Exception {

		Parameters requires = domain.getRequireBundle();

		for (Entry<String,Attrs> entry : requires.entrySet()) {
			StringBuilder filter = new StringBuilder();

			String bsn = Processor.removeDuplicateMarker(entry.getKey());
			filter.append("(osgi.wiring.bundle=").append(bsn).append(")");

			String versionStr = entry.getValue().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
			if (versionStr != null) {
				VersionRange version = new VersionRange(versionStr);
				filter.insert(0, "(&");
				filter.append(version.toFilter());
				filter.append(")");
			}

			CapReqBuilder builder = new CapReqBuilder(Namespaces.NS_WIRING_BUNDLE).addDirective(
					Namespaces.DIRECTIVE_FILTER, filter.toString());

			copyAttribsAndDirectives(entry.getValue(), builder, Constants.BUNDLE_VERSION_ATTRIBUTE);

			reqs.add(builder.buildRequirement());
		}
	}

	void doFragment(Domain domain, List< ? super Requirement> reqs) throws Exception {
		Entry<String,Attrs> fragmentHost = domain.getFragmentHost();

		if (fragmentHost != null) {
			StringBuilder filter = new StringBuilder();

			String bsn = fragmentHost.getKey();
			filter.append("(&(osgi.wiring.host=").append(bsn).append(")");

			String versionStr = fragmentHost.getValue().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
			VersionRange range = new VersionRange(versionStr);
			filter.append(range.toFilter());
			filter.append(")");

			CapReqBuilder builder = new CapReqBuilder(Namespaces.NS_WIRING_HOST).addDirective(
					Namespaces.DIRECTIVE_FILTER, filter.toString());

			reqs.add(builder.buildRequirement());
		}
	}

	void doExportService(Domain domain, List< ? super Capability> caps) throws Exception {
		@SuppressWarnings("deprecation")
		Parameters exports = new Parameters(domain.get(Constants.EXPORT_SERVICE));

		for (Entry<String,Attrs> export : exports.entrySet()) {
			String service = Processor.removeDuplicateMarker(export.getKey());
			CapReqBuilder builder = new CapReqBuilder(Namespaces.NS_SERVICE).addAttribute(Constants.OBJECTCLASS,
					service);
			for (Entry<String,String> attribEntry : export.getValue().entrySet())
				builder.addAttribute(attribEntry.getKey(), attribEntry.getValue());
			builder.addDirective(Namespaces.DIRECTIVE_EFFECTIVE, Namespaces.EFFECTIVE_ACTIVE);
			caps.add(builder.buildCapability());
		}
	}

	void doImportService(Domain domain, List< ? super Requirement> reqs) throws Exception {
		@SuppressWarnings("deprecation")
		Parameters imports = new Parameters(domain.get(Constants.IMPORT_SERVICE));

		for (Entry<String,Attrs> imp : imports.entrySet()) {
			String service = Processor.removeDuplicateMarker(imp.getKey());
			StringBuilder filter = new StringBuilder();
			filter.append('(').append(Constants.OBJECTCLASS).append('=').append(service).append(')');

			CapReqBuilder builder = new CapReqBuilder(Namespaces.NS_SERVICE).addDirective(Namespaces.DIRECTIVE_FILTER,
					filter.toString()).addDirective(Namespaces.DIRECTIVE_EFFECTIVE, Namespaces.EFFECTIVE_ACTIVE);
			reqs.add(builder.buildRequirement());
		}
	}

	void doBREE(Domain domain, List< ? super Requirement> reqs) throws Exception {
		Parameters brees = domain.getBundleRequiredExecutionEnvironment();

		final String filter;
		if (!brees.isEmpty()) {
			if (brees.size() == 1) {
				String bree = brees.keySet().iterator().next();
				filter = EE.parseBREE(bree).toFilter();
			} else {
				StringBuilder builder = new StringBuilder().append("(|");
				for (String bree : brees.keySet()) {
					bree = Processor.removeDuplicateMarker(bree);
					builder.append(EE.parseBREE(bree).toFilter());
				}
				builder.append(')');
				filter = builder.toString();
			}

			Requirement requirement = new CapReqBuilder(Namespaces.NS_EE).addDirective(Namespaces.DIRECTIVE_FILTER,
					filter).buildRequirement();
			reqs.add(requirement);
		}
	}

	void doCapabilities(Domain domain, final List< ? super Capability> caps) throws Exception {
		buildFromHeader(domain.getProvideCapability(), new Yield<CapReqBuilder>() {
			public void yield(CapReqBuilder builder) {
				caps.add(builder.buildCapability());
			}
		});
	}

	void doRequirements(Domain domain, final List< ? super Requirement> reqs) throws IOException {
		buildFromHeader(domain.getRequireCapability(), new Yield<CapReqBuilder>() {
			public void yield(CapReqBuilder builder) {
				reqs.add(builder.buildRequirement());
			}
		});
	}

	void doBundleNativeCode(Domain domain, final List< ? super Requirement> reqs) throws IOException {

		Parameters nativeHeader = new Parameters(domain.get(Constants.BUNDLE_NATIVECODE));
		if (nativeHeader.isEmpty())
			return;

		boolean optional = false;
		List<String> options = new LinkedList<String>();

		for (Entry<String,Attrs> entry : nativeHeader.entrySet()) {
			String name = entry.getKey();
			if ("*".equals(name)) {
				optional = true;
				continue;
			}

			StringBuilder builder = new StringBuilder().append("(&");
			Map<String,String> attribs = entry.getValue();

			String osnamesFilter = buildFilter(attribs, Constants.BUNDLE_NATIVECODE_OSNAME,
					Namespaces.ATTR_NATIVE_OSNAME);
			if (osnamesFilter != null)
				builder.append(osnamesFilter);

			String versionRangeStr = attribs.get(Constants.BUNDLE_NATIVECODE_OSVERSION);
			if (versionRangeStr != null) {
				VersionRange range = new VersionRange(versionRangeStr);
				builder.append(range.toFilter());
			}

			String processorFilter = buildFilter(attribs, Constants.BUNDLE_NATIVECODE_PROCESSOR,
					Namespaces.ATTR_NATIVE_PROCESSOR);
			if (processorFilter != null)
				builder.append(processorFilter);

			String languageFilter = buildFilter(attribs, Constants.BUNDLE_NATIVECODE_LANGUAGE,
					Namespaces.ATTR_NATIVE_LANGUAGE);
			if (languageFilter != null)
				builder.append(languageFilter);

			String selectionFilter = attribs.get(Constants.SELECTION_FILTER_ATTRIBUTE);
			if (selectionFilter != null)
				builder.append(selectionFilter);

			builder.append(")");
			options.add(builder.toString());
		}
		if (options.isEmpty())
			return;

		String filter;
		if (options.size() == 1)
			filter = options.get(0);
		else {
			StringBuilder builder = new StringBuilder();
			builder.append("(|");
			for (String option : options)
				builder.append(option);
			builder.append(")");
			filter = builder.toString();
		}

		CapReqBuilder builder = new CapReqBuilder(Namespaces.NS_NATIVE).addDirective(Namespaces.DIRECTIVE_FILTER,
				filter);
		if (optional)
			builder.addDirective(Namespaces.DIRECTIVE_RESOLUTION, Namespaces.RESOLUTION_OPTIONAL);
		reqs.add(builder.buildRequirement());
	}

	/*
	 * Assemble a compound filter by searching a map of attributes. E.g. the
	 * following values: 1. foo=bar 2. foo=baz 3. foo=quux become the filter
	 * (|(foo~=bar)(foo~=baz)(foo~=quux)). Note that the duplicate foo keys will
	 * have trailing tildes as duplicate markers, these will be removed.
	 */
	private String buildFilter(Map<String,String> attribs, String match, String filterKey) {
		List<String> options = new LinkedList<String>();
		for (Entry<String,String> entry : attribs.entrySet()) {
			String key = Processor.removeDuplicateMarker(entry.getKey());
			if (match.equals(key)) {
				String filter = String.format("(%s~=%s)", filterKey, entry.getValue());
				options.add(filter);
			}
		}

		if (options.isEmpty())
			return null;
		if (options.size() == 1)
			return options.get(0);

		StringBuilder builder = new StringBuilder();
		builder.append("(|");
		for (String option : options)
			builder.append(option);
		builder.append(")");

		return builder.toString();
	}

	private static void buildFromHeader(Parameters p, Yield<CapReqBuilder> output) {
		for (Entry<String,Attrs> entry : p.entrySet()) {
			String namespace = Processor.removeDuplicateMarker(entry.getKey());
			CapReqBuilder builder = new CapReqBuilder(namespace);

			copyAttribsToBuilder(builder, entry.getValue());
			output.yield(builder);
		}
	}

	public static void copyAttribsToBuilder(CapReqBuilder builder, Attrs attribs) {
		for (Entry<String,String> attrib : attribs.entrySet()) {
			String key = attrib.getKey();

			if (key.endsWith(":")) {
				String directiveName = key.substring(0, key.length() - 1);
				builder.addDirective(directiveName, attrib.getValue());
			} else {
				// TODO
				Type type = attribs.getType(key);
				Object value = attribs.getTyped(key);
				builder.addAttribute(key, value);
			}
		}
	}

}
