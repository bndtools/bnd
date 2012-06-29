package org.osgi.service.indexer.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
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
import org.osgi.service.indexer.impl.types.ScalarType;
import org.osgi.service.indexer.impl.types.SymbolicName;
import org.osgi.service.indexer.impl.types.VersionKey;
import org.osgi.service.indexer.impl.types.VersionRange;
import org.osgi.service.indexer.impl.util.Hex;
import org.osgi.service.indexer.impl.util.OSGiHeader;
import org.osgi.service.indexer.impl.util.QuotedTokenizer;
import org.osgi.service.indexer.impl.util.Yield;

class BundleAnalyzer implements ResourceAnalyzer {
	
	private static final String SHA_256 = "SHA-256";

	// Duplicate these constants here to avoid a compile-time dependency on OSGi R4.3
	private static final String PROVIDE_CAPABILITY = "Provide-Capability";
	private static final String REQUIRE_CAPABILITY = "Require-Capability";
	
	private final ThreadLocal<GeneratorState> state = new ThreadLocal<GeneratorState>();

	public void analyzeResource(Resource resource, List<Capability> capabilities, List<Requirement> requirements) throws Exception {
		doIdentity(resource, capabilities);
		doContent(resource, capabilities);
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
	}	

	private void doIdentity(Resource resource, List<? super Capability> caps) throws Exception {
		Manifest manifest = resource.getManifest();
		if (manifest == null)
			throw new IllegalArgumentException("Missing bundle manifest.");
		
		Attributes attribs = manifest.getMainAttributes();
		String fragmentHost = attribs.getValue(Constants.FRAGMENT_HOST);
		String identity = (fragmentHost == null) ? Namespaces.RESOURCE_TYPE_BUNDLE : Namespaces.RESOURCE_TYPE_FRAGMENT;
		
		SymbolicName bsn = getSymbolicName(resource);
		boolean singleton = Boolean.TRUE.toString().equalsIgnoreCase(bsn.getAttributes().get(Constants.SINGLETON_DIRECTIVE + ":"));
		
		Version version = getVersion(resource);
		
		Builder builder = new Builder()
				.setNamespace(Namespaces.NS_IDENTITY)
				.addAttribute(Namespaces.NS_IDENTITY, bsn.getName())
				.addAttribute(Namespaces.ATTR_IDENTITY_TYPE, identity)
				.addAttribute(Namespaces.ATTR_VERSION, version);
		if (singleton)
			builder.addDirective(Namespaces.DIRECTIVE_SINGLETON, Boolean.TRUE.toString());
		caps.add(builder.buildCapability());
	}

	private static Version getVersion(Resource resource) throws IOException {
		Manifest manifest = resource.getManifest();
		if (manifest == null)
			throw new IllegalArgumentException(String.format("Cannot identify version for resource %s: manifest unavailable", resource.getLocation()));
		String versionStr = manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
		Version version = (versionStr != null) ? new Version(versionStr) : Version.emptyVersion;
		return version;
	}
	
	SymbolicName getSymbolicName(Resource resource) throws IOException {
		Manifest manifest = resource.getManifest();
		if (manifest == null)
			throw new IllegalArgumentException(String.format("Cannot identify symbolic name for resource %s: manifest unavailable", resource.getLocation()));

		String header = manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
		if (header == null)
			throw new IllegalArgumentException("Not an OSGi R4+ bundle: missing 'Bundle-SymbolicName' entry from manifest.");

		Map<String, Map<String, String>> map = OSGiHeader.parseHeader(header);
		if (map.size() != 1)
			throw new IllegalArgumentException("Invalid format for Bundle-SymbolicName header.");

		Entry<String, Map<String, String>> entry = map.entrySet().iterator().next();
		return new SymbolicName(entry.getKey(), entry.getValue());
	}
	
	void setStateLocal(GeneratorState state) {
		this.state.set(state);
	}
	
	private GeneratorState getStateLocal() {
		return state.get();
	}
	
