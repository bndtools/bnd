package aQute.bnd.testing;

import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import org.osgi.framework.*;

import aQute.bnd.make.component.*;
import aQute.bnd.osgi.*;
import aQute.lib.collections.*;

/**
 * Intended to wire a number components for testing using the DS (from bnd)
 * annotations.
 * <p>
 * TODO add the OSGi Annotations and support more options. needs cleanup
 */
public class DSTestWiring {
	static Pattern	REFERENCE	= Pattern.compile("([^/]+)/([^/]+)(?:/([^/]+))?");

	public static class Reference {
		String	name;
		Method	set;
		Method	unset;
		boolean	multiple;
		boolean	optional;
		boolean	dynamic;
		String	target;

		@Override
		public String toString() {
			return "Reference [" + (name != null ? "name=" + name + ", " : "") + "multiple=" + multiple + ", "
					+ (target != null ? "target=" + target : "") + "]";
		}
	}

	BundleContext	context	= null;

	{
		Bundle b = FrameworkUtil.getBundle(DSTestWiring.class);
		if (b != null)
			context = b.getBundleContext();
	}

	public void setContext(BundleContext context) {
		this.context = context;
	}

	/**
	 * A representation of a component.
	 */
	public class Component<T> {

		Class<T>			type;
		T					instance;
		Map<String,Object>	properties	= new HashMap<String,Object>();
		boolean				wiring;
		Method				activate;
		Method				deactivate;
		List<Reference>		references	= new ArrayList<DSTestWiring.Reference>();

		/*
		 * Wire
		 */
		T wire(List<Component< ? >> ordered) throws Exception {

			//
			// We can defer instantiating the type
			//
			if (instance == null)
				instance = type.newInstance();

			//
			// Are we already done?
			//

			if (ordered.contains(this))
				return instance;

			//
			// Check for cycles
			//
			if (wiring)
				throw new RuntimeException("Cycle " + type);

			wiring = true;

			//
			// Now figure out our references
			//

			ClassLoader loader = type.getClassLoader();
			if (loader != null) {
				URL url = loader.getResource(type.getName().replace('.', '/') + ".class");
				if (url != null) {

					doReferences(url);

					outer: for (Reference ref : references) {
						Method m = ref.set;
						Class< ? > requested = m.getParameterTypes()[0];
						List<Component< ? >> refComp = map.get(requested);
						if (refComp == null || refComp.isEmpty()) {
							if (!ref.optional) {
								// Do we have a context as backup? If
								// use it to get services.
								if (context != null) {
									ServiceReference< ? > refs[] = context.getServiceReferences(requested.getName(),
											ref.target);

									for (int i = 1; i < 30 && refs == null; i++) {
										Thread.sleep(100 * i + 1);
										refs = context.getServiceReferences(requested.getName(), ref.target);
									}
									if (refs != null && refs.length > 0) {
										for (ServiceReference< ? > r : refs) {
											Object o = context.getService(r);
											m.setAccessible(true);
											m.invoke(instance, o);
											if (!ref.multiple)
												continue outer;
										}
										continue outer;
									}
								}
								throw new IllegalStateException(type + " requires at least one component for "
										+ ref.name + " of type " + requested);
							}
						} else
							for (Component< ? > c : refComp) {
								m.setAccessible(true);
								m.invoke(instance, c.wire(ordered));
								if (!ref.multiple)
									break;
							}
					}
					if (activate != null) {
						activate.setAccessible(true);
						Class< ? > types[] = activate.getParameterTypes();
						Object[] parameters = new Object[types.length];
						for (int i = 0; i < types.length; i++) {
							if (Map.class.isAssignableFrom(types[i])) {
								parameters[i] = properties;
							} else if (map.containsKey(types[i]))
								parameters[i] = map.get(types[i]).get(0).instance;
							else
								throw new IllegalArgumentException("Not a pojo " + activate.getDeclaringClass()
										+ ", requires " + types[i]);
						}
						activate.invoke(instance, parameters);
					}
				}
			}
			ordered.add(this);
			return instance;
		}

