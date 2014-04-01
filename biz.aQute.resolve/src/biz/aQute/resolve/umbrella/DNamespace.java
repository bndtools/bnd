package biz.aQute.resolve.umbrella;

import java.util.*;

import org.osgi.resource.*;

import aQute.bnd.header.*;

public class DNamespace {
	public String	ns;
	public Attrs	attributes;
	public Attrs	directives;

	transient Map<String,Object> typedAttrs;
	transient DResource resource;

	public DNamespace() {}
	
	public DNamespace(String ns) {
		this.ns = ns;
	}
	
	public String getNamespace() {
		return ns;
	}

	public Map<String,String> getDirectives() {
		if (directives == null)
			return Collections.emptyMap();

		return directives;
	}

	public Map<String,Object> getAttributes() {
		if (attributes == null || attributes.size() == 0)
			return Collections.emptyMap();

		if ( typedAttrs != null)
			return typedAttrs;
		
		typedAttrs = new HashMap<String,Object>();
		for ( String key : attributes.keySet()) {
			typedAttrs.put( key, attributes.getTyped(key));
		}
		return typedAttrs;
	}

	public Resource getResource() {
		return resource;
	}

}
