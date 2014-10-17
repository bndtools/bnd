package org.osgi.service.indexer.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.indexer.Builder;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Namespaces;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.impl.types.SymbolicName;
import org.osgi.service.indexer.impl.types.VersionKey;
import org.osgi.service.indexer.impl.types.VersionRange;
import org.osgi.service.indexer.impl.util.Hex;
import org.osgi.service.indexer.impl.util.OSGiHeader;
import org.osgi.service.indexer.impl.util.Yield;
import org.osgi.service.log.LogService;

public class BundleAnalyzer implements ResourceAnalyzer {

	private static final String SHA_256 = "SHA-256";

	// Duplicate these constants here to avoid a compile-time dependency on OSGi
	// R4.3
	private static final String PROVIDE_CAPABILITY = "Provide-Capability";
	private static final String REQUIRE_CAPABILITY = "Require-Capability";

	// Obsolete OSGi constants
	private static final String IMPORT_SERVICE_AVAILABILITY = "availability:";

	// Filename suffix for JAR files
	private static final String SUFFIX_JAR = ".jar";

	private final ThreadLocal<GeneratorState> state = new ThreadLocal<GeneratorState>();
	@SuppressWarnings("unused")
	private final LogService log;

	public BundleAnalyzer(LogService log) {
		this.log = log;
	}

	public void analyzeResource(Resource resource, List<Capability> capabilities, List<Requirement> requirements) throws Exception {
		MimeType mimeType = Util.getMimeType(resource);
		if (mimeType == MimeType.Bundle || mimeType == MimeType.Fragment) {
			doBundleIdentity(resource, mimeType, capabilities);
			doContent(resource, mimeType, capabilities);
			doBundleAndHost(resource, capabilities);
			doExports(resource, capabilities);
			doImports(resource, requirements);
			doRequireBundles(resource, requirements);
			doFragment(resource, requirements);
			doExportService(resource, capabilities);
			doImportService(resource, requirements);
			doBREE(resource, requirements);
			doCapabilities(resource, capabilities);
			doRequirements(resource, requirements);
			doBundleNativeCode(resource, requirements);
		} else {
			doPlainJarIdentity(resource, capabilities);
			doContent(resource, mimeType, capabilities);
		}
	}

	private void doBundleIdentity(Resource resource, MimeType mimeType, List<? super Capability> caps) throws Exception {
		Manifest manifest = resource.getManifest();
		if (manifest == null)
			throw new IllegalArgumentException("Missing bundle manifest.");

		String type;
		switch (mimeType) {
		case Bundle:
			type = Namespaces.RESOURCE_TYPE_BUNDLE;
			break;
		case Fragment:
			type = Namespaces.RESOURCE_TYPE_FRAGMENT;
			break;
		default:
			type = Namespaces.RESOURCE_TYPE_PLAIN_JAR;
			break;
		}

		SymbolicName bsn = Util.getSymbolicName(resource);
		boolean singleton = Boolean.TRUE.toString().equalsIgnoreCase(bsn.getAttributes().get(Constants.SINGLETON_DIRECTIVE + ":"));

		Version version = Util.getVersion(resource);

		Builder builder = new Builder().setNamespace(Namespaces.NS_IDENTITY).addAttribute(Namespaces.NS_IDENTITY, bsn.getName()).addAttribute(Namespaces.ATTR_IDENTITY_TYPE, type)
				.addAttribute(Namespaces.ATTR_VERSION, version);
		if (singleton)
			builder.addDirective(Namespaces.DIRECTIVE_SINGLETON, Boolean.TRUE.toString());
		caps.add(builder.buildCapability());
	}

	private void doPlainJarIdentity(Resource resource, List<? super Capability> caps) {
		String name = (String) resource.getProperties().get(Resource.NAME);
		if (name.toLowerCase().endsWith(SUFFIX_JAR))
			name = name.substring(0, name.length() - SUFFIX_JAR.length());

		Version version = null;
		int dashIndex = name.lastIndexOf('-');
		if (dashIndex > 0) {
			try {
				String versionStr = name.substring(dashIndex + 1);
				version = new Version(versionStr);
				name = name.substring(0, dashIndex);
			} catch (Exception e) {
				version = null;
			}
		}

		Builder builder = new Builder().setNamespace(Namespaces.NS_IDENTITY).addAttribute(Namespaces.NS_IDENTITY, name)
				.addAttribute(Namespaces.ATTR_IDENTITY_TYPE, Namespaces.RESOURCE_TYPE_PLAIN_JAR);
		if (version != null)
			builder.addAttribute(Namespaces.ATTR_VERSION, version);
		caps.add(builder.buildCapability());
	}

