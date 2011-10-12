package aQute.bnd.component;

import java.util.*;

import org.osgi.service.component.annotations.*;

import aQute.lib.collections.*;
import aQute.lib.osgi.*;
import aQute.lib.tag.*;
import aQute.libg.version.*;

class ComponentDef {
	final static String				NAMESPACE_STEM	= "http://www.osgi.org/xmlns/scr";
	Version							version			= new Version("1.1.0");
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
	final List<String>				properties		= new ArrayList<String>();
	final MultiMap<String, String>	property		= new MultiMap<String, String>();
	final Map<String, ReferenceDef>	references		= new TreeMap<String, ReferenceDef>();

	void prepare(Analyzer analyzer) {

		for (ReferenceDef ref : references.values()) {
			ref.prepare(analyzer);
			if (ref.version.compareTo(version) > 0)
				version = ref.version;
		}

		if (implementation == null)
			analyzer.error("No Implementation defined for component " + name);
		else
			analyzer.referTo(implementation);

		name = implementation;

		if (service != null && service.length > 0) {
			for (String interfaceName : service)
				analyzer.referTo(interfaceName);
		} else if (servicefactory != null && servicefactory)
			analyzer.warning("The servicefactory:=true directive is set but no service is provided, ignoring it");
	}

	Tag getTag() {
		Tag component = new Tag("scr:component");
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