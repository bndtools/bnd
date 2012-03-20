/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools.model.clauses;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.libg.header.Attrs;

public class ServiceComponent extends HeaderClause implements Cloneable {

	// v1.0.0 attributes
    // public final static String       COMPONENT_NAME                 = "name:";
    public final static String       COMPONENT_FACTORY              = "factory:";
    public final static String       COMPONENT_SERVICEFACTORY       = "servicefactory:";
    public final static String       COMPONENT_IMMEDIATE            = "immediate:";
    public final static String       COMPONENT_ENABLED              = "enabled:";

    public final static String       COMPONENT_DYNAMIC              = "dynamic:";
    public final static String       COMPONENT_MULTIPLE             = "multiple:";
    public final static String       COMPONENT_PROVIDE              = "provide:";
    public final static String       COMPONENT_OPTIONAL             = "optional:";
    public final static String       COMPONENT_PROPERTIES           = "properties:";
    // public final static String       COMPONENT_IMPLEMENTATION       = "implementation:";

    // v1.1.0 attributes
    public final static String       COMPONENT_VERSION              = "version:";
    public final static String       COMPONENT_CONFIGURATION_POLICY = "configuration-policy:";
    public final static String       COMPONENT_MODIFIED             = "modified:";
    public final static String       COMPONENT_ACTIVATE             = "activate:";
    public final static String       COMPONENT_DEACTIVATE           = "deactivate:";

    private final static Pattern REFERENCE_PATTERN = Pattern.compile("([^(]+)(\\(.+\\))?");

