package aQute.bnd.differ;

import static aQute.bnd.service.diff.Delta.*;
import static aQute.bnd.service.diff.Type.*;
import static java.lang.reflect.Modifier.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import aQute.bnd.annotation.*;
import aQute.bnd.differ.Element.*;
import aQute.bnd.service.diff.*;
import aQute.bnd.service.diff.DiffPlugin.Info;
import aQute.bnd.service.diff.Type;
import aQute.lib.collections.*;
import aQute.lib.osgi.*;
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
 * 
 */

class TypedElement extends Structured {
	private static final Structured	OBJECT	= new TypedElement(CLASS, "java.lang.Object", PUBLIC,
													Arrays.asList(new TypedElement(METHOD,
															"int hashCode()", PUBLIC, null, MINOR,
															MAJOR, null), //
															new TypedElement(METHOD,
																	"void finalize()", PROTECTED,
																	null, MINOR, MAJOR, null), //
															new TypedElement(METHOD,
																	"void equals()", PUBLIC, null,
																	MINOR, MAJOR, null), //
															new TypedElement(METHOD,
																	"java.lang.Object clone()",
																	PROTECTED, null, MINOR, MAJOR,
																	null), //
															new TypedElement(METHOD,
																	"int hashCode()", PUBLIC, null,
																	MINOR, MAJOR, null), //
															new TypedElement(METHOD,
																	"java.lang.String toString()",
																	PUBLIC, null, MINOR, MAJOR,
																	null) //

													), //
													MAJOR, MAJOR, "built in object");

