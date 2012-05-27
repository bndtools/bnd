package aQute.bnd.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.lib.tag.Tag;
import aQute.bnd.version.Version;

public class HeaderReader extends Processor {
	final static Pattern		PROPERTY_PATTERN		= Pattern
	.compile("([^=]+([:@](Boolean|Byte|Char|Short|Integer|Long|Float|Double|String))?)\\s*=(.*)");
	
    private Analyzer analyzer;

    public HeaderReader(Analyzer analyzer) {
    	this.analyzer = analyzer;
    }
    
	public Tag createComponentTag(String name, String impl, Map<String, String> info)
	throws Exception {
		ComponentDef cd = new ComponentDef();
		cd.name = name;
		if (info.get(COMPONENT_ENABLED) != null)
			cd.enabled = Boolean.valueOf(info.get(COMPONENT_ENABLED));
		cd.factory = info.get(COMPONENT_FACTORY);
		if (info.get(COMPONENT_IMMEDIATE) != null) 
		    cd.immediate = Boolean.valueOf(info.get(COMPONENT_IMMEDIATE));
		if (info.get(COMPONENT_CONFIGURATION_POLICY) != null)
		    cd.configurationPolicy = ConfigurationPolicy.valueOf(info.get(COMPONENT_CONFIGURATION_POLICY).toUpperCase());
		cd.activate = checkIdentifier(COMPONENT_ACTIVATE, info.get(COMPONENT_ACTIVATE));
		cd.deactivate = checkIdentifier(COMPONENT_DEACTIVATE, info.get(COMPONENT_DEACTIVATE));
		cd.modified = checkIdentifier(COMPONENT_MODIFIED, info.get(COMPONENT_MODIFIED));
		
		cd.implementation = analyzer.getTypeRefFromFQN(impl == null? name: impl);
		

		String provides = info.get(COMPONENT_PROVIDE);
		if (info.get(COMPONENT_SERVICEFACTORY) != null) {
			if (provides != null)
			    cd.servicefactory = Boolean.valueOf(info.get(COMPONENT_SERVICEFACTORY));
			else
				warning("The servicefactory:=true directive is set but no service is provided, ignoring it");
		}

		if (cd.servicefactory != null && cd.servicefactory  && cd.immediate != null && cd.immediate) {
			// TODO can become error() if it is up to me
			warning("For a Service Component, the immediate option and the servicefactory option are mutually exclusive for %(%s)",
					name, impl);
		}
		provide(cd, provides, impl);
		properties(cd, info, name);
		reference(info, impl, cd);
		//compute namespace after references, an updated method means ds 1.2.
		cd.xmlns = getNamespace(info, cd);
		cd.prepare(analyzer);
		return cd.getTag();

	}

	private String checkIdentifier(String name, String value) {
		if (value != null) {
			if (!Verifier.isIdentifier(value)) {
				error("Component attribute %s has value %s but is not a Java identifier",
						name, value);
				return null;
			}
		}
		return value;
	}

	/**
	 * Check if we need to use the v1.1 namespace (or later).
	 * 
	 * @param info
	 * @param cd TODO
	 * @return
	 */
	private String getNamespace(Map<String, String> info, ComponentDef cd) {
		String namespace = info.get(COMPONENT_NAMESPACE);
		if (namespace != null) {
			return namespace;
		}
		String version = info.get(COMPONENT_VERSION);
		if (version != null) {
			try {
				Version v = new Version(version);
				return NAMESPACE_STEM + "/v" + v;
			} catch (Exception e) {
				error("version: specified on component header but not a valid version: "
						+ version);
				return null;
			}
		}
		for (String key : info.keySet()) {
			if (SET_COMPONENT_DIRECTIVES_1_2.contains(key)) {
				return NAMESPACE_STEM + "/v1.2.0";
			}
		}
		for (ReferenceDef rd: cd.references.values()) {
			if (rd.updated != null) {
				return NAMESPACE_STEM + "/v1.2.0";
			}
		}
		for (String key : info.keySet()) {
			if (SET_COMPONENT_DIRECTIVES_1_1.contains(key)) {
				return NAMESPACE_STEM + "/v1.1.0";
			}
		}
		return null;
	}

