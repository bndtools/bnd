package aQute.bnd.differ;

import static aQute.bnd.service.diff.Delta.*;
import static aQute.bnd.service.diff.Type.*;
import static java.lang.reflect.Modifier.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.*;
import java.util.jar.*;

import aQute.bnd.annotation.*;
import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Clazz.JAVA;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Version;
import aQute.bnd.service.diff.*;
import aQute.bnd.service.diff.Type;
import aQute.lib.collections.*;
import aQute.libg.generics.*;

/**
 * An element that compares the access field in a binary compatible way. This
 * element is used for classes, methods, constructors, and fields. For that
 * reason we also included the only method that uses this class as a static
 * method.
 * <p>
 * Packages
 * <ul>
 * <li>MAJOR - Remove a public type
 * <li>MINOR - Add a public class
 * <li>MINOR - Add an interface
 * <li>MINOR - Add a method to a class
 * <li>MINOR - Add a method to a provider interface
 * <li>MAJOR - Add a method to a consumer interface
 * <li>MINOR - Add a field
 * <li>MICRO - Add an annotation to a member
 * <li>MINOR - Change the value of a constant
 * <li>MICRO - -abstract
 * <li>MICRO - -final
 * <li>MICRO - -protected
 * <li>MAJOR - +abstract
 * <li>MAJOR - +final
 * <li>MAJOR - +protected
 * </ul>
 */

class JavaElement {
	final static EnumSet<Type>			INHERITED		= EnumSet.of(FIELD, METHOD, EXTENDS, IMPLEMENTS);
	private static final Element		PROTECTED		= new Element(ACCESS, "protected", null, MAJOR, MINOR, null);
	private static final Element		STATIC			= new Element(ACCESS, "static", null, MAJOR, MAJOR, null);
	private static final Element		ABSTRACT		= new Element(ACCESS, "abstract", null, MAJOR, MINOR, null);
	private static final Element		FINAL			= new Element(ACCESS, "final", null, MAJOR, MINOR, null);
	// private static final Element DEPRECATED = new Element(ACCESS,
	// "deprecated", null,
	// CHANGED, CHANGED, null);

	final Analyzer						analyzer;
	final Map<PackageRef,Instructions>	providerMatcher	= Create.map();
	final Set<TypeRef>					notAccessible	= Create.set();
	final Map<Object,Element>			cache			= Create.map();
	MultiMap<PackageRef, //
	Element>							packages;
	final MultiMap<TypeRef, //
	Element>							covariant		= new MultiMap<TypeRef,Element>();
	final Set<JAVA>						javas			= Create.set();
	final Packages						exports;

	/**
	 * Create an element for the API. We take the exported packages and traverse
	 * those for their classes. If there is no manifest or it does not describe
	 * a bundle we assume the whole contents is exported.
	 * 
	 * @param infos
	 */
	JavaElement(Analyzer analyzer) throws Exception {
		this.analyzer = analyzer;

		Manifest manifest = analyzer.getJar().getManifest();
		if (manifest != null && manifest.getMainAttributes().getValue(Constants.BUNDLE_MANIFESTVERSION) != null) {
			exports = new Packages();
			for (Map.Entry<String,Attrs> entry : OSGiHeader.parseHeader(
					manifest.getMainAttributes().getValue(Constants.EXPORT_PACKAGE)).entrySet())
				exports.put(analyzer.getPackageRef(entry.getKey()), entry.getValue());
		} else
			exports = analyzer.getContained();
		//
		// We have to gather the -providers and parse them into instructions
		// so we can efficiently match them during class parsing to find
		// out who the providers and consumers are
		//

		for (Entry<PackageRef,Attrs> entry : exports.entrySet()) {
			String value = entry.getValue().get(Constants.PROVIDER_TYPE_DIRECTIVE);
			if (value != null) {
				providerMatcher.put(entry.getKey(), new Instructions(value));
			}
		}

		// we now need to gather all the packages but without
		// creating the packages yet because we do not yet know
		// which classes are accessible

		packages = new MultiMap<PackageRef,Element>();

		for (Clazz c : analyzer.getClassspace().values()) {
			if (c.isPublic() || c.isProtected()) {
				PackageRef packageName = c.getClassName().getPackageRef();

				if (exports.containsKey(packageName)) {
					Element cdef = classElement(c);
					packages.add(packageName, cdef);
				}
			}
		}

	}

