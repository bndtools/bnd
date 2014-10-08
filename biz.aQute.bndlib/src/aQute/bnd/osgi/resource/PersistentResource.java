package aQute.bnd.osgi.resource;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;

import org.osgi.framework.*;
import org.osgi.resource.*;

import aQute.bnd.util.dto.*;
import aQute.lib.collections.*;
import aQute.lib.converter.*;
import aQute.lib.hex.*;

/**
 * This class provides an efficient way to store a resource through JSON
 * serialization. It stores the requirements and provides in a structure of
 * Resource 1 -> * Namespace 1 -> * Req/Cap. It optimizes
 */
public class PersistentResource extends DTO implements Resource {

	public Namespace[]	namespaces;
	transient Resource	resource;
	public byte[]		sha;

	public static class Namespace extends DTO implements Comparable<Namespace> {
		public String	name;
		public RCData[]	capabilities;
		public RCData[]	requirements;

		public int compareTo(Namespace o) {
			return name.compareTo(o.name);
		}
	}

	public static class RCData extends DTO {
		public boolean	require;
		public Attr[]	properties;
		public int		directives;
	}

	public enum DataType {
		STRING, LONG, DOUBLE, VERSION;
	}

	public static class Attr extends DTO implements Comparable<Attr> {

		public String		key;
		public int			type;
		public Object		value;
		public boolean		directive	= false;

		transient Object	converted;

		public int compareTo(Attr o) {
			return key.compareTo(o.key);
		}

		public Object getValue() {
			if (converted == null && value != null) {
				DataType t = DataType.values()[type];
				if (value instanceof Collection) {
					Object[] cnv = ((Collection< ? >) value).toArray();
					for (int i = 0; i < cnv.length; i++) {
						cnv[i] = convert(t, (String) cnv[i]);
					}
					converted = cnv;
				} else
					converted = convert(t, value);
			}
			return converted;
		}

		private Object convert(DataType t, Object value) {
			try {
				switch (t) {
					case DOUBLE :
						return Converter.cnv(Double.class, value);

					case LONG :
						return Converter.cnv(Long.class, value);

					case STRING :
						return Converter.cnv(String.class, value);

					case VERSION :
						if (value instanceof String)
							return Version.parseVersion((String) value);

						return Converter.cnv(Version.class, value);

					default :
						return null;

				}
			}
			catch (Exception e) {
				return null;
			}

		}
	}

	public PersistentResource() {}

	public PersistentResource(Resource resource) {

		MultiMap<String,Capability> capMap = new MultiMap<String,Capability>();
		for (Capability cap : resource.getCapabilities(null))
			capMap.add(cap.getNamespace(), cap);

		MultiMap<String,Requirement> reqMap = new MultiMap<String,Requirement>();
		for (Requirement req : resource.getRequirements(null))
			reqMap.add(req.getNamespace(), req);

		Set<String> names = new HashSet<String>(capMap.keySet());
		names.addAll(reqMap.keySet());

		namespaces = new Namespace[names.size()];
		int i = 0;

		for (String name : names) {

			Namespace ns = new Namespace();
			ns.name = name;
			List<Requirement> requirements = reqMap.get(name);
			if (requirements != null && requirements.size() > 0) {

				ns.requirements = new RCData[requirements.size()];
				int rqi = 0;
				for (Requirement r : requirements)
					ns.requirements[rqi++] = getData(true, r.getAttributes(), r.getDirectives());
				;
			}

			List<Capability> capabilities = capMap.get(name);
			if (capabilities != null && capabilities.size() > 0) {

				ns.capabilities = new RCData[capabilities.size()];
				int rci = 0;
				for (Capability c : capabilities)
					ns.capabilities[rci++] = getData(false, c.getAttributes(), c.getDirectives());
			}
			namespaces[i++] = ns;
		}
		Arrays.sort(namespaces);
	}

