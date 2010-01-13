package name.neilbartlett.eclipse.bndtools.editor.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import aQute.bnd.plugin.Activator;

public class ServiceComponent implements Cloneable {

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

	
	private String name;
	private final Map<String,String> attribs;

	public ServiceComponent(String name, Map<String,String> attribs) {
		assert name != null;
		assert attribs != null;
		
		this.name = name;
		this.attribs = attribs;
	}
	public void  setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public boolean isPath() {
		return name.indexOf('/') >= 0 || name.endsWith(".xml");
	}
	public Map<String, String> getAttribs() {
		return attribs;
	}
	public List<String> getListAttrib(String attrib) {
		String string = attribs.get(attrib);
		if(string == null)
			return null;
		
		List<String> result = new ArrayList<String>();	
		StringTokenizer tokenizer = new StringTokenizer(string, ",");
		while(tokenizer.hasMoreTokens()) {
			result.add(tokenizer.nextToken().trim());
		}
		
		return result;
	}
	private Set<String> getStringSet(String attrib) {
		List<String> list = getListAttrib(attrib);
		return list != null ? new HashSet<String>(list) : new HashSet<String>();
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
			
			String bind = null;
			String unbind = null;
		
			if (referenceName.indexOf('/') >= 0) {
				String parts[] = referenceName.split("/");
				referenceName = parts[0];
				bind = parts[1];
				if (parts.length > 2)
					unbind = parts[2];
				else if (bind.startsWith("add"))
					unbind = bind.replaceAll("add(.+)", "remove$1");
				else
					unbind = "un" + bind;
			} else if (Character.isLowerCase(referenceName.charAt(0))) {
				bind = "set" + Character.toUpperCase(referenceName.charAt(0))
						+ referenceName.substring(1);
				unbind = "un" + bind;
			}
			
			String interfaceName = entry.getValue();
			if (interfaceName == null || interfaceName.length() == 0) {
				logError("Invalid Interface Name for references in Service Component: " + referenceName + "=" + interfaceName, null);
				continue;
			}
			
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
			
			// Parse the target from the interface name
			// The target is a filter.
			String target = null;
			Matcher m = REFERENCE_PATTERN.matcher(interfaceName);
			if (m.matches()) {
				interfaceName = m.group(1);
				target = m.group(2);
			}

			boolean optional = optionalSet.contains(referenceName);
			boolean multiple = multipleSet.contains(referenceName);
			boolean dynamic = dynamicSet.contains(referenceName);
			ComponentSvcReference svcRef = new ComponentSvcReference(referenceName, bind, unbind, interfaceName, optional, multiple, dynamic, target);
			result.add(svcRef);
		}
		
		return result;
	}
	private void logError(String message, Throwable t) {
		Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, message, t));
	}
	public void setListAttrib(String attrib, List<? extends String> value) {
		StringBuilder buffer = new StringBuilder();
		boolean first = true;
		for (String string : value) {
			if(!first)
				buffer.append(',');
			buffer.append(string);
			first = false;
		}
		attribs.put(attrib, buffer.toString());
	}
	
	@Override
	public ServiceComponent clone() {
		return new ServiceComponent(this.name, new HashMap<String, String>(this.attribs));
	}
	
	private static final String INTERNAL_LIST_SEPARATOR = ";\\\n\t\t";
	
	public void formatTo(StringBuilder buffer) {
		buffer.append(name);
		for(Iterator<Entry<String,String>> iter = attribs.entrySet().iterator(); iter.hasNext(); ) {
			Entry<String, String> entry = iter.next();
			String name = entry.getKey();
			String value = entry.getValue();
			
			if(value != null && value.length() > 0) {
				buffer.append(INTERNAL_LIST_SEPARATOR);
			
				// Quote commas in the value
				value = value.replaceAll(",", "','");
				buffer.append(name).append('=').append(value);
			}
		}
	}
}