	static Element getAPI(Analyzer analyzer) throws Exception {
		analyzer.analyze();
		JavaElement te = new JavaElement(analyzer);
		return te.getLocalAPI();
	}

	private Element getLocalAPI() throws Exception {
		List<Element> result = new ArrayList<Element>();

		for (Map.Entry<PackageRef,List<Element>> entry : packages.entrySet()) {
			List<Element> set = entry.getValue();
			for (Iterator<Element> i = set.iterator(); i.hasNext();) {

				if (notAccessible.contains(analyzer.getTypeRefFromFQN(i.next().getName())))
					i.remove();

			}
			String version = exports.get(entry.getKey()).get(Constants.VERSION_ATTRIBUTE);
			if (version != null) {
				Version v = new Version(version);
				set.add(new Element(Type.VERSION, v.getWithoutQualifier().toString(), null, IGNORED, IGNORED, null));
			}
			Element pd = new Element(Type.PACKAGE, entry.getKey().getFQN(), set, MINOR, MAJOR, null);
			result.add(pd);
		}

		for (JAVA java : javas) {
			result.add(new Element(CLASS_VERSION, java.toString(), null, Delta.CHANGED, Delta.CHANGED, null));
		}

		return new Element(Type.API, "<api>", result, CHANGED, CHANGED, null);
	}

