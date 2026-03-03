package aQute.p2.provider;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;

/**
 * Parser for Eclipse feature.xml files. This class parses Eclipse features and
 * creates OSGi Resource representations with capabilities and requirements in
 * the osgi.identity namespace with type=org.eclipse.update.feature.
 */
public class Feature extends XMLBase {

	/**
	 * Represents a plugin reference in a feature
	 */
	public static class Plugin {
		public String	id;
		public String	version;
		public boolean	unpack;
		public boolean	fragment;
		public String	os;
		public String	ws;
		public String	arch;
		public String	download_size;
		public String	install_size;

		@Override
		public String toString() {
			return id + ":" + version;
		}
	}

	/**
	 * Represents an included feature reference
	 */
	public static class Includes {
		public String	id;
		public String	version;
		public boolean	optional;
		public String	os;
		public String	ws;
		public String	arch;

		@Override
		public String toString() {
			return id + ":" + version + (optional ? " (optional)" : "");
		}
	}

	/**
	 * Represents a required feature or plugin
	 */
	public static class Requires {
		public String	plugin;
		public String	feature;
		public String	version;
		public String	match;

		@Override
		public String toString() {
			return (plugin != null ? "plugin:" + plugin : "feature:" + feature) + ":" + version;
		}
	}

	// Feature properties from feature.xml root element
	public String				id;
	public String				label;
	public String				version;
	public String				providerName;
	public String				plugin;
	public String				image;
	public String				application;
	public String				os;
	public String				ws;
	public String				arch;
	public String				nl;
	public String				colocationAffinity;
	public String				primary;
	public String				exclusive;
	public String				licenseFeature;
	public String				licenseFeatureVersion;

	// Child elements
	private List<Plugin>		plugins		= new ArrayList<>();
	private List<Includes>		includes	= new ArrayList<>();
	private List<Requires>		requires	= new ArrayList<>();

	// Feature properties for resolving %key references
	private Properties			properties	= new Properties();

	public Feature(Document document) {
		super(document);
	}

	public Feature(Document document, Properties properties) {
		super(document);
		this.properties = properties != null ? properties : new Properties();
	}

	public Feature(InputStream in) throws Exception {
		this(loadFromJar(in));
	}

	private Feature(DocumentAndProperties docAndProps) {
		super(docAndProps.document);
		this.properties = docAndProps.properties;
	}

	private static class DocumentAndProperties {
		Document document;
		Properties properties;

		DocumentAndProperties(Document document, Properties properties) {
			this.document = document;
			this.properties = properties != null ? properties : new Properties();
		}
	}

	private static DocumentAndProperties loadFromJar(InputStream in) throws Exception {
		try (Jar jar = new Jar("feature", in)) {
			// Load feature.xml
			aQute.bnd.osgi.Resource featureXml = jar.getResource("feature.xml");
			if (featureXml == null) {
				throw new IllegalArgumentException("JAR does not contain proper 'feature.xml");
			}
			DocumentBuilder db = XMLBase.dbf.newDocumentBuilder();
			Document doc = db.parse(featureXml.openInputStream());

			// Load feature.properties if present
			Properties props = new Properties();
			aQute.bnd.osgi.Resource featureProps = jar.getResource("feature.properties");
			if (featureProps != null) {
				try (InputStream propsIn = featureProps.openInputStream()) {
					props.load(propsIn);
				}
			}

			return new DocumentAndProperties(doc, props);
		}
	}

	/**
	 * Resolve property references in the form %key by looking up in feature.properties
	 */
	private String resolveProperty(String value) {
		if (value != null && value.startsWith("%")) {
			String key = value.substring(1);
			String resolved = properties.getProperty(key);
			return resolved != null ? resolved : value;
		}
		return value;
	}

	/**
	 * Parse the feature.xml document and populate all properties
	 */
	public void parse() throws Exception {
		// Parse root feature element attributes
		Node featureNode = getNodes("/feature").item(0);
		if (featureNode != null) {
			id = getAttribute(featureNode, "id");
			label = resolveProperty(getAttribute(featureNode, "label"));
			version = getAttribute(featureNode, "version");
			providerName = resolveProperty(getAttribute(featureNode, "provider-name"));
			plugin = getAttribute(featureNode, "plugin");
			image = getAttribute(featureNode, "image");
			application = getAttribute(featureNode, "application");
			os = getAttribute(featureNode, "os");
			ws = getAttribute(featureNode, "ws");
			arch = getAttribute(featureNode, "arch");
			nl = getAttribute(featureNode, "nl");
			colocationAffinity = getAttribute(featureNode, "colocation-affinity");
			primary = getAttribute(featureNode, "primary");
			exclusive = getAttribute(featureNode, "exclusive");
			licenseFeature = getAttribute(featureNode, "license-feature");
			licenseFeatureVersion = getAttribute(featureNode, "license-feature-version");
		}

		// Parse plugins
		plugins = getPlugins();

		// Parse includes
		includes = getIncludes();

		// Parse requires
		requires = getRequires();
	}