	void setStateLocal(GeneratorState state) {
		this.state.set(state);
	}

	private GeneratorState getStateLocal() {
		return state.get();
	}

	private void doContent(Resource resource, MimeType mimeType, List<? super Capability> capabilities) throws Exception {
		Builder builder = new Builder().setNamespace(Namespaces.NS_CONTENT);

		String sha = calculateSHA(resource);
		builder.addAttribute(Namespaces.NS_CONTENT, sha);

		String location = calculateLocation(resource);
		builder.addAttribute(Namespaces.ATTR_CONTENT_URL, location);

		long size = resource.getSize();
		if (size > 0L)
			builder.addAttribute(Namespaces.ATTR_CONTENT_SIZE, size);

		builder.addAttribute(Namespaces.ATTR_CONTENT_MIME, mimeType.toString());

		capabilities.add(builder.buildCapability());
	}

	private String calculateSHA(Resource resource) throws IOException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance(SHA_256);
		byte[] buf = new byte[1024];

		InputStream stream = null;
		try {
			stream = resource.getStream();
			while (true) {
				int bytesRead = stream.read(buf, 0, 1024);
				if (bytesRead < 0)
					break;

				digest.update(buf, 0, bytesRead);
			}
		} finally {
			if (stream != null)
				stream.close();
		}

