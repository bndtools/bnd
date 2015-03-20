package aQute.bnd.component;

import java.lang.reflect.*;
import java.util.*;

import org.osgi.service.component.annotations.*;

import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.version.*;
import aQute.lib.collections.*;
import aQute.lib.tag.*;

/**
 * This class just holds the information for the component, implementation, and
 * service/provide elements. The {@link #prepare(Analyzer)} method will check if
 * things are ok and the {@link #getTag()} method returns a tag if the prepare
 * method returns without any errors. The class uses {@link ReferenceDef} to
 * hold the references.
 */
class ComponentDef {

	final static String				NAMESPACE_STEM	= "http://www.osgi.org/xmlns/scr";
	final static String 			MARKER 			= new String("|marker");
	final List<String>				properties		= new ArrayList<String>();
	final MultiMap<String,String>	property		= new MultiMap<String,String>(); //key is property name
	final Map<String, String>		propertyType	= new HashMap<String, String>();
	final Map<String,ReferenceDef>	references		= new LinkedHashMap<String,ReferenceDef>();
	private final Map<String, String>		attributes		= new HashMap<String, String>();
	private final Map<String, String>		namespaces		= new HashMap<String, String>();

	Version							version			= AnnotationReader.V1_0;
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
	List<Tag>						propertyTags	= new ArrayList<Tag>();

	public String registerNamespace(final String prefix, String namespace) {
		if (namespaces.containsValue(namespace)) {
			for (Map.Entry<String, String> entry: namespaces.entrySet()) {
				if (entry.getValue().equals(namespace)) {
					return entry.getKey();
				}
			}
		}
		String prefix2 = prefix;
		int i = 1;
		while (namespaces.containsKey(prefix2)) {
			if (namespaces.get(prefix2).equals(namespace)) {
				return prefix2;
			}
			prefix2 = prefix + i++;
		}
		namespaces.put(prefix2, namespace);
		return prefix2;
	}
	
	public void addExtensionAttribute(String prefix, String key, String value) {
		attributes.put(prefix + ":" + key, value);
	}

	/**
	 * Called to prepare. If will look for any errors or inconsistencies in the
	 * setup.
	 * 
	 * @param analyzer
	 *            the analyzer to report errors and create references
	 * @throws Exception
	 */
	void prepare(Analyzer analyzer) throws Exception {

		prepareVersion(analyzer);


		if (implementation == null) {
			analyzer.error("No Implementation defined for component " + name);
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

		for (Map.Entry<String,List<String>> kvs : property.entrySet()) {
			Tag property = new Tag("property");
			String name = kvs.getKey();
			String type = propertyType.get(name);

			property.addAttribute("name", name);
			if (type != null) {
				property.addAttribute("type", type);
			}
			if (kvs.getValue().size() == 1) {
				String value = kvs.getValue().get(0);
				value = check(type, value, analyzer);
				property.addAttribute("value", value);
			} else {
				StringBuilder sb = new StringBuilder();

				String del = "";
				for (String v : kvs.getValue()) {
					if (v == MARKER) {
						continue;
					}
					sb.append(del);
					v = check(type, v, analyzer);
					sb.append(v);
					del = "\n";
				}
				property.addContent(sb.toString());
			}
			propertyTags.add(property);
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
			
	}
	
	void sortReferences() {
		Map<String, ReferenceDef> temp = new TreeMap<String,ReferenceDef>(references);
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
		if (xmlns == null && version != AnnotationReader.V1_0)
			xmlns = NAMESPACE_STEM + "/v" + version;
		Tag component = new Tag(xmlns == null? "component": "scr:component");
		if (xmlns != null)
			component.addAttribute("xmlns:scr", xmlns);
		
		for (Map.Entry<String, String> a: namespaces.entrySet()) {
			component.addAttribute("xmlns:" + a.getKey(), a.getValue());
		}

		for (Map.Entry<String, String> a: attributes.entrySet()) {
			component.addAttribute(a.getKey(), a.getValue());
		}

		component.addAttribute("name", name);

		if (configurationPolicy != null)
			component.addAttribute("configuration-policy", configurationPolicy.toString().toLowerCase());

		if (enabled != null)
			component.addAttribute("enabled", enabled);

		if (immediate != null)
			component.addAttribute("immediate", immediate);

		if (factory != null)
			component.addAttribute("factory", factory);

		if (activate != null && version != AnnotationReader.V1_0)
			component.addAttribute("activate", activate);

		if (deactivate != null && version != AnnotationReader.V1_0)
			component.addAttribute("deactivate", deactivate);

		if (modified != null)
			component.addAttribute("modified", modified);

		if (configurationPid != null) {
			StringBuilder b = new StringBuilder();
			String space = "";
			for (String pid: configurationPid) {
				if ("$".equals(pid))
					pid = name;
				b.append(space).append(pid);
				space = " ";
			}
			component.addAttribute("configuration-pid", b.toString());
		}
		Tag impl = new Tag(component, "implementation");
		impl.addAttribute("class", implementation.getFQN());

		if (service != null && service.length != 0) {
			Tag s = new Tag(component, "service");
			if (scope != null) {//TODO check for DEFAULT???
				if (AnnotationReader.V1_3.compareTo(version) >0 ) {
					if (scope == ServiceScope.PROTOTYPE) {
						throw new IllegalStateException("verification failed, pre 1.3 component with scope PROTOTYPE");
					}
				    s.addAttribute("servicefactory", scope == ServiceScope.BUNDLE);
				} else {
					s.addAttribute("scope", scope.toString().toLowerCase());
				}
			}

			for (TypeRef ss : service) {
				Tag provide = new Tag(s, "provide");
				provide.addAttribute("interface", ss.getFQN());
			}
		}

		for (ReferenceDef ref : references.values()) {
			Tag refTag = ref.getTag();
			component.addContent(refTag);
		}

		for (Tag tag : propertyTags)
			component.addContent(tag);

		for (String entry : properties) {
			Tag properties = new Tag(component, "properties");
			properties.addAttribute("entry", entry);
		}
		return component;
	}

	private String check(String type, String v, Analyzer analyzer) {
		if (type == null)
			return v;

		try {
			if ( type.equals("Char"))
				type = "Character";
			
			Class< ? > c = Class.forName("java.lang." + type);
			if (c == String.class)
				return v;

			v = v.trim();
			if (c == Character.class)
				c = Integer.class;
			Method m = c.getMethod("valueOf", String.class);
			m.invoke(null, v);
		}
		catch (ClassNotFoundException e) {
			analyzer.error("Invalid data type %s", type);
		}
		catch (NoSuchMethodException e) {
			analyzer.error("Cannot convert data %s to type %s", v, type);
		}
		catch (NumberFormatException e) {
			analyzer.error("Not a valid number %s for %s, %s", v, type, e.getMessage());
		}
		catch (Exception e) {
			analyzer.error("Cannot convert data %s to type %s", v, type);
		}
		return v;
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