	/**
	 * Calculate the class element. This requires parsing the class file and
	 * finding all the methods that were added etc. The parsing will take super
	 * interfaces and super classes into account. For this reason it maintains a
	 * queue of classes/interfaces to parse.
	 * 
	 * @param analyzer
	 * @param clazz
	 * @param infos
	 * @return
	 * @throws Exception
	 */
	Element classElement(final Clazz clazz) throws Exception {
		Element e = cache.get(clazz);
		if (e != null)
			return e;

		final StringBuilder comment = new StringBuilder();
		final Set<Element> members = new HashSet<Element>();
		final Set<MethodDef> methods = Create.set();
		final Set<Clazz.FieldDef> fields = Create.set();
		final MultiMap<Clazz.Def,Element> annotations = new MultiMap<Clazz.Def,Element>();

		final TypeRef name = clazz.getClassName();

		final String fqn = name.getFQN();
		final String shortName = name.getShortName();

		// Check if this clazz is actually a provider or not
		// providers must be listed in the exported package in the
		// PROVIDER_TYPE directive.
		Instructions matchers = providerMatcher.get(name.getPackageRef());
		boolean p = matchers != null && matchers.matches(shortName);
		final AtomicBoolean provider = new AtomicBoolean(p);

		//
		// Check if we already had this clazz in the cache
		//

		Element before = cache.get(clazz); // for super classes
		if (before != null)
			return before;

		clazz.parseClassFileWithCollector(new ClassDataCollector() {
			boolean			memberEnd;
			Clazz.FieldDef	last;

			@Override
			public void version(int minor, int major) {
				javas.add(Clazz.JAVA.getJava(major, minor));
			}

			@Override
			public void method(MethodDef defined) {
				if ((defined.isProtected() || defined.isPublic())) {
					last = defined;
					methods.add(defined);
				} else {
					last = null;
				}
			}

			@Override
			public void deprecated() {
				if (memberEnd)
					clazz.setDeprecated(true);
				else
					last.setDeprecated(true);
			}

			@Override
			public void field(Clazz.FieldDef defined) {
				if (defined.isProtected() || defined.isPublic()) {
					last = defined;
					fields.add(defined);
				} else
					last = null;
			}

			@Override
			public void constant(Object o) {
				if (last != null) {
					// Must be accessible now
					last.setConstant(o);
				}
			}

			@Override
			public void extendsClass(TypeRef name) throws Exception {
				String comment = null;
				if (!clazz.isInterface())
					comment = inherit(members, name);

				Clazz c = analyzer.findClass(name);
				if ((c == null || c.isPublic()) && !name.isObject())
					members.add(new Element(Type.EXTENDS, name.getFQN(), null, MICRO, MAJOR, comment));
			}

			@Override
			public void implementsInterfaces(TypeRef names[]) throws Exception {
				// TODO is interface reordering important for binary
				// compatibility??

				for (TypeRef name : names) {

					String comment = null;
					if (clazz.isInterface() || clazz.isAbstract())
						comment = inherit(members, name);
					members.add(new Element(Type.IMPLEMENTS, name.getFQN(), null, MINOR, MAJOR, comment));
				}
			}

			/**
			 * @param members
			 * @param name
			 * @param comment
			 * @return
			 */
			Set<Element>	OBJECT	= Create.set();

			public String inherit(final Set<Element> members, TypeRef name) throws Exception {
				if (name.isObject()) {
					if (OBJECT.isEmpty()) {
						Clazz c = analyzer.findClass(name);
						Element s = classElement(c);
						for (Element child : s.children) {
							if (INHERITED.contains(child.type)) {
								String n = child.getName();
								if (child.type == METHOD) {
									if (n.startsWith("<init>") || "getClass()".equals(child.getName())
											|| n.startsWith("wait(") || n.startsWith("notify(")
											|| n.startsWith("notifyAll("))
										continue;
								}
								OBJECT.add(child);
							}
						}
					}
					members.addAll(OBJECT);
				} else {

					Clazz c = analyzer.findClass(name);
					if (c == null) {
						return "Cannot load " + name;
					}
					Element s = classElement(c);
					for (Element child : s.children) {
						if (INHERITED.contains(child.type) && !child.name.startsWith("<")) {
							members.add(child);
						}
					}
				}
				return null;
			}

			@Override
			public void annotation(Annotation annotation) {
				Collection<Element> properties = Create.set();
				if (Deprecated.class.getName().equals(annotation.getName().getFQN())) {
					if (memberEnd)
						clazz.setDeprecated(true);
					else
						last.setDeprecated(true);
					return;
				}

				for (String key : annotation.keySet()) {
					StringBuilder sb = new StringBuilder();
					sb.append(key);
					sb.append('=');
					toString(sb, annotation.get(key));

					properties.add(new Element(Type.PROPERTY, sb.toString(), null, CHANGED, CHANGED, null));
				}

				if (memberEnd) {
					members.add(new Element(Type.ANNOTATED, annotation.getName().getFQN(), properties, CHANGED,
							CHANGED, null));
					if (ProviderType.class.getName().equals(annotation.getName().getFQN())) {
						provider.set(true);
					} else if (ConsumerType.class.getName().equals(annotation.getName().getFQN())) {
						provider.set(false);
					}
				} else if (last != null)
					annotations.add(last, new Element(Type.ANNOTATED, annotation.getName().getFQN(), properties,
							CHANGED, CHANGED, null));
			}

			private void toString(StringBuilder sb, Object object) {

				if (object.getClass().isArray()) {
					sb.append('[');
					int l = Array.getLength(object);
					for (int i = 0; i < l; i++)
						toString(sb, Array.get(object, i));
					sb.append(']');
				} else
					sb.append(object);
			}

			@Override
			public void innerClass(TypeRef innerClass, TypeRef outerClass, String innerName, int innerClassAccessFlags)
					throws Exception {
				Clazz clazz = analyzer.findClass(innerClass);
				if (clazz != null)
					clazz.setInnerAccess(innerClassAccessFlags);

				if (Modifier.isProtected(innerClassAccessFlags) || Modifier.isPublic(innerClassAccessFlags))
					return;
				notAccessible.add(innerClass);
			}

			@Override
			public void memberEnd() {
				memberEnd = true;
			}
		});

		// This is the heart of the semantic versioning. If we
		// add or remove a method from an interface then
		Delta add;
		Delta remove;
		Type type;

		// Calculate the type of the clazz. A class
		// can be an interface, class, enum, or annotation

		if (clazz.isInterface())
			if (clazz.isAnnotation())
				type = Type.INTERFACE;
			else
				type = Type.ANNOTATION;
		else if (clazz.isEnum())
			type = Type.ENUM;
		else
			type = Type.CLASS;

		if (type == Type.INTERFACE) {
			if (provider.get()) {
				// Adding a method for a provider is not an issue
				// because it must be aware of the changes
				add = MINOR;

				// Removing a method influences consumers since they
				// tend to call this guy.
				remove = MAJOR;
			} else {
				// Adding a method is a major change
				// because the consumer has to implement it
				// or the provider will call a non existent
				// method on the consumer
				add = MAJOR;

				// Removing a method is not an issue because the
				// provider, which calls this contract must be
				// aware of the removal

				remove = MINOR;
			}
		} else {
			// Adding a method to a class can never do any harm
			add = MINOR;

			// Removing it will likely hurt consumers
			remove = MAJOR;
		}

		// Remove all synthetic methods, we need
		// to treat them special for the covariant returns

		Set<MethodDef> synthetic = Create.set();
		for (Iterator<MethodDef> i = methods.iterator(); i.hasNext();) {
			MethodDef m = i.next();
			if (m.isSynthetic()) {
				synthetic.add(m);
				i.remove();
			}
		}

		for (MethodDef m : methods) {
			List<Element> children = annotations.get(m);
			if (children == null)
				children = new ArrayList<Element>();

			access(children, m.getAccess(), m.isDeprecated());

			// A final class cannot be extended, ergo,
			// all methods defined in it are by definition
			// final. However, marking them final (either
			// on the method or inheriting it from the class)
			// will create superfluous changes if we
			// override a method from a super class that was not
			// final. So we actually remove the final for methods
			// in a final class.
			if (clazz.isFinal())
				children.remove(FINAL);

			// for covariant types we need to add the return types
			// and all the implemented and extended types. This is already
			// do for us when we get the element of the return type.

			getCovariantReturns(children, m.getType());

			for (Iterator<MethodDef> i = synthetic.iterator(); i.hasNext();) {
				MethodDef s = i.next();
				if (s.getName().equals(m.getName()) && Arrays.equals(s.getPrototype(), m.getPrototype())) {
					i.remove();
					getCovariantReturns(children, s.getType());
				}
			}

			Element member = new Element(Type.METHOD, m.getName() + toString(m.getPrototype()), children, add, remove,
					null);

			if (!members.add(member)) {
				members.remove(member);
				members.add(member);
			}
		}

		/**
		 * Repeat for the remaining synthetic methods
		 */
		for (MethodDef m : synthetic) {
			List<Element> children = annotations.get(m);
			if (children == null)
				children = new ArrayList<Element>();
			access(children, m.getAccess(), m.isDeprecated());

			// A final class cannot be extended, ergo,
			// all methods defined in it are by definition
			// final. However, marking them final (either
			// on the method or inheriting it from the class)
			// will create superfluous changes if we
			// override a method from a super class that was not
			// final. So we actually remove the final for methods
			// in a final class.
			if (clazz.isFinal())
				children.remove(FINAL);

			// for covariant types we need to add the return types
			// and all the implemented and extended types. This is already
			// do for us when we get the element of the return type.

			getCovariantReturns(children, m.getType());

			Element member = new Element(Type.METHOD, m.getName() + toString(m.getPrototype()), children, add, remove,
					"synthetic");

			if (!members.add(member)) {
				members.remove(member);
				members.add(member);
			}
		}

		for (Clazz.FieldDef f : fields) {
			List<Element> children = annotations.get(f);
			if (children == null)
				children = new ArrayList<Element>();

			// Fields can have a constant value, this is a new element
			if (f.getConstant() != null) {
				children.add(new Element(Type.CONSTANT, f.getConstant().toString(), null, CHANGED, CHANGED, null));
			}

			access(children, f.getAccess(), f.isDeprecated());
			Element member = new Element(Type.FIELD, f.getType().getFQN() + " " + f.getName(), children, MINOR, MAJOR,
					null);

			if (!members.add(member)) {
				members.remove(member);
				members.add(member);
			}
		}

		access(members, clazz.getAccess(), clazz.isDeprecated());

		// And make the result
		Element s = new Element(type, fqn, members, MINOR, MAJOR, comment.length() == 0 ? null : comment.toString());
		cache.put(clazz, s);
		return s;
	}