	public ServiceComponent(String name, Attrs attribs) {
		super(name, attribs);
	}
	public boolean isPath() {
		return name.indexOf('/') >= 0 || name.endsWith(".xml");
	}
	private Set<String> getStringSet(String attrib) {
		List<String> list = getListAttrib(attrib);
		return list != null ? new HashSet<String>(list) : new HashSet<String>();
	}
	public void setPropertiesMap(Map<String, String> properties) {
		List<String> strings = new ArrayList<String>(properties.size());
		for (Entry<String,String> entry : properties.entrySet()) {
			String line = new StringBuilder().append(entry.getKey()).append("=").append(entry.getValue()).toString();
			strings.add(line);
		}
		setListAttrib(COMPONENT_PROPERTIES, strings);
	}
	public Map<String, String> getPropertiesMap() {
		Map<String,String> result = new LinkedHashMap<String, String>();

		List<String> list = getListAttrib(COMPONENT_PROPERTIES);
		if(list != null) {
			for (String entryStr : list) {
				String name;
				String value;

				int index = entryStr.lastIndexOf('=');
				if(index == -1) {
					name = entryStr;
					value = null;
				} else {
					name = entryStr.substring(0, index);
					value = entryStr.substring(index + 1);
				}

				result.put(name, value);
			}
		}

		return result;
	}
	public void setSvcRefs(List<? extends ComponentSvcReference> refs) {
		// First remove all existing references, i.e. non-directives
		for(Iterator<String> iter = attribs.keySet().iterator(); iter.hasNext(); ) {
			String name = iter.next();
			if(!name.endsWith(":")) {
				iter.remove();
			}
		}

		// Add in the references
		Set<String> dynamic = new HashSet<String>();
		Set<String> optional = new HashSet<String>();
		Set<String> multiple = new HashSet<String>();
		for (ComponentSvcReference ref : refs) {
			// Build the reference name with bind and unbind
			String expandedRefName = ref.getName();
			if(ref.getBind() != null) {
				expandedRefName += "/" + ref.getBind();
				if(ref.getUnbind() != null) {
					expandedRefName += "/" + ref.getUnbind();
				}
			}

			// Start building the map value
			StringBuilder buffer = new StringBuilder();
			buffer.append(ref.getServiceClass());

			// Add the target filter
			if(ref.getTargetFilter() != null) {
				buffer.append('(').append(ref.getTargetFilter()).append(')');
			}

			// Work out the cardinality suffix (i.e. *, +, ? org ~).
			// Adding to the dynamic/multiple/optional lists for non-standard cases
			String cardinalitySuffix;
			if(ref.isDynamic()) {
				if(ref.isOptional()) {
					if(ref.isMultiple()) //0..n dynamic
						cardinalitySuffix = "*";
					else // 0..1 dynamic
						cardinalitySuffix = "?";
				} else {
					if(ref.isMultiple()) // 1..n dynamic
						cardinalitySuffix = "+";
					else { // 1..1 dynamic, not a normal combination
						cardinalitySuffix = null;
						dynamic.add(ref.getName());
					}
				}
			} else {
				if(ref.isOptional()) {
					if(ref.isMultiple()) { // 0..n static, not a normal combination
						cardinalitySuffix = null;
						optional.add(ref.getName());
						multiple.add(ref.getName());
					} else { // 0..1 static
						cardinalitySuffix = "~";
					}
				} else {
					if(ref.isMultiple()) { // 1..n static, not a normal combination
						multiple.add(ref.getName());
						cardinalitySuffix = null;
					} else { // 1..1 static
						cardinalitySuffix = null;
					}
				}
			}

			if(cardinalitySuffix != null)
				buffer.append(cardinalitySuffix);

			// Write to the map
			attribs.put(expandedRefName, buffer.toString());
		}
		setListAttrib(COMPONENT_OPTIONAL, optional);
		setListAttrib(COMPONENT_MULTIPLE, multiple);
		setListAttrib(COMPONENT_DYNAMIC, dynamic);
	}
	public List<ComponentSvcReference> getSvcRefs() {
		List<ComponentSvcReference> result = new ArrayList<ComponentSvcReference>();

		Set<String> dynamicSet = getStringSet(COMPONENT_DYNAMIC);
		Set<String> optionalSet = getStringSet(COMPONENT_OPTIONAL);
		Set<String> multipleSet = getStringSet(COMPONENT_MULTIPLE);

		for (Entry<String, String> entry : attribs.entrySet()) {
			String referenceName = entry.getKey();

			// Skip directives
			if(referenceName.endsWith(":"))//$NON-NLS-1$
				continue;

			ComponentSvcReference svcRef = new ComponentSvcReference();

			String bind = null;
			String unbind = null;

			if (referenceName.indexOf('/') >= 0) {
				String parts[] = referenceName.split("/");
				referenceName = parts[0];
				bind = parts[1];
				if (parts.length > 2)
					unbind = parts[2];
			/*
				else if (bind.startsWith("add"))
					unbind = bind.replaceAll("add(.+)", "remove$1");
				else
					unbind = "un" + bind;
			} else if (Character.isLowerCase(referenceName.charAt(0))) {
				bind = "set" + Character.toUpperCase(referenceName.charAt(0))
						+ referenceName.substring(1);
				unbind = "un" + bind;
			*/
			}
			svcRef.setName(referenceName);
			svcRef.setBind(bind);
			svcRef.setUnbind(unbind);

			String interfaceName = entry.getValue();
			if (interfaceName == null || interfaceName.length() == 0) {
				continue;
			}
			svcRef.setServiceClass(interfaceName);

			// Check the cardinality by looking at the last
			// character of the value
			char c = interfaceName.charAt(interfaceName.length() - 1);
			if ("?+*~".indexOf(c) >= 0) {
				if (c == '?' || c == '*' || c == '~')
					optionalSet.add(referenceName);
				if (c == '+' || c == '*')
					multipleSet.add(referenceName);
				if (c == '+' || c == '*' || c == '?')
					dynamicSet.add(referenceName);
				interfaceName = interfaceName.substring(0, interfaceName.length() - 1);
			}
			svcRef.setOptional(optionalSet.contains(referenceName));
			svcRef.setMultiple(multipleSet.contains(referenceName));
			svcRef.setDynamic(dynamicSet.contains(referenceName));

			// Parse the target from the interface name
			// The target is a filter.
			String target = null;
			Matcher m = REFERENCE_PATTERN.matcher(interfaceName);
			if (m.matches()) {
				interfaceName = m.group(1);
				target = m.group(2);
			}
			svcRef.setTargetFilter(target);

			result.add(svcRef);
		}

		return result;
	}
	@Override
	public ServiceComponent clone() {
		return new ServiceComponent(this.name, new Attrs(this.attribs));
	}
	@Override
	protected boolean newlinesBetweenAttributes() {
		return true;
	}
}