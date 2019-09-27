package aQute.bnd.metatype;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.annotation.xml.XMLAttribute;
import aQute.bnd.metatype.MetatypeAnnotations.Options;
import aQute.bnd.metatype.annotations.AttributeDefinition;
import aQute.bnd.metatype.annotations.AttributeType;
import aQute.bnd.metatype.annotations.Icon;
import aQute.bnd.metatype.annotations.ObjectClassDefinition;
import aQute.bnd.metatype.annotations.Option;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Annotation;
import aQute.bnd.osgi.ClassDataCollector;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.FieldDef;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.xmlattribute.XMLAttributeFinder;

class OCDReader {
	final Analyzer				analyzer;
	private final Clazz			clazz;
	final Set<Options>			options;
	private final Set<TypeRef>	analyzed	= new HashSet<>();
	private final OCDDef		ocd;
	final XMLAttributeFinder	finder;

	private OCDReader(Analyzer analyzer, Clazz clazz, Set<Options> options, XMLAttributeFinder finder,
		MetatypeVersion minVersion) {
		this.analyzer = analyzer;
		this.clazz = clazz;
		this.options = options;
		this.finder = finder;
		this.ocd = new OCDDef(finder, minVersion);
	}

	static OCDDef getOCDDef(Clazz c, Analyzer analyzer, Set<Options> options, XMLAttributeFinder finder,
		MetatypeVersion minVersion) throws Exception {
		OCDReader r = new OCDReader(analyzer, c, options, finder, minVersion);
		return r.getDef();
	}

	private OCDDef getDef() throws Exception {
		clazz.parseClassFileWithCollector(new OCDDataCollector(ocd));
		if (ocd.id == null) {
			return null;
		}
		parseExtends(clazz);
		return ocd;
	}

	private void parseExtends(Clazz clazz) {
		TypeRef[] inherits = clazz.getInterfaces();
		if (inherits != null) {
			for (TypeRef typeRef : inherits) {
				if (!typeRef.isJava() && analyzed.add(typeRef)) {
					try {
						Clazz inherit = analyzer.findClass(typeRef);
						if (inherit != null) {
							inherit.parseClassFileWithCollector(new OCDDataCollector(ocd));
							parseExtends(inherit);
						} else {
							analyzer.error("Could not obtain super class %s of class %s", typeRef.getFQN(),
								clazz.getClassName()
									.getFQN());
						}
					} catch (Exception e) {
						analyzer.exception(e, "Could not obtain super class %s of class %s; exception %s",
							typeRef.getFQN(), clazz.getClassName()
								.getFQN(),
							e);
					}
				}
			}
		}
	}

	// TODO what about Queue|Stack|Deque?
	static final Pattern	GENERIC					= Pattern
		.compile("((Ljava/util/Collection|Ljava/util/Set|Ljava/util/List|Ljava/lang/Iterable)|(.*))<(L.+;)>;");

	// Determine if we can identify that this class is a concrete subtype of
	// collection with a no-arg constructor
	// So far this implementation doesn't try very hard. It only looks to see if
	// the class directly implements a known collection interface.
	static final Pattern	COLLECTION				= Pattern
		.compile("(" + Collection.class.getName() + "|" + Set.class.getName() + "|" + List.class.getName() + "|"
			+ Queue.class.getName() + "|" + Stack.class.getName() + "|" + Deque.class.getName() + ")");

	static final Pattern	IDENTIFIERTOPROPERTY	= Pattern.compile("(__)|(_)|(\\$_\\$)|(\\$\\$)|(\\$)");

	private final class OCDDataCollector extends ClassDataCollector {
		private final OCDDef				ocd;
		private final Map<MethodDef, ADDef>	methods			= new LinkedHashMap<>();
		private Clazz						clazz;
		private TypeRef						name;
		private int							hasNoDefault	= 0;
		private boolean						hasValue		= false;
		private FieldDef					prefixField		= null;
		private ADDef						current;

		OCDDataCollector(OCDDef ocd) {
			this.ocd = ocd;
		}

		@Override
		public boolean classStart(Clazz clazz) {
			this.clazz = clazz;
			this.name = clazz.getClassName();
			return true;
		}

		@Override
		public void field(FieldDef defined) {
			if (defined.isStatic() && defined.getName()
				.equals("PREFIX_")) {
				prefixField = defined;
			}
		}

		@Override
		public void method(MethodDef defined) {
			if (defined.isStatic()) {
				current = null;
				return;
			}
			current = new ADDef(finder);
			methods.put(defined, current);
			if (clazz.isAnnotation()) {
				if (defined.getName()
					.equals("value")) {
					hasValue = true;
				} else {
					hasNoDefault++;
				}
			}
		}