	/**
	 * Print the Service-Component properties element
	 * 
	 * @param cd
	 * @param info
	 */
	void properties(ComponentDef cd, Map<String, String> info, String name) {
		Collection<String> properties = split(info.get(COMPONENT_PROPERTIES));
		for (String p : properties) {
			Matcher m = PROPERTY_PATTERN.matcher(p);

			if (m.matches()) {
				String key = m.group(1).replaceAll("@", ":");
				String value = m.group(4);
				String parts[] = value.split("\\s*(\\||\\n)\\s*");
				for (String part: parts) {
					cd.property.add(key, part);
				}
			} else
				throw new IllegalArgumentException("Malformed property '" + p
						+ "' on component: " + name);
		}
	}

	/**
	 * @param cd
	 * @param provides
	 */
	void provide(ComponentDef cd, String provides, String impl) {
		if (provides != null) {
			StringTokenizer st = new StringTokenizer(provides, ",");
			List<TypeRef> provide = new ArrayList<TypeRef>();
			while (st.hasMoreTokens()) {
				String interfaceName = st.nextToken();
				TypeRef ref = analyzer.getTypeRefFromFQN(interfaceName);
				provide.add(ref);
				analyzer.referTo(ref);

				// TODO verifies the impl. class extends or implements the
				// interface
			}
			cd.service = provide.toArray(new TypeRef[provide.size()]);
		} 
	}

	public final static Pattern	REFERENCE	= Pattern.compile("([^(]+)(\\(.+\\))?");

