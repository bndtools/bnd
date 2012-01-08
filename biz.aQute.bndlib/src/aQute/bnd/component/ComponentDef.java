package aQute.bnd.component;

import java.util.*;

import org.osgi.service.component.annotations.*;

import aQute.lib.collections.*;
import aQute.lib.osgi.*;
import aQute.lib.tag.*;
import aQute.libg.version.*;

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
	final MultiMap<String, String>	property		= new MultiMap<String, String>();
	final Map<String, ReferenceDef>	references		= new TreeMap<String, ReferenceDef>();

	Version							version			= AnnotationReader.V1_1;
	String							name;
	String							factory;
	Boolean							immediate;
	Boolean							servicefactory;
	ConfigurationPolicy				configurationPolicy;
	String							implementation;
	String							service[];
	String							activate;
	String							deactivate;
	String							modified;
	Boolean							enabled;
	String							xmlns;
	String							configurationPid;

	/**
	 * Called to prepare. If will look for any errors or inconsistencies in the
	 * setup.
	 * 
	 * @param analyzer
	 *            the analyzer to report errors and create references
	 */
	void prepare(Analyzer analyzer) {

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
			name = implementation;

		if (service != null && service.length > 0) {
			for (String interfaceName : service)
				analyzer.referTo(interfaceName);
		} else if (servicefactory != null && servicefactory)
			analyzer.warning("The servicefactory:=true directive is set but no service is provided, ignoring it");
		
		if ( configurationPid != null)
			version = ReferenceDef.max(version,AnnotationReader.V1_2);
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
			component.addAttribute("xmlns:scr", NAMESPACE_STEM + "/" + version);

		component.addAttribute("name", name);

		if (servicefactory != null)
			component.addAttribute("servicefactory", servicefactory);

		if (configurationPolicy != null)
			component.addAttribute("configuration-policy", configurationPolicy.toString()
					.toLowerCase());

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
		impl.addAttribute("class", implementation);

		if (service != null && service.length != 0) {
			Tag s = new Tag(component, "service");
			if (servicefactory != null && servicefactory)
				s.addAttribute("servicefactory", true);

			for (String ss : service) {
				Tag provide = new Tag(s, "provide");
				provide.addAttribute("interface", ss);
			}
		}

		for (ReferenceDef ref : references.values()) {
			Tag refTag = ref.getTag();
			component.addContent(refTag);
		}

		for (Map.Entry<String, Set<String>> kvs : property.entrySet()) {
			Tag property = new Tag(component, "property");
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
			StringBuffer sb = new StringBuffer();

			String del = "";
			for (String v : kvs.getValue()) {
				sb.append(del);
				sb.append(v);
				del = "\n";
			}
			property.addContent(sb.toString());
		}

		for (String entry : properties) {
			Tag properties = new Tag(component, "properties");
			properties.addAttribute("entry", entry);
		}
		return component;
	}

}