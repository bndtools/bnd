package aQute.bnd.component;

import static java.util.stream.Collectors.joining;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.version.Version;
import aQute.bnd.xmlattribute.ExtensionDef;
import aQute.bnd.xmlattribute.Namespaces;
import aQute.bnd.xmlattribute.XMLAttributeFinder;
import aQute.lib.tag.Tag;

/**
 * This class just holds the information for the component, implementation, and
 * service/provide elements. The {@link #prepare(Analyzer)} method will check if
 * things are ok and the {@link #getTag()} method returns a tag if the prepare
 * method returns without any errors. The class uses {@link ReferenceDef} to
 * hold the references.
 */
class ComponentDef extends ExtensionDef {
	final static String				NAMESPACE_STEM	= "http://www.osgi.org/xmlns/scr";
	final PropertiesDef				properties;
	final PropertiesDef				factoryProperties;
	final Map<String, ReferenceDef>	references		= new LinkedHashMap<>();
	Version							version;
	String							name;
	String							factory;
	Boolean							immediate;
	ServiceScope					scope;
	ConfigurationPolicy				configurationPolicy;
	TypeRef							implementation;
	TypeRef							service[];
	String							activate;
	String							deactivate;
	String							modified;
	Boolean							enabled;
	String							xmlns;
	String[]						configurationPid;

	public ComponentDef(Analyzer analyzer, XMLAttributeFinder finder, Version minVersion) {
		super(finder);
		version = minVersion;
		properties = new PropertiesDef(analyzer);
		factoryProperties = new PropertiesDef(analyzer);
	}

	String effectiveName() {
		if (name != null)
			return name;
		if (implementation != null)
			return implementation.getFQN();
		return "<name not yet determined>";
	}

	/**
	 * Called to prepare. If will look for any errors or inconsistencies in the
	 * setup.
	 * 
	 * @param analyzer the analyzer to report errors and create references
	 * @throws Exception
	 */
	void prepare(Analyzer analyzer) throws Exception {

		prepareVersion(analyzer);

		if (implementation == null) {
			analyzer.error("No Implementation defined for component %s", name);
			return;
		}

		analyzer.referTo(implementation);

		if (name == null)
			name = implementation.getFQN();

		if (service != null && service.length > 0) {
			for (TypeRef interfaceName : service)
				analyzer.referTo(interfaceName);
		} else if (scope != null && scope != ServiceScope.BUNDLE)
			analyzer.warning("The servicefactory:=true directive is set but no service is provided, ignoring it");

		if (factory == null && !factoryProperties.isEmpty()) {
			analyzer.error("The factoryProperty and/or factoryProperies elements are used on a non-factory component");
		}
	}

	private void prepareVersion(Analyzer analyzer) throws Exception {

		for (ReferenceDef ref : references.values()) {
			ref.prepare(analyzer);
			updateVersion(ref.version);
		}
		if (configurationPolicy != null)
			updateVersion(AnnotationReader.V1_1);
		if (configurationPid != null)
			updateVersion(AnnotationReader.V1_2);
		if (modified != null)
			updateVersion(AnnotationReader.V1_1);
		if (!factoryProperties.isEmpty()) {
			updateVersion(AnnotationReader.V1_4);
		}

	}

	void sortReferences() {
		Map<String, ReferenceDef> temp = new TreeMap<>(references);
		references.clear();
		references.putAll(temp);
	}

	/**
	 * Returns a tag describing the component element.
	 * 
	 * @return a component element
	 */
	Tag getTag() {
		String xmlns = this.xmlns;
		if (xmlns == null && !version.equals(AnnotationReader.V1_0))
			xmlns = NAMESPACE_STEM + "/v" + version;
		Tag component = new Tag(xmlns == null ? "component" : "scr:component");
		Namespaces namespaces = null;
		if (xmlns != null) {

			namespaces = new Namespaces();
			namespaces.registerNamespace("scr", xmlns);
			addNamespaces(namespaces, xmlns);
			for (ReferenceDef ref : references.values())
				ref.addNamespaces(namespaces, xmlns);

			namespaces.addNamespaces(component);

		}
		component.addAttribute("name", name);

		if (configurationPolicy != null)
			component.addAttribute("configuration-policy", configurationPolicy.toString());

		if (enabled != null)
			component.addAttribute("enabled", enabled);

		if (immediate != null)
			component.addAttribute("immediate", immediate);

		if (factory != null)
			component.addAttribute("factory", factory);

		if (activate != null && !version.equals(AnnotationReader.V1_0))
			component.addAttribute("activate", activate);

		if (deactivate != null && !version.equals(AnnotationReader.V1_0))
			component.addAttribute("deactivate", deactivate);

		if (modified != null)
			component.addAttribute("modified", modified);

		if (configurationPid != null) {
			component.addAttribute("configuration-pid", Stream.of(configurationPid)
				.map(this::map$)
				.collect(joining(" ")));
		}

		addAttributes(component, namespaces);

		properties.propertyTags("property")
			.forEachOrdered(component::addContent);
		properties.propertiesTags("properties")
			.forEachOrdered(component::addContent);
		factoryProperties.propertyTags("factory-property")
			.forEachOrdered(component::addContent);
		factoryProperties.propertiesTags("factory-properties")
			.forEachOrdered(component::addContent);

		if (service != null && service.length != 0) {
			Tag s = new Tag(component, "service");
			if (scope != null) {// TODO check for DEFAULT???
				if (AnnotationReader.V1_3.compareTo(version) > 0) {
					if (scope == ServiceScope.PROTOTYPE) {
						throw new IllegalStateException("verification failed, pre 1.3 component with scope PROTOTYPE");
					}
					s.addAttribute("servicefactory", scope == ServiceScope.BUNDLE);
				} else {
					s.addAttribute("scope", scope.toString());
				}
			}

			for (TypeRef ss : service) {
				Tag provide = new Tag(s, "provide");
				provide.addAttribute("interface", ss.getFQN());
			}
		}

		for (ReferenceDef ref : references.values()) {
			Tag refTag = ref.getTag(namespaces);
			component.addContent(refTag);
		}

		Tag impl = new Tag(component, "implementation");
		impl.addAttribute("class", implementation.getFQN());

		return component;
	}

	private String map$(String v) {
		return "$".equals(v) ? name : v;
	}

	void updateVersion(Version version) {
		this.version = max(this.version, version);
	}

	static <T extends Comparable<T>> T max(T a, T b) {
		int n = a.compareTo(b);
		if (n >= 0)
			return a;
		return b;
	}

}