	/**
	 * @param info
	 * @param impl TODO
	 * @param pw
	 * @throws Exception 
	 */
	void reference(Map<String, String> info, String impl, ComponentDef cd) throws Exception {
		Collection<String> dynamic = new ArrayList<String>(split(info.get(COMPONENT_DYNAMIC)));
		Collection<String> optional = new ArrayList<String>(split(info.get(COMPONENT_OPTIONAL)));
		Collection<String> multiple = new ArrayList<String>(split(info.get(COMPONENT_MULTIPLE)));
		Collection<String> greedy = new ArrayList<String>(split(info.get(COMPONENT_GREEDY)));

		Collection<String> descriptors = split(info.get(COMPONENT_DESCRIPTORS));
		if (descriptors.size() == 0) {
			TypeRef typeRef = analyzer.getTypeRefFromFQN(impl);
			Clazz c = analyzer.findClass(typeRef);
			if (c != null) {
				final Collection<String> methods = new ArrayList<String>();
				c.parseClassFileWithCollector(new ClassDataCollector() {
					public void method(MethodDef md) {
						methods.add(md.getName());
					}
				});
				descriptors = methods;
			}
		}

		for (Map.Entry<String, String> entry : info.entrySet()) {

			// Skip directives
			String referenceName = entry.getKey();
			if (referenceName.endsWith(":")) {
				if (!SET_COMPONENT_DIRECTIVES.contains(referenceName))
					error("Unrecognized directive in Service-Component header: "
							+ referenceName);
				continue;
			}

			// Parse the bind/unbind methods from the name
			// if set. They are separated by '/'
			String bind = null;
			String unbind = null;
			String updated = null;

			boolean bindCalculated = true;
			boolean unbindCalculated = true;
			boolean updatedCalculated = true;

			if (referenceName.indexOf('/') >= 0) {
				String parts[] = referenceName.split("/");
				referenceName = parts[0];
				if (parts[1].length() > 0) {
					bind = parts[1];
					bindCalculated = false;
				} else {
					bind = calculateBind(referenceName);
				}
				bind = parts[1].length() == 0? calculateBind(referenceName): parts[1];
				if (parts.length > 2 && parts[2].length() > 0) {
					unbind = parts[2] ;
					unbindCalculated = false;
				} else {
					if (bind.startsWith("add"))
						unbind = bind.replaceAll("add(.+)", "remove$1");
					else
						unbind = "un" + bind;
				}
				if (parts.length > 3) {
					updated = parts[3];
					updatedCalculated = false;
				}
			} else if (Character.isLowerCase(referenceName.charAt(0))) {
				bind = calculateBind(referenceName);
				unbind = "un" + bind;
				updated = "updated" + Character.toUpperCase(referenceName.charAt(0))
				+ referenceName.substring(1);
			}

			String interfaceName = entry.getValue();
			if (interfaceName == null || interfaceName.length() == 0) {
				error("Invalid Interface Name for references in Service Component: "
						+ referenceName + "=" + interfaceName);
				continue;
			}

			// If we have descriptors, we have analyzed the component.
			// So why not check the methods
			if (descriptors.size() > 0) {
				// Verify that the bind method exists
				if (!descriptors.contains(bind))
					if (bindCalculated)
						bind = null;
					else
						error("In component %s, the bind method %s for %s not defined", cd.name, bind, referenceName);

				// Check if the unbind method exists
				if (!descriptors.contains(unbind)) {
					if (unbindCalculated)
						// remove it
						unbind = null;
					else
						error("In component %s, the unbind method %s for %s not defined", cd.name, unbind, referenceName);
				}
				if (!descriptors.contains(updated)) {
					if (updatedCalculated)
						//remove it
						updated = null;
					else 
						error("In component %s, the updated method %s for %s is not defined", cd.name, updated, referenceName);
				}
			}
			// Check the cardinality by looking at the last
			// character of the value
			char c = interfaceName.charAt(interfaceName.length() - 1);
			if ("?+*~".indexOf(c) >= 0) {
				if (c == '?' || c == '*' || c == '~')
					optional.add(referenceName);
				if (c == '+' || c == '*')
					multiple.add(referenceName);
				if (c == '+' || c == '*' || c == '?')
					dynamic.add(referenceName);
				interfaceName = interfaceName.substring(0, interfaceName.length() - 1);
			}

			// Parse the target from the interface name
			// The target is a filter.
			String target = null;
			Matcher m = REFERENCE.matcher(interfaceName);
			if (m.matches()) {
				interfaceName = m.group(1);
				target = m.group(2);
			}
			TypeRef ref = analyzer.getTypeRefFromFQN(interfaceName);
			analyzer.referTo(ref);
			ReferenceDef rd = new ReferenceDef();
			rd.name = referenceName;
			rd.service = interfaceName;

			if (optional.contains(referenceName)) {
				if (multiple.contains(referenceName)) {
					rd.cardinality = ReferenceCardinality.MULTIPLE;
				} else {
					rd.cardinality = ReferenceCardinality.OPTIONAL;
				}
			} else {
				if (multiple.contains(referenceName)) {
					rd.cardinality = ReferenceCardinality.AT_LEAST_ONE;
				} else {
					rd.cardinality = ReferenceCardinality.MANDATORY;
				}
			}
			if (bind != null) {
				rd.bind = bind;
				if (unbind != null) {
					rd.unbind = unbind;
				}
				if (updated != null) {
					rd.updated = updated;
				}
			}

			if (dynamic.contains(referenceName)) {
				rd.policy = ReferencePolicy.DYNAMIC;
			}

			if (greedy.contains(referenceName)) {
				rd.policyOption = ReferencePolicyOption.GREEDY;
			}

			if (target != null) {
				rd.target = target;
			}
			cd.references.put(referenceName, rd);
		}
	}

	private String calculateBind(String referenceName) {
		return "set" + Character.toUpperCase(referenceName.charAt(0))
		+ referenceName.substring(1);
	}

}