		@Override
		public void annotationDefault(MethodDef defined, Object value) {
			if (!defined.getName()
				.equals("value")) {
				hasNoDefault--;
			}
		}

		@Override
		public void annotation(Annotation annotation) throws Exception {
			try {
				switch (annotation.getName()
					.getFQN()) {
					case "org.osgi.service.metatype.annotations.ObjectClassDefinition" :
						doOCD(annotation.getAnnotation(ObjectClassDefinition.class), annotation);
						break;
					case "org.osgi.service.metatype.annotations.AttributeDefinition" :
						current.ad = annotation.getAnnotation(AttributeDefinition.class);
						current.a = annotation;
						break;
					default :
						XMLAttribute xmlAttr = finder.getXMLAttribute(annotation);
						if (xmlAttr != null) {
							doXmlAttribute(annotation, xmlAttr);
						}
						break;
				}
			} catch (Exception e) {
				analyzer.exception(e, "During generation of a component on class %s, exception %s", clazz, e);
			}
		}

		@Override
		public void memberEnd() {
			current = null;
		}

		@Override
		public void classEnd() throws Exception {
			current = null;
			if (ocd.id == null) {
				return; // no ObjectClassDefinition annotation found
			}
			String prefix = null;
			if (prefixField != null) {
				Object c = prefixField.getConstant();
				if (prefixField.isFinal() && (prefixField.getType() == analyzer.getTypeRef("java/lang/String"))
					&& (c instanceof String)) {
					prefix = (String) c;
					ocd.updateVersion(MetatypeVersion.VERSION_1_4);
				} else {
					analyzer.warning(
						"Field PREFIX_ in %s is not a static final String field with a compile-time constant value: %s",
						name.getFQN(), c);
				}
			}
			String singleElementAnnotation = null;
			if (hasValue && (hasNoDefault == 0)) {
				StringBuilder sb = new StringBuilder(name.getShorterName());
				boolean lastLowerCase = false;
				for (int i = 0; i < sb.length(); i++) {
					char c = sb.charAt(i);
					if (Character.isUpperCase(c)) {
						sb.setCharAt(i, Character.toLowerCase(c));
						if (lastLowerCase) {
							sb.insert(i++, '.');
						}
						lastLowerCase = false;
					} else {
						lastLowerCase = Character.isLowerCase(c);
					}
				}
				singleElementAnnotation = sb.toString();
				ocd.updateVersion(MetatypeVersion.VERSION_1_4);
			}

			for (Map.Entry<MethodDef, ADDef> entry : methods.entrySet()) {
				MethodDef defined = entry.getKey();
				if (defined.isConstructor()) {
					analyzer.error("Constructor %s for %s.%s found; only interfaces and annotations allowed for OCDs",
						defined.getName(), clazz.getClassName()
							.getFQN(),
						defined.getName());

				}
				if (defined.getPrototype().length > 0) {
					analyzer.error(
						"Element %s for %s.%s has parameters; only no-parameter elements in an OCD interface allowed",
						defined.getName(), clazz.getClassName()
							.getFQN(),
						defined.getName());
					continue;
				}
				ADDef ad = entry.getValue();
				ocd.attributes.add(ad);
				String key = defined.getName();
				if ((singleElementAnnotation != null) && key.equals("value")) {
					key = singleElementAnnotation;
				} else {
					key = identifierToPropertyName(key);
				}
				if (prefix != null) {
					key = prefix + key;
				}
				ad.id = key;
				ad.name = space(defined.getName());
				String rtype = defined.getGenericReturnType();
				if (rtype.startsWith("[")) {
					ad.cardinality = Integer.MAX_VALUE;
					rtype = rtype.substring(1);
				}
				Matcher m = GENERIC.matcher(rtype);
				if (m.matches()) {
					boolean knownCollection = m.group(2) != null;
					boolean collection = knownCollection || identifiableCollection(m.group(3) + ";", false);
					if (collection) {
						if (ad.cardinality != 0)
							analyzer.error(
								"AD for %s.%s uses an array of collections in return type (%s), Metatype allows either Vector or array",
								clazz.getClassName()
									.getFQN(),
								defined.getName(), defined.getType()
									.getFQN());
						rtype = m.group(4);
						ad.cardinality = Integer.MIN_VALUE;
					}
				}
				if (rtype.indexOf('<') > 0) {
					rtype = rtype.substring(0, rtype.indexOf('<')) + ";";
				}
				ad.type = getType(rtype);

				ad.required = true;
				TypeRef typeRef = analyzer.getTypeRef(rtype);
				try {
					Clazz c = analyzer.findClass(typeRef);
					if (c != null && c.isEnum()) {
						parseOptionValues(c, ad.options);
					}
				} catch (Exception e) {
					analyzer.exception(e, "AD for %s.%s Can not parse option values from type (%s), %s",
						clazz.getClassName()
							.getFQN(),
						defined.getName(), defined.getType()
							.getFQN(),
						e);
				}
				if (ad.ad != null) {
					doAD(ad);
				}
				if (ad.defaults == null && clazz.isAnnotation() && defined.getConstant() != null) {
					// defaults from annotation default
					Object value = defined.getConstant();
					boolean isClass = false;
					TypeRef type = defined.getType()
						.getClassRef();
					if (!type.isPrimitive()) {
						if (Class.class.getName()
							.equals(type.getFQN())) {
							isClass = true;
						} else {
							try {
								Clazz r = analyzer.findClass(type);
								if (r.isAnnotation()) {
									analyzer.warning("Nested annotation type found in field %s, %s", defined.getName(),
										type.getFQN());
									return;
								}
							} catch (Exception e) {
								analyzer.exception(e,
									"Exception looking at annotation type default for element with descriptor %s,  type %s",
									defined, type);
							}
						}
					}
					if (value != null) {
						if (value.getClass()
							.isArray()) {
							// add element individually
							ad.defaults = new String[Array.getLength(value)];
							for (int i = 0; i < Array.getLength(value); i++) {
								Object element = Array.get(value, i);
								ad.defaults[i] = valueToProperty(element, isClass);
							}
						} else {
							ad.defaults = new String[] {
								valueToProperty(value, isClass)
							};
						}
					}
				}
			}
		}