	private void doContent(Resource resource, List<? super Capability> capabilities) throws Exception {
		Builder builder = new Builder()
			.setNamespace(Namespaces.NS_CONTENT);
		
		String sha = calculateSHA(resource);
		builder.addAttribute(Namespaces.NS_CONTENT, sha);
		
		String location = calculateLocation(resource);
		builder.addAttribute(Namespaces.ATTR_CONTENT_URL, location);

		long size = resource.getSize();
		if (size > 0L) builder.addAttribute(Namespaces.ATTR_CONTENT_SIZE, size);
		
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
			if (stream != null) stream.close();
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
				result = urlTemplate.replaceAll("%s", getSymbolicName(resource).getName());
				result = result.replaceAll("%f", fileName);
				result = result.replaceAll("%p", dir);
				result = result.replaceAll("%v", "" + getVersion(resource));
			} else {
				result = dir + fileName;
			}
		}
		
		return result;
	}


	private static String translate(String value, Properties localStrings) {
		if (value == null)
			return null;
		
		if (!value.startsWith("%"))
			return value;
		
		value = value.substring(1);
		return localStrings.getProperty(value, value);
	}

	private static Properties loadLocalStrings(Resource resource) throws IOException {
		Properties props = new Properties();
		
		Attributes attribs = resource.getManifest().getMainAttributes();
		String path = attribs.getValue(Constants.BUNDLE_LOCALIZATION);
		if (path == null)
			path = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
		path += ".properties";
		
		Resource propsResource = resource.getChild(path);
		if (propsResource != null) {
			try {
				props.load(propsResource.getStream());
			} finally {
				propsResource.close();
			}
		}
		
		return props;
	}

	private void doBundleAndHost(Resource resource, List<? super Capability> caps) throws Exception {
		Builder bundleBuilder = new Builder().setNamespace(Namespaces.NS_WIRING_BUNDLE);
		Builder hostBuilder   = new Builder().setNamespace(Namespaces.NS_WIRING_HOST);
		boolean allowFragments = true;
		
		Attributes attribs = resource.getManifest().getMainAttributes();
		if (attribs.getValue(Constants.FRAGMENT_HOST) != null)
			return;
		
		SymbolicName bsn = getSymbolicName(resource);
		Version version = getVersion(resource);
		
		bundleBuilder.addAttribute(Namespaces.NS_WIRING_BUNDLE, bsn.getName())
			.addAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE, version);
		hostBuilder.addAttribute(Namespaces.NS_WIRING_HOST, bsn.getName())
			.addAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE, version);
		
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
			
			caps.add(builder.buildCapability());
		}
	}

	private void doImports(Resource resource, List<? super Requirement> reqs) throws Exception {
		Manifest manifest = resource.getManifest();
		
		String importsStr = manifest.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
		Map<String, Map<String, String>> imports = OSGiHeader.parseHeader(importsStr);
		for (Entry<String, Map<String, String>> entry: imports.entrySet()) {
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
			
			Builder builder = new Builder()
				.setNamespace(Namespaces.NS_WIRING_PACKAGE)
				.addDirective(Namespaces.DIRECTIVE_FILTER, filter.toString());
			
			for (Entry<String, String> attribEntry : entry.getValue().entrySet()) {
				String key = attribEntry.getKey();
				
				if (!Constants.VERSION_ATTRIBUTE.equalsIgnoreCase(key) && !"specification-version".equals(key)) {
					if (key.endsWith(":")) {
						String directive = key.substring(0, key.length() - 1);
						builder.addDirective(directive, attribEntry.getValue());
					} else {
						builder.addAttribute(key, attribEntry.getValue());
					}
				}
			}

			reqs.add(builder.buildRequirement());
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
			
			Builder builder = new Builder()
				.setNamespace(Namespaces.NS_WIRING_BUNDLE)
				.addDirective(Namespaces.DIRECTIVE_FILTER, filter.toString());
			
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
			
			Builder builder = new Builder()
				.setNamespace(Namespaces.NS_WIRING_HOST)
				.addDirective(Namespaces.DIRECTIVE_FILTER, filter.toString());
			
			reqs.add(builder.buildRequirement());
		}
	}
	
	private void doExportService(Resource resource, List<? super Capability> caps) throws Exception {
		@SuppressWarnings("deprecation")
		String exportsStr = resource.getManifest().getMainAttributes().getValue(Constants.EXPORT_SERVICE);
		Map<String, Map<String, String>> exports = OSGiHeader.parseHeader(exportsStr);
		
		for (Entry<String, Map<String, String>> export : exports.entrySet()) {
			String service = OSGiHeader.removeDuplicateMarker(export.getKey());
			Builder builder = new Builder()
					.setNamespace(Namespaces.NS_WIRING_SERVICE)
					.addAttribute(Namespaces.NS_WIRING_SERVICE, service);
			caps.add(builder.buildCapability());
		}
	}
	
	private void doImportService(Resource resource, List<? super Requirement> reqs) throws Exception {
		@SuppressWarnings("deprecation")
		String importsStr = resource.getManifest().getMainAttributes().getValue(Constants.IMPORT_SERVICE);
		Map<String, Map<String, String>> imports = OSGiHeader.parseHeader(importsStr);
		
		for (Entry<String, Map<String, String>> imp : imports.entrySet()) {
			String service = OSGiHeader.removeDuplicateMarker(imp.getKey());
			StringBuilder filter = new StringBuilder();
			filter.append('(').append(Namespaces.NS_WIRING_SERVICE).append('=').append(service).append(')');
			
			Builder builder = new Builder()
				.setNamespace(Namespaces.NS_WIRING_SERVICE)
				.addDirective(Namespaces.DIRECTIVE_FILTER, filter.toString())
				.addDirective(Namespaces.DIRECTIVE_EFFECTIVE, Namespaces.EFFECTIVE_ACTIVE);
			reqs.add(builder.buildRequirement());
		}
	}
	
	private void doBREE(Resource resource, List<? super Requirement> reqs) throws Exception {
		@SuppressWarnings("deprecation")
		String breeStr = resource.getManifest().getMainAttributes().getValue(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		Map<String, Map<String, String>> brees = OSGiHeader.parseHeader(breeStr);
		
		for (String bree : brees.keySet()) {
			StringBuilder filter = new StringBuilder();
			filter.append("(ee=");
			filter.append(OSGiHeader.removeDuplicateMarker(bree));
			filter.append(')');
			
			Builder builder = new Builder()
				.setNamespace(Namespaces.NS_WIRING_EE)
				.addDirective(Namespaces.DIRECTIVE_FILTER, filter.toString());
			
			reqs.add(builder.buildRequirement());
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
	
	private static void buildFromHeader(String headerStr, Yield<Builder> output) {
		if (headerStr == null) return;
		Map<String, Map<String, String>> header = OSGiHeader.parseHeader(headerStr);
		
		for (Entry<String, Map<String, String>> entry : header.entrySet()) {
			String namespace = OSGiHeader.removeDuplicateMarker(entry.getKey());
			Builder builder = new Builder().setNamespace(namespace);
			
			for (Entry<String, String> attrib : entry.getValue().entrySet()) {
				String key = attrib.getKey();
				
				if (key.endsWith(":")) {
					String directiveName = key.substring(0, key.length() - 1);
					builder.addDirective(directiveName, attrib.getValue());
				} else {
					int colonIndex = key.lastIndexOf(":");
					
					String name;
					String typeStr;
					if (colonIndex > -1) {
						name = key.substring(0, colonIndex);
						typeStr = key.substring(colonIndex + 1);
					} else {
						name = key;
						typeStr = ScalarType.String.name();
					}
					
					Object value = parseValue(attrib.getValue(), typeStr);
					builder.addAttribute(name, value);
				}
			}
			output.yield(builder);
		}
	}
	
	static Object parseValue(String value, String typeStr) {
		Object result;
		
		if (typeStr.startsWith("List<")) {
			typeStr = typeStr.substring("List<".length(), typeStr.length() - 1);
			result = parseListValue(value, typeStr);
		} else {
			result = parseScalarValue(value, typeStr);
		}
		
		return result;
	}

	static List<?> parseListValue(String value, String typeStr) throws IllegalArgumentException {
		
		QuotedTokenizer tokenizer = new QuotedTokenizer(value, ",");
		String[] tokens = tokenizer.getTokens();
		List<Object> result = new ArrayList<Object>(tokens.length);
		for (String token : tokens)
			result.add(parseScalarValue(token, typeStr));
		
		return result;
	}

	static Object parseScalarValue(String value, String typeStr) throws IllegalArgumentException {
		ScalarType type = Enum.valueOf(ScalarType.class, typeStr);
		switch (type) {
		case String:
			return value;
		case Long:
			return Long.valueOf(value);
		case Double:
			return Double.valueOf(value);
		case Version:
			return new Version(value);
		default:
			throw new IllegalArgumentException(typeStr);
		}
	}

}