		return Hex.toHexString(digest.digest());
	}

	private String calculateLocation(Resource resource) throws IOException {
		String location = resource.getLocation();

		File path = new File(location);
		String fileName = path.getName();
		String dir = path.getAbsoluteFile().getParentFile().toURI().toURL().toString();

		String result = location;

		GeneratorState state = getStateLocal();
		if (state != null) {
			String rootUrl = state.getRootUrl().toString();
			if (!rootUrl.endsWith("/"))
				rootUrl += "/";

			if (rootUrl != null) {
				if (dir.startsWith(rootUrl))
					dir = dir.substring(rootUrl.length());
				else
					throw new IllegalArgumentException("Cannot index files above the root URL.");
			}

			String urlTemplate = state.getUrlTemplate();
			if (urlTemplate != null) {
				String bsn = (urlTemplate.indexOf("%s") == -1) ? "" : Util.getSymbolicName(resource).getName();
				Version version = (urlTemplate.indexOf("%v") == -1) ? Version.emptyVersion : Util.getVersion(resource);
				urlTemplate = urlTemplate.replaceAll("%s", "%1\\$s").replaceAll("%f", "%2\\$s").replaceAll("%p", "%3\\$s").replaceAll("%v", "%4\\$s");
				result = String.format(urlTemplate, bsn, fileName, dir, version);
			} else {
				result = dir + fileName;
			}
		}

		return result;
	}

	private void doBundleAndHost(Resource resource, List<? super Capability> caps) throws Exception {
		Builder bundleBuilder = new Builder().setNamespace(Namespaces.NS_WIRING_BUNDLE);
		Builder hostBuilder = new Builder().setNamespace(Namespaces.NS_WIRING_HOST);
		boolean allowFragments = true;

		Attributes attribs = resource.getManifest().getMainAttributes();
		if (attribs.getValue(Constants.FRAGMENT_HOST) != null)
			return;

		SymbolicName bsn = Util.getSymbolicName(resource);
		Version version = Util.getVersion(resource);

		bundleBuilder.addAttribute(Namespaces.NS_WIRING_BUNDLE, bsn.getName()).addAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE, version);
		hostBuilder.addAttribute(Namespaces.NS_WIRING_HOST, bsn.getName()).addAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE, version);

		for (Entry<String, String> attribEntry : bsn.getAttributes().entrySet()) {
			String key = attribEntry.getKey();
			if (key.endsWith(":")) {
				String directiveName = key.substring(0, key.length() - 1);
				if (Constants.FRAGMENT_ATTACHMENT_DIRECTIVE.equalsIgnoreCase(directiveName)) {
					if (Constants.FRAGMENT_ATTACHMENT_NEVER.equalsIgnoreCase(attribEntry.getValue()))
						allowFragments = false;
				} else if (!Constants.SINGLETON_DIRECTIVE.equalsIgnoreCase(directiveName)) {
					bundleBuilder.addDirective(directiveName, attribEntry.getValue());
				}
			} else {
				bundleBuilder.addAttribute(key, attribEntry.getValue());
			}
		}

		caps.add(bundleBuilder.buildCapability());
		if (allowFragments)
			caps.add(hostBuilder.buildCapability());
	}

	private void doExports(Resource resource, List<? super Capability> caps) throws Exception {
		Manifest manifest = resource.getManifest();

		String exportsStr = manifest.getMainAttributes().getValue(Constants.EXPORT_PACKAGE);
		Map<String, Map<String, String>> exports = OSGiHeader.parseHeader(exportsStr);
		for (Entry<String, Map<String, String>> entry : exports.entrySet()) {
			Builder builder = new Builder().setNamespace(Namespaces.NS_WIRING_PACKAGE);

			String pkgName = OSGiHeader.removeDuplicateMarker(entry.getKey());
			builder.addAttribute(Namespaces.NS_WIRING_PACKAGE, pkgName);

			String versionStr = entry.getValue().get(Constants.VERSION_ATTRIBUTE);
			Version version = (versionStr != null) ? new Version(versionStr) : new Version(0, 0, 0);
			builder.addAttribute(Namespaces.ATTR_VERSION, version);

			for (Entry<String, String> attribEntry : entry.getValue().entrySet()) {
				String key = attribEntry.getKey();
				if (!"specification-version".equalsIgnoreCase(key) && !Constants.VERSION_ATTRIBUTE.equalsIgnoreCase(key)) {
					if (key.endsWith(":"))
						builder.addDirective(key.substring(0, key.length() - 1), attribEntry.getValue());
					else
						builder.addAttribute(key, attribEntry.getValue());
				}
			}

			SymbolicName bsn = Util.getSymbolicName(resource);
			builder.addAttribute(Namespaces.ATTR_BUNDLE_SYMBOLIC_NAME, bsn.getName());
			Version bundleVersion = Util.getVersion(resource);
			builder.addAttribute(Namespaces.ATTR_BUNDLE_VERSION, bundleVersion);

			caps.add(builder.buildCapability());
		}
	}

	private void doImports(Resource resource, List<? super Requirement> reqs) throws Exception {
		Manifest manifest = resource.getManifest();

		String importsStr = manifest.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
		Map<String, Map<String, String>> imports = OSGiHeader.parseHeader(importsStr);
		for (Entry<String, Map<String, String>> entry : imports.entrySet()) {
			StringBuilder filter = new StringBuilder();

			String pkgName = OSGiHeader.removeDuplicateMarker(entry.getKey());
			filter.append("(osgi.wiring.package=").append(pkgName).append(")");

			String versionStr = entry.getValue().get(Constants.VERSION_ATTRIBUTE);
			if (versionStr != null) {
				VersionRange version = new VersionRange(versionStr);
				filter.insert(0, "(&");
				Util.addVersionFilter(filter, version, VersionKey.PackageVersion);
				filter.append(")");
			}

			Builder builder = new Builder().setNamespace(Namespaces.NS_WIRING_PACKAGE).addDirective(Namespaces.DIRECTIVE_FILTER, filter.toString());

			copyAttribsAndDirectives(entry.getValue(), builder, Constants.VERSION_ATTRIBUTE, "specification-version");

			reqs.add(builder.buildRequirement());
		}
	}

	private void copyAttribsAndDirectives(Map<String, String> input, Builder output, String... ignores) {
		Set<String> ignoreSet = new HashSet<String>(Arrays.asList(ignores));

		for (Entry<String, String> entry : input.entrySet()) {
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

	private void doRequireBundles(Resource resource, List<? super Requirement> reqs) throws Exception {
		Manifest manifest = resource.getManifest();

		String requiresStr = manifest.getMainAttributes().getValue(Constants.REQUIRE_BUNDLE);
		if (requiresStr == null)
			return;

		Map<String, Map<String, String>> requires = OSGiHeader.parseHeader(requiresStr);
		for (Entry<String, Map<String, String>> entry : requires.entrySet()) {
			StringBuilder filter = new StringBuilder();

			String bsn = OSGiHeader.removeDuplicateMarker(entry.getKey());
			filter.append("(osgi.wiring.bundle=").append(bsn).append(")");

			String versionStr = entry.getValue().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
			if (versionStr != null) {
				VersionRange version = new VersionRange(versionStr);
				filter.insert(0, "(&");
				Util.addVersionFilter(filter, version, VersionKey.BundleVersion);
				filter.append(")");
			}

			Builder builder = new Builder().setNamespace(Namespaces.NS_WIRING_BUNDLE).addDirective(Namespaces.DIRECTIVE_FILTER, filter.toString());

			copyAttribsAndDirectives(entry.getValue(), builder, Constants.BUNDLE_VERSION_ATTRIBUTE);

			reqs.add(builder.buildRequirement());
		}
	}

	private void doFragment(Resource resource, List<? super Requirement> reqs) throws Exception {
		Manifest manifest = resource.getManifest();
		String fragmentHost = manifest.getMainAttributes().getValue(Constants.FRAGMENT_HOST);

		if (fragmentHost != null) {
			StringBuilder filter = new StringBuilder();
			Map<String, Map<String, String>> fragmentList = OSGiHeader.parseHeader(fragmentHost);
			if (fragmentList.size() != 1)
				throw new IllegalArgumentException("Invalid Fragment-Host header: cannot contain multiple entries");
			Entry<String, Map<String, String>> entry = fragmentList.entrySet().iterator().next();

			String bsn = entry.getKey();
			filter.append("(&(osgi.wiring.host=").append(bsn).append(")");

			String versionStr = entry.getValue().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
			VersionRange version = versionStr != null ? new VersionRange(versionStr) : new VersionRange(Version.emptyVersion.toString());
			Util.addVersionFilter(filter, version, VersionKey.BundleVersion);
			filter.append(")");

			Builder builder = new Builder().setNamespace(Namespaces.NS_WIRING_HOST).addDirective(Namespaces.DIRECTIVE_FILTER, filter.toString());

			reqs.add(builder.buildRequirement());
		}
	}

	private void doExportService(Resource resource, List<? super Capability> caps) throws Exception {
		@SuppressWarnings("deprecation")
		String exportsStr = resource.getManifest().getMainAttributes().getValue(Constants.EXPORT_SERVICE);
		Map<String, Map<String, String>> exports = OSGiHeader.parseHeader(exportsStr);

		for (Entry<String, Map<String, String>> export : exports.entrySet()) {
			String service = OSGiHeader.removeDuplicateMarker(export.getKey());
			Builder builder = new Builder().setNamespace(Namespaces.NS_SERVICE).addAttribute(Constants.OBJECTCLASS, service);
			for (Entry<String, String> attribEntry : export.getValue().entrySet())
				builder.addAttribute(attribEntry.getKey(), attribEntry.getValue());
			builder.addDirective(Namespaces.DIRECTIVE_EFFECTIVE, Namespaces.EFFECTIVE_ACTIVE);
			caps.add(builder.buildCapability());
		}
	}

	private void doImportService(Resource resource, List<? super Requirement> reqs) throws Exception {
		@SuppressWarnings("deprecation")
		String importsStr = resource.getManifest().getMainAttributes().getValue(Constants.IMPORT_SERVICE);
		Map<String, Map<String, String>> imports = OSGiHeader.parseHeader(importsStr);

		for (Entry<String, Map<String, String>> imp : imports.entrySet()) {
			String service = OSGiHeader.removeDuplicateMarker(imp.getKey());
			Map<String, String> attribs = imp.getValue();

			boolean optional = false;
			String availabilityStr = attribs.get(IMPORT_SERVICE_AVAILABILITY);
			if (Constants.RESOLUTION_OPTIONAL.equals(availabilityStr))
				optional = true;

			StringBuilder filter = new StringBuilder();
			filter.append('(').append(Constants.OBJECTCLASS).append('=').append(service).append(')');

			Builder builder = new Builder().setNamespace(Namespaces.NS_SERVICE).addDirective(Namespaces.DIRECTIVE_FILTER, filter.toString())
					.addDirective(Namespaces.DIRECTIVE_EFFECTIVE, Namespaces.EFFECTIVE_ACTIVE);
			if (optional)
				builder.addDirective(Namespaces.DIRECTIVE_RESOLUTION, Constants.RESOLUTION_OPTIONAL);
			reqs.add(builder.buildRequirement());
		}
	}

	private void doBREE(Resource resource, List<? super Requirement> reqs) throws Exception {
		@SuppressWarnings("deprecation")
		String breeStr = resource.getManifest().getMainAttributes().getValue(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		Map<String, Map<String, String>> brees = OSGiHeader.parseHeader(breeStr);

		final String filter;
		if (!brees.isEmpty()) {
			if (brees.size() == 1) {
				String bree = brees.keySet().iterator().next();
				filter = EE.parseBREE(bree).toFilter();
			} else {
				StringBuilder builder = new StringBuilder().append("(|");
				for (String bree : brees.keySet()) {
					bree = OSGiHeader.removeDuplicateMarker(bree);
					builder.append(EE.parseBREE(bree).toFilter());
				}
				builder.append(')');
				filter = builder.toString();
			}

			Requirement requirement = new Builder().setNamespace(Namespaces.NS_EE).addDirective(Namespaces.DIRECTIVE_FILTER, filter).buildRequirement();
			reqs.add(requirement);
		}
	}

	private void doCapabilities(Resource resource, final List<? super Capability> caps) throws Exception {
		String capsStr = resource.getManifest().getMainAttributes().getValue(PROVIDE_CAPABILITY);
		buildFromHeader(capsStr, new Yield<Builder>() {
			public void yield(Builder builder) {
				caps.add(builder.buildCapability());
			}
		});
	}

	private void doRequirements(Resource resource, final List<? super Requirement> reqs) throws IOException {
		String reqsStr = resource.getManifest().getMainAttributes().getValue(REQUIRE_CAPABILITY);
		buildFromHeader(reqsStr, new Yield<Builder>() {
			public void yield(Builder builder) {
				reqs.add(builder.buildRequirement());
			}
		});
	}

	private void doBundleNativeCode(Resource resource, final List<? super Requirement> reqs) throws IOException {
		String nativeHeaderStr = resource.getManifest().getMainAttributes().getValue(Constants.BUNDLE_NATIVECODE);
		if (nativeHeaderStr == null)
			return;

		boolean optional = false;
		List<String> options = new LinkedList<String>();

		Map<String, Map<String, String>> nativeHeader = OSGiHeader.parseHeader(nativeHeaderStr);
		for (Entry<String, Map<String, String>> entry : nativeHeader.entrySet()) {
			String name = entry.getKey();
			if ("*".equals(name)) {
				optional = true;
				continue;
			}

			StringBuilder builder = new StringBuilder().append("(&");
			Map<String, String> attribs = entry.getValue();

			String osnamesFilter = buildFilter(attribs, Constants.BUNDLE_NATIVECODE_OSNAME, Namespaces.ATTR_NATIVE_OSNAME);
			if (osnamesFilter != null)
				builder.append(osnamesFilter);

			String versionRangeStr = attribs.get(Constants.BUNDLE_NATIVECODE_OSVERSION);
			if (versionRangeStr != null)
				Util.addVersionFilter(builder, new VersionRange(versionRangeStr), VersionKey.NativeOsVersion);

			String processorFilter = buildFilter(attribs, Constants.BUNDLE_NATIVECODE_PROCESSOR, Namespaces.ATTR_NATIVE_PROCESSOR);
			if (processorFilter != null)
				builder.append(processorFilter);

			String languageFilter = buildFilter(attribs, Constants.BUNDLE_NATIVECODE_LANGUAGE, Namespaces.ATTR_NATIVE_LANGUAGE);
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

		Builder builder = new Builder().setNamespace(Namespaces.NS_NATIVE).addDirective(Namespaces.DIRECTIVE_FILTER, filter);
		if (optional)
			builder.addDirective(Namespaces.DIRECTIVE_RESOLUTION, Namespaces.RESOLUTION_OPTIONAL);
		reqs.add(builder.buildRequirement());
	}

	/*
	 * Assemble a compound filter by searching a map of attributes. E.g. the
	 * following values:
	 * 
	 * 1. foo=bar 2. foo=baz 3. foo=quux
	 * 
	 * become the filter (|(foo~=bar)(foo~=baz)(foo~=quux)).
	 * 
	 * Note that the duplicate foo keys will have trailing tildes as duplicate
	 * markers, these will be removed.
	 */
	private String buildFilter(Map<String, String> attribs, String match, String filterKey) {
		List<String> options = new LinkedList<String>();
		for (Entry<String, String> entry : attribs.entrySet()) {
			String key = OSGiHeader.removeDuplicateMarker(entry.getKey());
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

	private static void buildFromHeader(String headerStr, Yield<Builder> output) {
		if (headerStr == null)
			return;
		Map<String, Map<String, String>> header = OSGiHeader.parseHeader(headerStr);

		for (Entry<String, Map<String, String>> entry : header.entrySet()) {
			String namespace = OSGiHeader.removeDuplicateMarker(entry.getKey());
			Builder builder = new Builder().setNamespace(namespace);

			Map<String, String> attribs = entry.getValue();
			Util.copyAttribsToBuilder(builder, attribs);
			output.yield(builder);
		}
	}

}