	public Resource getResource() {
		if (resource == null) {
			ResourceBuilder rb = new ResourceBuilder();

			for (Namespace ns : namespaces) {
				if (ns.capabilities != null)
					for (RCData rcdata : ns.capabilities) {

						CapReqBuilder capb = new CapReqBuilder(ns.name);

						for (Attr attrs : rcdata.properties) {
							if (attrs.directive)
								capb.addDirective(attrs.key, (String) attrs.value);
							else
								capb.addAttribute(attrs.key, attrs.getValue());
						}
						rb.addCapability(capb);
					}
				if (ns.requirements != null)
					for (RCData rcdata : ns.requirements) {

						CapReqBuilder reqb = new CapReqBuilder(ns.name);

						for (Attr attrs : rcdata.properties) {
							if (attrs.directive)
								reqb.addDirective(attrs.key, (String) attrs.value);
							else
								reqb.addAttribute(attrs.key, attrs.getValue());
						}
						rb.addRequirement(reqb);
					}
			}

			resource = rb.build();
		}
		return resource;
	}

	private static int getType(Object value) {
		if (value == null || value instanceof String)
			return DataType.STRING.ordinal();

		if (value instanceof Version)
			return DataType.VERSION.ordinal();

		if (value instanceof Long)
			return DataType.LONG.ordinal();

		if (value instanceof Double)
			return DataType.DOUBLE.ordinal();

		return DataType.STRING.ordinal();
	}

	private static Attr getAttr(String key, Object value, boolean directive) {
		Attr attr = new Attr();
		attr.key = key;

		if (directive) {
			attr.type = DataType.STRING.ordinal();
			attr.value = value.toString();
			attr.directive = true;
			return attr;
		}

		attr.value = value;

		if (value instanceof Collection) {
			if (((Collection< ? >) value).size() > 0) {
				Object member = ((Collection< ? >) value).iterator().next();
				attr.type = getType(member);
			} else {
				attr.type = DataType.STRING.ordinal();
			}
			return attr;
		}

		if (value.getClass().isArray()) {
			int length = Array.getLength(value);
			if (length > 0) {
				Object member = Array.get(value, 0);
				attr.type = getType(member);

			} else {
				attr.type = DataType.STRING.ordinal();
			}
		}

		attr.type = getType(value);

		return attr;
	}

	private static RCData getData(boolean require, Map<String,Object> attributes, Map<String,String> directives) {
		RCData data = new RCData();
		data.require = require;
		List<Attr> props = new ArrayList<Attr>(attributes.size() + directives.size());

		for (Entry<String,Object> entry : attributes.entrySet()) {
			props.add(getAttr(entry.getKey(), entry.getValue(), false));
		}
		for (Entry<String,String> entry : directives.entrySet()) {
			props.add(getAttr(entry.getKey(), entry.getValue(), true));
			data.directives++;
		}
		Collections.sort(props);
		data.properties = props.toArray(new Attr[props.size()]);

		return data;
	}

	public String toString() {
		try {
			return "P-" + getResource();
		}
		catch (Exception e) {
			return "P-" + Hex.toHexString(sha);
		}
	}

	@Deprecated
	public List<Capability> getCapabilities(String ns) {
		return null;
	}

	@Deprecated
	public List<Requirement> getRequirements(String ns) {
		return null;
	}

	@Deprecated
	public static RCData getData(Map<String,Object> attributes, Map<String,String> directives) {
		return null;
	}

	@Deprecated
	public PersistentResource(byte[] digest, List<Capability> caps, List<Requirement> reqs) {}

	@Deprecated
	public class RC implements Requirement, Capability {
		public RC(RCData data, String ns) {}

		public String getNamespace() {
			return null;
		}

		public Resource getResource() {
			return null;
		}

		public Map<String,Object> getAttributes() {
			return null;
		}

		public Map<String,String> getDirectives() {
			return null;
		}

	}
}