		private void doReferences(URL url) throws Exception {
			//
			// Get the component definition
			//

			Analyzer a = new Analyzer();
			Clazz clazz = new Clazz(a, "", new URLResource(url));
			Map<String,String> d = ComponentAnnotationReader.getDefinition(clazz);

			for (String key : d.keySet()) {
				if ("activate:".equals(key))
					activate = findMethod(d.get(key));
				else if ("deactivate:".equals(key))
					deactivate = findMethod(d.get(key));
				else {
					//
					// Pick out the references
					//

					Matcher matcher = REFERENCE.matcher(key);
					if (matcher.matches()) {
						Reference r = new Reference();
						r.name = matcher.group(1);
						r.set = findMethod(matcher.group(2));
						r.unset = findMethod(matcher.group(3));
						// TODO handle target
						String type = d.get(key);
						if (type.endsWith("*")) {
							r.multiple = true;
							r.optional = true;
							r.dynamic = true;
						} else if (type.endsWith("?")) {
							r.multiple = false;
							r.optional = true;
							r.dynamic = true;
						} else if (type.endsWith("+")) {
							r.multiple = true;
							r.optional = false;
							r.dynamic = true;
						} else {
							r.multiple = false;
							r.optional = false;
							r.dynamic = false;
						}

						references.add(r);

					}
				}
			}
		}

		private Method findMethod(String group) {
			for (Method m : type.getDeclaredMethods())
				if (m.getName().equals(group))
					return m;
			return null;
		}

		public Component<T> $(String key, Object value) {
			properties.put(key, value);
			return this;
		}

		public Component<T> instance(T x) {
			this.instance = x;
			return this;
		}

		void index(Class< ? > c) {
			while (c != null && c != Object.class) {
				map.add(c, this);
				for (Class< ? > interf : c.getInterfaces()) {
					index(interf);
				}
				c = c.getSuperclass();
			}
		}

		@Override
		public String toString() {
			return "Component [" + (type != null ? "type=" + type + ", " : "")
					+ (activate != null ? "activate=" + activate + ", " : "")
					+ (deactivate != null ? "deactivate=" + deactivate + ", " : "")
					+ (references != null ? "references=" + references : "") + "]";
		}
	}

	final MultiMap<Class< ? >,Component< ? >>	map			= new MultiMap<Class< ? >,Component< ? >>();
	final Set<Component< ? >>					components	= new HashSet<Component< ? >>();				;
	final List<Component< ? >>					ordered		= new ArrayList<Component< ? >>();				;

	public void wire() throws Exception {
		for (Component< ? > c : components) {
			c.wire(ordered);
		}
	}

	public <T> Component<T> add(Class<T> type) throws Exception {
		Component<T> c = new Component<T>();
		c.type = type;
		c.index(type);
		components.add(c);
		return c;
	}

	/**
	 * Add the class by name. If the class cannot be found in the local class
	 * loader, and a Bundle Context is specified, try each bundle for that
	 * class.
	 * 
	 * @param cname
	 *            the name of the class
	 * @return the class
	 * @throws ClassNotFoundException
	 *             if not found
	 * @throws Exception
	 *             if something goes wrong
	 */
	public Component< ? > add(String cname) throws ClassNotFoundException, Exception {
		try {
			return add(getClass().getClassLoader().loadClass(cname));
		}
		catch (ClassNotFoundException cnfe) {
			if (context != null) {
				for (Bundle b : context.getBundles()) {
					try {
						Class< ? > c = b.loadClass(cname);
						return add(c);
					}
					catch (ClassNotFoundException e) {
						// ignore
					}
				}
			}
			throw cnfe;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> Component<T> add(T instance) throws Exception {
		return add((Class<T>) instance.getClass()).instance(instance);
	}

	public <T> T get(Class<T> c) {
		List<Component< ? >> components = map.get(c);
		if (components == null || components.size() == 0)
			return null;

		return c.cast(components.get(0).instance);
	}

}