	final int						access;

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
	static Structured classElement(final Analyzer analyzer, final Clazz clazz,
			final Set<String> notAccessible, final Map<Clazz, Structured> cache, final Info[] infos)
			throws Exception {
		final StringBuilder comment = new StringBuilder();
		final Set<Element> members = new HashSet<Element>();
		final List<Clazz.MethodDef> methods = Create.list();
		final List<Clazz.FieldDef> fields = Create.list();
		final MultiMap<Clazz.Def, Element> annotations = new MultiMap<Clazz.Def, Element>();
		final AtomicBoolean provider = new AtomicBoolean(false);

		for (Element el : OBJECT.children)
			members.add(el);

		//
		// Check if we already had this clazz in the cache
		//

		Structured before = cache.get(clazz); // for super classes
		if (before != null)
			return before;

		clazz.parseClassFileWithCollector(new ClassDataCollector() {
			boolean			memberEnd;
			Clazz.FieldDef	last;

			@Override public void version(int minor, int major) {
				// members.add(new Leaf(Type.CLASS_VERSION,
				// "<classversion>", major + "." + minor,
				// true));
			}

			@Override public void method(Clazz.MethodDef defined) {
				if (Modifier.isProtected(defined.access) || Modifier.isPublic(defined.access)) {
					last = defined;
					methods.add(defined);
				} else {
					last = null;
				}
			}

			@Override public void field(Clazz.FieldDef defined) {
				if (Modifier.isProtected(defined.access) || Modifier.isPublic(defined.access)) {
					last = defined;
					fields.add(defined);
				} else
					last = null;
			}

			@Override public void constant(Object o) {
				if (last != null) {
					// Must be accessible now
					last.constant = o;
				}
			}

			@Override public void extendsClass(String name) {
				String comment = null;
				if (!clazz.isInterface())
					try {
						if (!name.equals("java/lang/Object")) {
							Clazz c = analyzer.findClass(name + ".class");
							if (c != null) {
								Structured s = classElement(analyzer, c, notAccessible, cache,
										infos);
								addAll(members, s.children);
							} else {
								comment = "Not found";
							}
						}
					} catch (Exception e) {
						comment = e.toString();
					}

				String fqn = Clazz.binaryToFQN(name);
				members.add(new Element(Type.EXTENDS, fqn, null, MICRO, MAJOR, comment));
			}

			@Override public void implementsInterfaces(String names[]) {
				// TODO is interface reordering important for binary
				// compatibility??

				for (String name : names) {
					String comment = null;
					if (clazz.isInterface())
						try {
							if (!name.equals("java/lang/Object")) {
								Clazz c = analyzer.findClass(name + ".class");
								if (c != null) {
									Structured s = classElement(analyzer, c, notAccessible, cache,
											infos);
									addAll(members, s.children);
								} else {
									comment = "Not found";
								}
							}
						} catch (Exception e) {
							comment = e.toString();
						}
					String fqn = Clazz.binaryToFQN(name);
					members.add(new Element(Type.IMPLEMENTS, fqn, null, MINOR, MAJOR, comment));
				}
			}

			private void addAll(Set<Element> members, Element[] children) {
				for (Element e : children)
					members.add(e);
			}

			@Override public void annotation(Annotation annotation) {
				Collection<Element> properties = Create.set();

				for (String key : annotation.keySet()) {
					StringBuilder sb = new StringBuilder();
					toString(sb, annotation.get(key));

					properties.add(new Element(Type.PROPERTY, key, sb.toString(), CHANGED, CHANGED,
							null));
				}

				if (memberEnd) {
					members.add(new Structured(Type.ANNOTATION, annotation.getName(), null,
							properties, CHANGED, CHANGED, null));
					String name = Clazz.binaryToFQN(annotation.getName());
					if (ProviderType.class.getName().equals(name)) {
						provider.set(true);
					}
				} else if (last != null)
					annotations.add(last, new Structured(Type.ANNOTATION, annotation.getName(),
							null, properties, CHANGED, CHANGED, null));
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

			@Override public void innerClass(String innerClass, String outerClass,
					String innerName, int innerClassAccessFlags) {
				if (Modifier.isProtected(innerClassAccessFlags)
						|| Modifier.isPublic(innerClassAccessFlags))
					return;

				notAccessible.add(Clazz.binaryToFQN(innerClass));
			}

			@Override public void memberEnd() {
				memberEnd = true;
			}
		});

		// If Interface, on method diff
		// ADD REMOVE
		// provider MINOR MAJOR
		// consumer MAJOR MAJOR (TODO could be MINOR)

		String fqn = clazz.getFQN();
		Delta add = MAJOR;
		Delta remove = MAJOR;
		if (clazz.isInterface()) {
			boolean p = provider.get();
			for (Info info : infos) {
				if (p) {
					add = MINOR;
					break;
				}
				p |= info.isProvider(fqn);
			}
		}

		for (Clazz.MethodDef m : methods) {
			Element member = new TypedElement(Type.METHOD, Clazz.descriptorToJava(m.name,
					m.descriptor), m.access, annotations.get(m), add, remove, null);
			members.add(member);
		}

		for (Clazz.FieldDef f : fields) {
			Set<Element> children = annotations.get(f);

			// Fields can have a constant value, this is a new element
			if (f.constant != null) {
				if (children == null)
					children = Create.set();
				children.add(new Element(Type.CONSTANT, "<constant>", f.constant.toString(),
						CHANGED, CHANGED, null));
			}

			Element member = new TypedElement(Type.FIELD, Clazz.descriptorToJava(f.name,
					f.descriptor), f.access, children, MINOR, MAJOR, null);

			members.add(member);
		}

		// And make the result
		Structured s = new TypedElement(clazz.isInterface() ? Type.INTERFACE : Type.CLASS, fqn,
				clazz.getAccess(), members, MINOR, MAJOR, comment.length() == 0 ? null
						: comment.toString());
		cache.put(clazz, s);
		return s;
	}

	/**
	 * Constructor for a type element.
	 * 
	 * @param type
	 * @param name
	 * @param access
	 * @param children
	 * @param add
	 * @param remove
	 * @param comment
	 */
	TypedElement(Type type, String name, int access, Collection<? extends Element> children,
			Delta add, Delta remove, String comment) {
		super(type, name, Integer.toHexString(access), children, add, remove, comment);
		this.access = access & (PUBLIC | PROTECTED | STATIC | ABSTRACT | FINAL);
	}

	@Override Delta getValueDelta(Element o) {
		int other = ((TypedElement) o).access;
		if (access == other)
			return Delta.UNCHANGED;

		if (isInterface(access) != isInterface(other))
			return Delta.MAJOR;

		if (isProtected(access) && isPublic(other))
			return Delta.MAJOR;
		if (isPublic(access) && isProtected(other))
			return Delta.MINOR;

		if (isAbstract(access) && !isAbstract(other))
			return Delta.MAJOR;

		if (!isAbstract(access) && isAbstract(other))
			return Delta.MINOR;

		if (isFinal(access) && !isFinal(other))
			return Delta.MAJOR;

		if (!isFinal(access) && isFinal(other))
			return Delta.MINOR;

		if (isStatic(access) != isStatic(other))
			return Delta.MINOR;
		else
			return Delta.UNCHANGED;
	}

	String getValue() {
		return Modifier.toString(access);
	}

}