	private String toString(TypeRef[] prototype) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		String del = "";
		for (TypeRef ref : prototype) {
			sb.append(del);
			sb.append(ref.getFQN());
			del = ",";
		}
		sb.append(")");
		return sb.toString();
	}

	static Element	BOOLEAN_R	= new Element(RETURN, "boolean");
	static Element	BYTE_R		= new Element(RETURN, "byte");
	static Element	SHORT_R		= new Element(RETURN, "short");
	static Element	CHAR_R		= new Element(RETURN, "char");
	static Element	INT_R		= new Element(RETURN, "int");
	static Element	LONG_R		= new Element(RETURN, "long");
	static Element	FLOAT_R		= new Element(RETURN, "float");
	static Element	DOUBLE_R	= new Element(RETURN, "double");

	private void getCovariantReturns(Collection<Element> elements, TypeRef type) throws Exception {
		if (type == null || type.isObject())
			return;

		if (type.isPrimitive()) {
			if (type.getFQN().equals("void"))
				return;

			String name = type.getBinary();
			Element e;
			switch (name.charAt(0)) {
				case 'Z' :
					e = BOOLEAN_R;
					break;
				case 'S' :
					e = SHORT_R;
					break;
				case 'I' :
					e = INT_R;
					break;
				case 'B' :
					e = BYTE_R;
					break;
				case 'C' :
					e = CHAR_R;
					break;
				case 'J' :
					e = LONG_R;
					break;
				case 'F' :
					e = FLOAT_R;
					break;
				case 'D' :
					e = DOUBLE_R;
					break;

				default :
					throw new IllegalArgumentException("Unknown primitive " + type);
			}
			elements.add(e);
			return;
		}

		List<Element> set = covariant.get(type);
		if (set != null) {
			elements.addAll(set);
			return;
		}

		Element current = new Element(RETURN, type.getFQN());
		Clazz clazz = analyzer.findClass(type);
		if (clazz == null) {
			elements.add(current);
			return;
		}

		set = Create.list();
		set.add(current);
		getCovariantReturns(set, clazz.getSuper());

		TypeRef[] interfaces = clazz.getInterfaces();
		if (interfaces != null)
			for (TypeRef intf : interfaces) {
				getCovariantReturns(set, intf);
			}

		covariant.put(type, set);
		elements.addAll(set);
	}

	private static void access(Collection<Element> children, int access, @SuppressWarnings("unused") boolean deprecated) {
		if (!isPublic(access))
			children.add(PROTECTED);
		if (isAbstract(access))
			children.add(ABSTRACT);
		if (isFinal(access))
			children.add(FINAL);
		if (isStatic(access))
			children.add(STATIC);

		// Ignore for now
		// if (deprecated)
		// children.add(DEPRECATED);

	}

}