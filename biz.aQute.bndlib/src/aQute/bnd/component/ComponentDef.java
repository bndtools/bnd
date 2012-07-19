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
	final List<String>				properties		= new ArrayList<String>();
	final MultiMap<String,String>	property		= new MultiMap<String,String>();
	final Map<String,ReferenceDef>	references		= new TreeMap<String,ReferenceDef>();

	Version							version			= AnnotationReader.V1_1;
	String							name;
	String							factory;
	Boolean							immediate;
	Boolean							servicefactory;
	ConfigurationPolicy				configurationPolicy;
	TypeRef							implementation;
	TypeRef							service[];
	String							activate;
	String							deactivate;
	String							modified;
	Boolean							enabled;
	String							xmlns;
	String							configurationPid;
	List<Tag>						propertyTags	= new ArrayList<Tag>();

	/**
	 * Called to prepare. If will look for any errors or inconsistencies in the
	 * setup.
	 * 
	 * @param analyzer
	 *            the analyzer to report errors and create references
	 * @throws Exception
	 */
	void prepare(Analyzer analyzer) throws Exception {

		for (ReferenceDef ref : references.values()) {
			ref.prepare(analyzer);
			if (ref.version.compareTo(version) > 0)
				version = ref.version;
		}

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
		} else if (servicefactory != null && servicefactory)
			analyzer.warning("The servicefactory:=true directive is set but no service is provided, ignoring it");

		if (configurationPid != null)
			version = ReferenceDef.max(version, AnnotationReader.V1_2);

		for (Map.Entry<String,List<String>> kvs : property.entrySet()) {
			Tag property = new Tag("property");
			String name = kvs.getKey();
			String type = null;
			int n = name.indexOf(':');
			if (n > 0) {
				type = name.substring(n + 1);
				name = name.substring(0, n);
			}

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

	/**
	 * Returns a tag describing the component element.
	 * 
	 * @return a component element
	 */
	Tag getTag() {
		Tag component = new Tag("scr:component");
		if (xmlns != null)
			component.addAttribute("xmlns:scr", xmlns);
		else
			component.addAttribute("xmlns:scr", NAMESPACE_STEM + "/v" + version);

		component.addAttribute("name", name);

		if (servicefactory != null)
			component.addAttribute("servicefactory", servicefactory);

		if (configurationPolicy != null)
			component.addAttribute("configuration-policy", configurationPolicy.toString().toLowerCase());

		if (enabled != null)
			component.addAttribute("enabled", enabled);

		if (immediate != null)
			component.addAttribute("immediate", immediate);

		if (factory != null)
			component.addAttribute("factory", factory);

		if (activate != null)
			component.addAttribute("activate", activate);

		if (deactivate != null)
			component.addAttribute("deactivate", deactivate);

		if (modified != null)
			component.addAttribute("modified", modified);

		if (configurationPid != null)
			component.addAttribute("configuration-pid", configurationPid);

		Tag impl = new Tag(component, "implementation");
		impl.addAttribute("class", implementation.getFQN());

		if (service != null && service.length != 0) {
			Tag s = new Tag(component, "service");
			if (servicefactory != null && servicefactory)
				s.addAttribute("servicefactory", true);

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
}