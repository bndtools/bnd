package aQute.bnd.osgi.resource;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;

import org.osgi.framework.*;
import org.osgi.resource.*;

import aQute.lib.collections.*;

public class PersistentResource implements Resource {

	public static class Namespace implements Comparable<Namespace> {
		public String	name;
		public RCData[]	capabilities;
		public RCData[]	requirements;

		public int compareTo(Namespace o) {
			return name.compareTo(o.name);
		}
	}

	public static class RCData {
		public Attr[]	properties;
		public int		directives;
	}

	public Namespace[]	namespaces;
	public byte[]		sha;

	public enum DataType {
		STRING, LONG, DOUBLE, VERSION;
	}

	public static class Attr implements Comparable<Attr> {

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
					converted = convert(t, (String) value);
			}
			return converted;
		}

		private Object convert(DataType t, String value) {
			switch (t) {
				case DOUBLE :
					return Double.valueOf(value);

				case LONG :
					return Long.valueOf(value);

				case STRING :
					return value;

				case VERSION :
					return Version.parseVersion(value);

				default :
					return null;

			}
		}
	}

	public PersistentResource(byte[] sha, List<Capability> caps, List<Requirement> reqs) {

		this.sha = sha;

		MultiMap<String,Capability> capMap = new MultiMap<String,Capability>();
		for (Capability cap : caps)
			capMap.add(cap.getNamespace(), cap);

		MultiMap<String,Requirement> reqMap = new MultiMap<String,Requirement>();
		for (Requirement req : reqs)
			reqMap.add(req.getNamespace(), req);

		Set<String> names = new HashSet<String>(capMap.keySet());
		names.addAll(reqMap.keySet());

		namespaces = new Namespace[names.size()];
		int i = 0;

		for (String name : names) {

			Namespace ns = new Namespace();

			List<Requirement> requirements = reqMap.get(name);
			if (requirements.size() > 0) {

				ns.requirements = new RCData[requirements.size()];
				int rqi = 0;
				for (Requirement r : requirements)
					ns.requirements[rqi] = getData(r.getAttributes(), r.getDirectives());
				;
			}

			List<Capability> capabilities = capMap.get(name);
			if (capabilities.size() > 0) {

				ns.capabilities = new RCData[capabilities.size()];
				int rci = 0;
				for (Capability c : capabilities)
					ns.requirements[rci] = getData(c.getAttributes(), c.getDirectives());
			}
			namespaces[i] = ns;
		}
		Arrays.sort(namespaces);
	}

	transient MultiMap<String,Capability>	tcapabilities;
	transient MultiMap<String,Requirement>	trequirements;

	public List<Capability> getCapabilities(final String namespace) {
		init();
		if (namespace == null)
			return tcapabilities.allValues();

		List<Capability> list = tcapabilities.get(namespace);
		if (list != null)
			return list;

		return Collections.emptyList();
	}

	public List<Requirement> getRequirements(String namespace) {
		init();
		if (namespace == null)
			return trequirements.allValues();

		List<Requirement> list = trequirements.get(namespace);
		if (list != null)
			return list;

		return Collections.emptyList();
	}

	private void init() {
		tcapabilities = new MultiMap<String,Capability>();
		trequirements = new MultiMap<String,Requirement>();

		for (int i = 0; i < namespaces.length; i++) {
			final Namespace ns = namespaces[i];
			for (final RCData rs : ns.capabilities) {
				tcapabilities.add(ns.name, new RC(rs, ns.name, this));
			}
			for (final RCData rs : ns.requirements) {
				trequirements.add(ns.name, new RC(rs, ns.name, this));
			}
		}

	}

	public class RC implements Requirement, Capability {

		final Attr					props[];
		final int					directivesCount;
		final String				namespace;
		final PersistentResource	resource;

		Map<String,Object>			attributes;
		Map<String,String>			directives;

		public RC(RCData data, String ns, PersistentResource resource) {
			this.props = data.properties;
			this.directivesCount = data.directives;
			this.namespace = ns;
			this.resource = resource;
		}

		public String getNamespace() {
			return namespace;
		}

		public Resource getResource() {
			return resource;
		}

		class PropMap<V> implements Map<String,V> {
			boolean	directive;

			public PropMap(boolean directive) {
				this.directive = directive;
			}

			public int size() {
				return directive ? directivesCount : props.length - directivesCount;
			}

			public boolean isEmpty() {
				return props.length == 0;
			}

			public boolean containsKey(Object key) {
				return get(key) != null;
			}

			public boolean containsValue(Object value) {
				if (value == null)
					return false;

				for (Attr attr : props) {
					if (value.equals(attr.getValue()))
						return true;
				}
				return false;
			}

			@SuppressWarnings("unchecked")
			public V get(Object key) {
				if (key instanceof String) {
					Attr attr = search((String) key);
					if (attr != null)
						return (V) attr.getValue();
				}
				return null;
			}

			public V put(String key, V value) {
				throw new UnsupportedOperationException();
			}

			public V remove(Object key) {
				throw new UnsupportedOperationException();
			}

			public void putAll(Map< ? extends String, ? extends V> m) {
				throw new UnsupportedOperationException();
			}

			public void clear() {
				throw new UnsupportedOperationException();
			}

			public Set<String> keySet() {
				Set<String> result = new HashSet<String>();
				for (Attr attr : props) {
					if (attr.directive == directive)
						result.add(attr.key);
				}
				return result;
			}

			@SuppressWarnings("unchecked")
			public Collection<V> values() {
				List<V> values = new ArrayList<V>();
				for (Attr attr : props) {
					if (attr.directive == directive)
						values.add((V) attr.getValue());
				}
				return values;
			}

			public Set<java.util.Map.Entry<String,V>> entrySet() {
				Set<java.util.Map.Entry<String,V>> result = new HashSet<java.util.Map.Entry<String,V>>();
				for (final Attr attr : props) {
					if (attr.directive == directive)
						result.add(new Map.Entry<String,V>() {

							public String getKey() {
								return attr.key;
							}

							@SuppressWarnings("unchecked")
							public V getValue() {
								return (V) attr.getValue();
							}

							public V setValue(V value) {
								throw new UnsupportedOperationException();
							}
						});
				}
				return result;
			}

			private Attr search(String key) {
				int low = 0;
				int high = props.length - 1;

				while (low <= high) {
					int mid = (low + high) >>> 1;
					Attr midVal = props[mid];

					int r = midVal.key.compareTo(key);
					if (r < 0)
						low = mid + 1;
					else if (r > 0)
						high = mid - 1;
					else {
						Attr attr = props[mid];
						if (attr.directive == this.directive)
							return attr;

						mid += 1;

						if (mid <= high && props[mid].key.equals(key))
							return props[mid];

						mid -= 2;

						if (mid >= 0 && props[mid].key.equals(key))
							return props[mid];

						break;
					}
				}
				return null;
			}
		}

		public Map<String,Object> getAttributes() {
			if (attributes != null)
				return attributes;
			return attributes = new PropMap<Object>(false);
		}

		public Map<String,String> getDirectives() {
			if (directives != null)
				return directives;
			return directives = new PropMap<String>(true);
		}

	}

	static int getType(Object value) {
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

		attr.value = value + "";

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

	public static RCData getData(Map<String,Object> attributes, Map<String,String> directives) {
		RCData data = new RCData();
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

}
