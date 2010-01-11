package name.neilbartlett.eclipse.bndtools.editor.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

public class ServiceComponent implements Cloneable {

	// v1.0.0 attributes
    public final static String       COMPONENT_NAME                 = "name:";
    public final static String       COMPONENT_FACTORY              = "factory:";
    public final static String       COMPONENT_SERVICEFACTORY       = "servicefactory:";
    public final static String       COMPONENT_IMMEDIATE            = "immediate:";
    public final static String       COMPONENT_ENABLED              = "enabled:";
    
    public final static String       COMPONENT_DYNAMIC              = "dynamic:";
    public final static String       COMPONENT_MULTIPLE             = "multiple:";
    public final static String       COMPONENT_PROVIDE              = "provide:";
    public final static String       COMPONENT_OPTIONAL             = "optional:";
    public final static String       COMPONENT_PROPERTIES           = "properties:";
    public final static String       COMPONENT_IMPLEMENTATION       = "implementation:";

    // v1.1.0 attributes
    public final static String       COMPONENT_VERSION              = "version:";
    public final static String       COMPONENT_CONFIGURATION_POLICY = "configuration-policy:";
    public final static String       COMPONENT_MODIFIED             = "modified:";
    public final static String       COMPONENT_ACTIVATE             = "activate:";
    public final static String       COMPONENT_DEACTIVATE           = "deactivate:";
	
	private String pattern;
	private final Map<String,String> attribs;

	public ServiceComponent(String pattern, Map<String,String> attribs) {
		assert pattern != null;
		assert attribs != null;
		
		this.pattern = pattern;
		this.attribs = attribs;
	}
	public void  setPattern(String pattern) {
		this.pattern = pattern;
	}
	public String getPattern() {
		return pattern;
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
		return new ServiceComponent(this.pattern, new HashMap<String, String>(this.attribs));
	}
	
	private static final String INTERNAL_LIST_SEPARATOR = ";\\\n\t\t";
	
	public void formatTo(StringBuilder buffer) {
		buffer.append(pattern);
		for(Iterator<Entry<String,String>> iter = attribs.entrySet().iterator(); iter.hasNext(); ) {
			Entry<String, String> entry = iter.next();
			buffer.append(INTERNAL_LIST_SEPARATOR);
			
			String name = entry.getKey();
			String value = entry.getValue();
			
			// Quote commas in the value
			value = value.replaceAll(",", "','");
			buffer.append(name).append('=').append(value);
		}
	}
}