		private void doOCD(ObjectClassDefinition o, Annotation annotation) {
			if (ocd.id == null) {
				if (clazz.isInterface()) {
					ocd.id = annotation.get("id") == null ? name.getFQN() : o.id();
					ocd.name = annotation.get("name") == null ? space(ocd.id) : o.name();
					ocd.description = annotation.get("description") == null ? "" : o.description();
					ocd.localization = annotation.get("localization") == null ? "OSGI-INF/l10n/" + name.getFQN()
						: o.localization();
					if (annotation.get("pid") != null) {
						String[] pids = o.pid();
						designates(name.getFQN(), pids, false);
					}
					if (annotation.get("factoryPid") != null) {
						String[] pids = o.factoryPid();
						designates(name.getFQN(), pids, true);
					}
					if (annotation.get("icon") != null) {
						Icon[] icons = o.icon();
						for (Icon icon : icons) {
							ocd.icons.add(new IconDef(icon.resource(), icon.size()));
						}
					}
				} else {
					analyzer.error("ObjectClassDefinition applied to non-interface, non-annotation class %s", clazz);
				}
			}
		}

		private void doAD(ADDef adDef) throws Exception {
			AttributeDefinition ad = adDef.ad;
			Annotation a = adDef.a;

			if (a.get("name") != null) {
				adDef.name = ad.name();
			}
			adDef.description = a.get("description");
			if (a.get("type") != null) {
				adDef.type = ad.type();
			}
			if (a.get("cardinality") != null) {
				adDef.cardinality = ad.cardinality();
			}
			if (a.get("max") != null) {
				adDef.max = ad.max();
			}
			if (a.get("min") != null) {
				adDef.min = ad.min();
			}
			if (a.get("defaultValue") != null) {
				adDef.defaults = ad.defaultValue();
			}
			if (a.get("required") != null) {
				adDef.required = ad.required();
			}
			if (a.get("options") != null) {
				adDef.options.clear();
				for (Object o : (Object[]) a.get("options")) {
					Option opt = ((Annotation) o).getAnnotation(Option.class);
					adDef.options.add(new OptionDef(opt.label(), opt.value()));
				}
			}
		}

		private void doXmlAttribute(Annotation annotation, XMLAttribute xmlAttr) {
			if (current == null) {
				if (clazz.isInterface()) {
					ocd.addExtensionAttribute(xmlAttr, annotation);
				}
			} else {
				current.addExtensionAttribute(xmlAttr, annotation);
			}
		}

		private boolean identifiableCollection(String type, boolean intface) {
			return identifiableCollection(analyzer.getTypeRef(type), intface, true);
		}

