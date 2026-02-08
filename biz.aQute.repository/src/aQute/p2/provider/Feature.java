package aQute.p2.provider;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * the osgi.identity namespace with type=eclipse.feature.
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

	public Feature(Document document) {
		super(document);
	}

	public Feature(InputStream in) throws Exception {
		this(toDoc(in));
	}

	private static Document toDoc(InputStream in) throws Exception {
		try (Jar jar = new Jar("feature", in)) {
			aQute.bnd.osgi.Resource resource = jar.getResource("feature.xml");
			if (resource == null) {
				throw new IllegalArgumentException("JAR does not contain proper 'feature.xml");
			}
			DocumentBuilder db = XMLBase.dbf.newDocumentBuilder();
			Document doc = db.parse(resource.openInputStream());
			return doc;
		}
	}

	/**
	 * Parse the feature.xml document and populate all properties
	 */
	public void parse() throws Exception {
		// Parse root feature element attributes
		Node featureNode = getNodes("/feature").item(0);
		if (featureNode != null) {
			id = getAttribute(featureNode, "id");
			label = getAttribute(featureNode, "label");
			version = getAttribute(featureNode, "version");
			providerName = getAttribute(featureNode, "provider-name");
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

		// Create identity capability with type=eclipse.feature
		CapReqBuilder identity = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
		identity.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, id);
		identity.addAttribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, "eclipse.feature");
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
			String filter = String.format("(&(osgi.identity=%s)(type=eclipse.feature))", include.id);
			if (include.version != null && !include.version.equals("0.0.0")) {
				filter = String.format("(&(osgi.identity=%s)(type=eclipse.feature)(version=%s))", include.id,
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
			if (requirement.plugin != null) {
				req.addDirective("filter", String.format("(osgi.identity=%s)", requirement.plugin));
			} else if (requirement.feature != null) {
				req.addDirective("filter",
					String.format("(&(osgi.identity=%s)(type=eclipse.feature))", requirement.feature));
			}
			rb.addRequirement(req);
		}

		return rb.build();
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