	/**
	 * Get all plugin references in this feature
	 */
	public List<Plugin> getPlugins() throws Exception {
		NodeList nodes = getNodes("/feature/plugin");
		List<Plugin> result = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node item = nodes.item(i);
			Plugin plugin = getFromType(item, Plugin.class);
			result.add(plugin);
		}
		return result;
	}

	/**
	 * Get all included features
	 */
	public List<Includes> getIncludes() throws Exception {
		NodeList nodes = getNodes("/feature/includes");
		List<Includes> result = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node item = nodes.item(i);
			Includes include = getFromType(item, Includes.class);
			result.add(include);
		}
		return result;
	}

	/**
	 * Get all required features and plugins
	 */
	public List<Requires> getRequires() throws Exception {
		NodeList nodes = getNodes("/feature/requires/import");
		List<Requires> result = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node item = nodes.item(i);
			Requires req = getFromType(item, Requires.class);
			result.add(req);
		}
		return result;
	}

	/**
	 * Create an OSGi Resource representation of this feature with capabilities
	 * and requirements
	 */
	public Resource toResource() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();

		// Create identity capability with type=org.eclipse.update.feature
		CapReqBuilder identity = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
		identity.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, id);
		identity.addAttribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, "org.eclipse.update.feature");
		if (version != null) {
			try {
				Version v = Version.parseVersion(version);
				identity.addAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, v);
			} catch (IllegalArgumentException e) {
				// If version parsing fails, store as string
				identity.addAttribute("version.string", version);
			}
		}

		// Add feature properties as attributes
		Map<String, Object> props = new HashMap<>();
		if (label != null)
			props.put("label", label);
		if (providerName != null)
			props.put("provider-name", providerName);
		if (plugin != null)
			props.put("plugin", plugin);
		if (image != null)
			props.put("image", image);
		if (application != null)
			props.put("application", application);
		if (os != null)
			props.put("os", os);
		if (ws != null)
			props.put("ws", ws);
		if (arch != null)
			props.put("arch", arch);
		if (nl != null)
			props.put("nl", nl);
		if (colocationAffinity != null)
			props.put("colocation-affinity", colocationAffinity);
		if (primary != null)
			props.put("primary", primary);
		if (exclusive != null)
			props.put("exclusive", exclusive);
		if (licenseFeature != null)
			props.put("license-feature", licenseFeature);
		if (licenseFeatureVersion != null)
			props.put("license-feature-version", licenseFeatureVersion);

		for (Map.Entry<String, Object> entry : props.entrySet()) {
			identity.addAttribute(entry.getKey(), entry.getValue());
		}

		rb.addCapability(identity);

		// Create requirements for included plugins
		for (Plugin plugin : plugins) {
			CapReqBuilder req = new CapReqBuilder("osgi.identity");
			req.addDirective("filter", String.format("(osgi.identity=%s)", plugin.id));
			if (plugin.version != null && !plugin.version.equals("0.0.0")) {
				// Add version constraint
				req.addDirective("filter",
					String.format("(&(osgi.identity=%s)(version=%s))", plugin.id, plugin.version));
			}
			rb.addRequirement(req);
		}

		// Create requirements for included features
		for (Includes include : includes) {
			CapReqBuilder req = new CapReqBuilder("osgi.identity");
			String filter = String.format("(&(osgi.identity=%s)(type=org.eclipse.update.feature))", include.id);
			if (include.version != null && !include.version.equals("0.0.0")) {
				filter = String.format("(&(osgi.identity=%s)(type=org.eclipse.update.feature)(version=%s))", include.id,
					include.version);
			}
			req.addDirective("filter", filter);
			if (include.optional) {
				req.addDirective("resolution", "optional");
			}
			rb.addRequirement(req);
		}

		// Create requirements for required features and plugins
		for (Requires requirement : requires) {
			CapReqBuilder req = new CapReqBuilder("osgi.identity");
			String reqIdentity = requirement.plugin != null ? requirement.plugin : requirement.feature;
			
			// Build the filter with version constraint if present
			String filter = buildRequirementFilter(reqIdentity, requirement.version, requirement.match,
				requirement.feature != null);
			req.addDirective("filter", filter);
			rb.addRequirement(req);
		}

		return rb.build();
	}

	/**
	 * Build an LDAP filter for a requirement with version constraint.
	 * Handles Eclipse match rules: "perfect", "equivalent", "compatible", "greaterOrEqual"
	 * 
	 * @param identity the identity (plugin or feature id)
	 * @param version the version string (may be null)
	 * @param match the match rule (may be null, defaults to "greaterOrEqual")
	 * @param isFeature true if this is a feature (adds type filter)
	 * @return LDAP filter string
	 */
	public String buildRequirementFilter(String identity, String version, String match, boolean isFeature) {
		StringBuilder filter = new StringBuilder();
		
		// Determine if we need version filtering
		boolean hasVersion = version != null && !version.equals("0.0.0");
		
		// Start with identity
		if (isFeature || hasVersion) {
			filter.append("(&");
		}
		filter.append("(osgi.identity=")
			.append(identity)
			.append(")");
		
		// Add type for features
		if (isFeature) {
			filter.append("(type=org.eclipse.update.feature)");
		}
		
		// Add version constraint if present
		if (hasVersion) {
			String versionFilter = buildVersionFilter(version, match);
			if (versionFilter != null) {
				filter.append(versionFilter);
			}
		}
		
		if (isFeature || hasVersion) {
			filter.append(")");
		}
		
		return filter.toString();
	}

	/**
	 * Build a version filter based on Eclipse match rules.
	 * Eclipse match rules are:
	 * - perfect: exact version match
	 * - equivalent: same major.minor.micro, any qualifier
	 * - compatible: same major.minor, micro >= specified  
	 * - greaterOrEqual: version >= specified (default if no match specified)
	 * 
	 * @param version the version string
	 * @param match the match rule (may be null)
	 * @return version filter fragment (without outer parentheses)
	 */
	public String buildVersionFilter(String version, String match) {
		if (version == null || version.isEmpty() || version.equals("0.0.0")) {
			return null;
		}
		
		// Default to greaterOrEqual if no match specified
		if (match == null || match.isEmpty()) {
			match = "greaterOrEqual";
		}
		
		try {
			Version v = Version.parseVersion(version);
			
			switch (match.toLowerCase()) {
				case "perfect":
					// Exact version match
					return String.format("(version=%s)", version);
					
				case "equivalent":
					// Same major.minor.micro, any qualifier
					// Range: [major.minor.micro, major.minor.(micro+1))
					Version lower = v;
					Version upper = new Version(v.getMajor(), v.getMinor(), v.getMicro() + 1);
					return String.format("(version>=%s)(!(version>=%s))", lower, upper);
					
				case "compatible":
					// Same major.minor, micro >= specified
					// Range: [major.minor.micro, major.(minor+1).0)
					Version lowerCompat = v;
					Version upperCompat = new Version(v.getMajor(), v.getMinor() + 1, 0);
					return String.format("(version>=%s)(!(version>=%s))", lowerCompat, upperCompat);
					
				case "greaterorequal":
				default:
					// Unbounded range: version >= specified
					return String.format("(version>=%s)", v);
			}
		} catch (Exception e) {
			// If version parsing fails, fall back to greaterOrEqual with the raw string
			return String.format("(version>=%s)", version);
		}
	}

	/**
	 * Build a platform filter expression from os, ws, and arch attributes.
	 * Returns null if no platform attributes are specified.
	 * 
	 * @param os the operating system (may be null)
	 * @param ws the windowing system (may be null)
	 * @param arch the architecture (may be null)
	 * @return LDAP filter string for platform attributes, or null if none specified
	 */
	public static String buildPlatformFilter(String os, String ws, String arch) {
		List<String> filters = new ArrayList<>();
		
		if (os != null && !os.isEmpty()) {
			filters.add(String.format("(osgi.os=%s)", os));
		}
		if (ws != null && !ws.isEmpty()) {
			filters.add(String.format("(osgi.ws=%s)", ws));
		}
		if (arch != null && !arch.isEmpty()) {
			filters.add(String.format("(osgi.arch=%s)", arch));
		}
		
		if (filters.isEmpty()) {
			return null;
		}
		
		if (filters.size() == 1) {
			return filters.get(0);
		}
		
		// Combine multiple filters with AND
		StringBuilder result = new StringBuilder("(&");
		for (String filter : filters) {
			result.append(filter);
		}
		result.append(")");
		
		return result.toString();
	}

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public String getVersion() {
		return version;
	}

	public String getProviderName() {
		return providerName;
	}

	@Override
	public String toString() {
		return "Feature [id=" + id + ", version=" + version + ", plugins=" + plugins.size() + ", includes="
			+ includes.size() + ", requires=" + requires.size() + "]";
	}
}