		private boolean identifiableCollection(TypeRef type, boolean intface, boolean topLevel) {
			try {
				Clazz clazz = analyzer.findClass(type);
				if (clazz != null && (!topLevel || !clazz.isAbstract())
					&& ((intface && clazz.isInterface()) ^ clazz.hasPublicNoArgsConstructor())) {
					TypeRef[] intfs = clazz.getInterfaces();
					if (intfs != null) {
						for (TypeRef intf : intfs) {
							if (COLLECTION.matcher(intf.getFQN())
								.matches() || identifiableCollection(intf, true, false)) {
								return true;
							}
						}
					}
					TypeRef ext = clazz.getSuper();
					return ext != null && identifiableCollection(ext, false, false);
				}
			} catch (Exception e) {
				return false;
			}
			return false;
		}

		private String valueToProperty(Object value, boolean isClass) {
			if (isClass)
				return ((TypeRef) value).getFQN();
			return value.toString();
		}

		private void parseOptionValues(Clazz c, final List<OptionDef> options) throws Exception {

			c.parseClassFileWithCollector(new ClassDataCollector() {
				@Override
				public void field(Clazz.FieldDef def) {
					if (def.isEnum()) {
						OptionDef o = new OptionDef(def.getName(), def.getName());
						options.add(o);
					}
				}
			});
		}

		private AttributeType getType(String rtype) {
			if (rtype.startsWith("[")) {
				analyzer.error("Can only handle array of depth one field , nested type %s", rtype);
				return null;
			}
			switch (rtype) {
				case "Z" :
				case "Ljava/lang/Boolean;" :
					return AttributeType.BOOLEAN;
				case "B" :
				case "Ljava/lang/Byte;" :
					return AttributeType.BYTE;
				case "C" :
				case "Ljava/lang/Character;" :
					return AttributeType.CHARACTER;
				case "S" :
				case "Ljava/lang/Short;" :
					return AttributeType.SHORT;
				case "I" :
				case "Ljava/lang/Integer;" :
					return AttributeType.INTEGER;
				case "J" :
				case "Ljava/lang/Long;" :
					return AttributeType.LONG;
				case "F" :
				case "Ljava/lang/Float;" :
					return AttributeType.FLOAT;
				case "D" :
				case "Ljava/lang/Double;" :
					return AttributeType.DOUBLE;
				case "Ljava/lang/String;" :
				case "Ljava/lang/Class;" :
					return AttributeType.STRING;
				default :
					if (acceptableType(rtype)) {
						return AttributeType.STRING;
					}
					return null;
			}
		}

		private boolean acceptableType(String rtype) {
			TypeRef ref = analyzer.getTypeRef(rtype);
			try {
				Clazz returnType = analyzer.findClass(ref);
				if (returnType.isEnum()) {
					return true;
				}
				// TODO check this is true for interfaces and annotations
				if (!returnType.isAbstract() || (returnType.isInterface() && options.contains(Options.nested))) {
					return true;
				}
				if (!returnType.isInterface()) {
					analyzer.error("Abstract classes not allowed as interface method return values: %s", rtype);
				} else {
					analyzer.error("Nested metatype only allowed with option: nested type %s", rtype);
				}
				return false;
			} catch (Exception e) {
				analyzer.exception(e, "could not examine class for return type %s, exception message: %s", rtype, e);
				return false;
			}
		}

		private String identifierToPropertyName(String name) {
			Matcher m = IDENTIFIERTOPROPERTY.matcher(name);
			StringBuilder sb = new StringBuilder();
			int start = 0;
			for (; m.find(); start = m.end()) {
				sb.append(name, start, m.start());
				switch (m.group()) {
					case "__" : // __ to _
						sb.append('_');
						break;
					case "_" : // _ to .
						sb.append('.');
						break;
					case "$_$" : // $_$ to -
						sb.append('-');
						ocd.updateVersion(MetatypeVersion.VERSION_1_4);
						break;
					case "$$" : // $$ to $
						sb.append('$');
						break;
					case "$" : // $ removed
						break;
					default : // unknown!
						sb.append(m.group());
						analyzer.error("unknown mapping %s in property name %s", m.group(), name);
						break;
				}
			}
			return (start == 0) ? name
				: sb.append(name, start, name.length())
					.toString();
		}

		private String space(String name) {
			return Clazz.unCamel(name);
		}

		private void designates(String name, String[] pids, boolean factory) {
			for (String pid : pids) {
				if ("$".equals(pid)) {
					pid = name;
				}
				ocd.designates.add(new DesignateDef(ocd.id, pid, factory, finder));
			}
		}
	}
}
