package aQute.bnd.metatype;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import org.osgi.service.metatype.annotations.*;

import aQute.bnd.annotation.xml.*;
import aQute.bnd.metatype.MetatypeAnnotations.Options;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.xmlattribute.*;

public class OCDReader extends ClassDataCollector {
	
	private Analyzer	analyzer;
	private Clazz	clazz;
	private EnumSet<Options>			options;
	
	private TypeRef name;
	private boolean topLevel = true;
	private Set<TypeRef> analyzed;

	private final Map<MethodDef,ADDef>	methods		= new LinkedHashMap<MethodDef,ADDef>();
	private ADDef						current;
	private OCDDef ocd;

	private final XMLAttributeFinder	finder;


	OCDReader(Analyzer analyzer, Clazz clazz, EnumSet<Options> options, XMLAttributeFinder finder) {
		this.analyzer = analyzer;
		this.clazz = clazz;
		this.options = options;
		this.finder = finder;
	}


	static OCDDef getOCDDef(Clazz c, Analyzer analyzer, EnumSet<Options> options, XMLAttributeFinder finder) throws Exception {

		OCDReader r = new OCDReader(analyzer, c, options, finder);
		return r.getDef();
	}


     private OCDDef getDef() throws Exception {
    	 clazz.parseClassFileWithCollector(this);
    	 if (ocd != null) {
    		 topLevel = false;
    		 parseExtends(clazz);
    		 
    		 doMethods();
    	 }
    	 return ocd;
     }


     private void parseExtends(Clazz clazz) {
    	 TypeRef[] inherits = clazz.getInterfaces();
    	 if (inherits != null) {
    		 if (analyzed == null) {
    			 analyzed = new HashSet<TypeRef>();
    		 }
    		 for (TypeRef typeRef: inherits) {
    			 if (!typeRef.isJava() && analyzed.add(typeRef)) {
    				 try {
    					 Clazz inherit = analyzer.findClass(typeRef);
    					 if (inherit != null) {
    						 inherit.parseClassFileWithCollector(this);
    						 parseExtends(inherit);
    					 } else {
    							analyzer.error(
    									"Could not obtain super class %s of class %s",
    									typeRef.getFQN(), clazz.getClassName().getFQN());	
    					 }
    				 }
    				 catch (Exception e) {
							analyzer.error(
									"Could not obtain super class %s of class %s; exception %s",
									typeRef.getFQN(), clazz.getClassName().getFQN(), e.getMessage());	
    				 }
    			 }

    		 }
    	 }

     }


	@Override
	public void classBegin(int access, TypeRef name) {
		this.name = name;
	}

	@Override
	public void method(MethodDef defined) {
		current = new ADDef();
		methods.put(defined, current);
	}

	@Override
	public void classEnd() throws Exception {
		current = null;
	}

	@Override
	public void memberEnd() {
		current = null;
	}

	//TODO what about Queue|Stack|Deque?
	static Pattern	GENERIC	= Pattern.compile("((" + Collection.class.getName() + "|" + 
			Set.class.getName() + "|" +
			List.class.getName() + "|" +
			Iterable.class.getName() + ")|(.*))<(L.+;)>");
	private void doMethods() throws Exception {
		for (Map.Entry<MethodDef,ADDef> entry : methods.entrySet()) {
			MethodDef defined = entry.getKey();
			if (defined.isConstructor()) {
				analyzer.error(
						"Constructor %s for %s.%s found; only interfaces and annotations allowed for OCDs",
						defined.getName(), clazz.getClassName().getFQN(), defined.getName());	

			}
			if (defined.getPrototype().length > 0) {
				analyzer.error(
						"Element %s for %s.%s has parameters; only no-parameter elements in an OCD interface allowed",
						defined.getName(), clazz.getClassName().getFQN(), defined.getName());	
				continue;
			}
			ADDef ad = entry.getValue();
			ocd.attributes.add(ad);
			ad.id = fixup(defined.getName());
			ad.name = space(defined.getName());
			String rtype = defined.getGenericReturnType();
			if (rtype.endsWith("[]")) {
				ad.cardinality = Integer.MAX_VALUE;
				rtype = rtype.substring(0, rtype.length() - 2);
			}
			Matcher m = GENERIC.matcher(rtype);
			if (m.matches()) {
				boolean knownCollection = m.group(2) != null;
				boolean collection = knownCollection || identifiableCollection(m.group(3), false, true);
				if (collection) {
					if (ad.cardinality != 0)
						analyzer.error(
								"AD for %s.%s uses an array of collections in return type (%s), Metatype allows either Vector or array",
								clazz.getClassName().getFQN(), defined.getName(), defined.getType().getFQN());
					rtype = Clazz.objectDescriptorToFQN(m.group(4));
					ad.cardinality = Integer.MIN_VALUE;
				}
			}
			if (rtype.indexOf('<') > 0) {
				rtype = rtype.substring(0, rtype.indexOf('<'));
			}
			ad.type = getType(rtype);

			ad.required = true;
			TypeRef typeRef = analyzer.getTypeRefFromFQN(rtype);
			try {
				Clazz c = analyzer.findClass(typeRef);
				if (c != null && c.isEnum()) {
					parseOptionValues(c, ad.options);
				}
			}
			catch (Exception e) {
				analyzer.error(
						"AD for %s.%s Can not parse option values from type (%s), %s",
						clazz.getClassName().getFQN(), defined.getName(), defined.getType().getFQN(), e.getMessage());
			}
			if (ad.ad != null) {
				doAD(ad);
			}
			if (ad.defaults == null && clazz.isAnnotation() && defined.getConstant() != null) {
				//defaults from annotation default
				Object value = defined.getConstant();
				boolean isClass = false;
				TypeRef type = defined.getType().getClassRef();
				if (!type.isPrimitive()) {
					if (Class.class.getName().equals(type.getFQN())) {
						isClass = true;
					} else {
						try {
							Clazz r = analyzer.findClass(type);
							if (r.isAnnotation()) {
								analyzer.warning("Nested annotation type found in field % s, %s", defined.getName(), type.getFQN());
								return;
							}
						} catch (Exception e) {
							analyzer.error("Exception looking at annotation type default for element with descriptor %s,  type %s", e, defined, type);
						}
					}
				}
				if (value != null) {
					if (value.getClass().isArray()) {
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

	//Determine if we can identify that this class is a concrete subtype of collection with a no-arg constructor
	//So far this implementation doesn't try very hard. It only looks to see if the class directly implements a known collection interface.
	static Pattern	COLLECTION	= Pattern.compile("(" + Collection.class.getName() + "|" + 
			Set.class.getName() + "|" +
			List.class.getName() + "|" +
			Queue.class.getName() + "|" +
			Stack.class.getName() + "|" +
			Deque.class.getName() + ")");
	
	private boolean identifiableCollection(String type, boolean intface, boolean topLevel) {
		try {
			Clazz clazz = analyzer.findClass(analyzer.getTypeRefFromFQN(type));
			if (clazz != null &&
					(!topLevel || !clazz.isAbstract()) && 
					((intface && clazz.isInterface()) ^ clazz.hasPublicNoArgsConstructor())) {
				TypeRef[] intfs= clazz.getInterfaces();
				if (intfs != null) {
					for (TypeRef intf : intfs) {
						if (COLLECTION.matcher(intf.getFQN()).matches()
								|| identifiableCollection(intf.getFQN(), true, false)) {
							return true;
						}
					}
				}
				TypeRef ext = clazz.getSuper();
				return ext != null && identifiableCollection(ext.getFQN(), false, false);
			}
		}
		catch (Exception e) {
			return false;
		}
		return false;
	}


	private String valueToProperty(Object value, boolean isClass) {
		if (isClass)
			return ((TypeRef) value).getFQN();
		return value.toString();
	}
	
	private void doAD(ADDef adDef) throws Exception {
		AttributeDefinition ad = adDef.ad;
		Annotation a = adDef.a;

		if (ad.name() != null) {
			adDef.name = ad.name();
		}
		adDef.description = a.get("description");
		if (a.get("type") != null) {
			adDef.type = ad.type();
		}
		if (a.get("cardinality") != null) {
			adDef.cardinality = ad.cardinality();
		}
		adDef.max = ad.max();
		adDef.min = ad.min();
		if (a.get("defaultValue") != null) {
			adDef.defaults = ad.defaultValue();
		}
		if (a.get("required") != null) {
			adDef.required = ad.required();
		}
		if (a.get("options") != null) {
			adDef.options.clear();
			for (Object o : (Object[]) a.get("options")) {
				Option opt = ((Annotation)o).getAnnotation();
				adDef.options.add(new OptionDef(opt.label(), opt.value()));
			}
		}

	}

	private static final Pattern p = Pattern.compile("(\\$\\$)|(\\$)|(__)|(_)");
    
    static String fixup(String name)
    {
        Matcher m = p.matcher(name);
        StringBuffer b = new StringBuffer();
        while (m.find())
        {
            String replacement = "";//null;
            if (m.group(1) != null) replacement = "\\$";
            if (m.group(2) != null) replacement = "";
            if (m.group(3) != null) replacement = "_";
            if (m.group(4) != null) replacement = ".";
            
            m.appendReplacement(b, replacement);
        }
        m.appendTail(b);
        return b.toString();
    }
    
    static String space(String name) {
    	return Clazz.unCamel(name);
    }

    AttributeType getType(String rtype) {
    	if (rtype.endsWith("[]")) {
			analyzer.error("Can only handle array of depth one field , nested type %s", rtype);
    		return null;
    	}

    	if ("boolean".equals(rtype) || Boolean.class.getName().equals(rtype))
    		return AttributeType.BOOLEAN;
    	else if ("byte".equals(rtype) || Byte.class.getName().equals(rtype))
    		return AttributeType.BYTE;
    	else if ("char".equals(rtype) || Character.class.getName().equals(rtype))
    		return AttributeType.CHARACTER;
    	else if ("short".equals(rtype) || Short.class.getName().equals(rtype))
    		return AttributeType.SHORT;
    	else if ("int".equals(rtype) || Integer.class.getName().equals(rtype))
    		return AttributeType.INTEGER;
    	else if ("long".equals(rtype) || Long.class.getName().equals(rtype))
    		return AttributeType.LONG;
    	else if ("float".equals(rtype) || Float.class.getName().equals(rtype))
    		return AttributeType.FLOAT;
    	else if ("double".equals(rtype) || Double.class.getName().equals(rtype))
    		return AttributeType.DOUBLE;
    	else if (String.class.getName().equals(rtype) || Class.class.getName().equals(rtype) || acceptableType(rtype) ) 
    		return AttributeType.STRING;
    	else {
    		return null;

    	}
    }
    
	private boolean acceptableType(String rtype) {
		TypeRef ref = analyzer.getTypeRefFromFQN(rtype);
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
		}
		catch (Exception e) {
			analyzer.error("could not examine class for return type %s, exception message: %s", rtype, e.getMessage());
			return false;
		}
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

	
    @Override
	public void annotation(Annotation annotation) throws Exception {
		try {
			java.lang.annotation.Annotation a = annotation.getAnnotation();
			if (a instanceof ObjectClassDefinition)
				doOCD((ObjectClassDefinition) a, annotation);
			else if (a instanceof AttributeDefinition) {
				current.ad = (AttributeDefinition) a;
				current.a = annotation;
			} else {
				XMLAttribute xmlAttr = finder.getXMLAttribute(annotation, analyzer);
				if (xmlAttr != null) {
					doXmlAttribute(annotation, xmlAttr);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			analyzer.error("During generation of a component on class %s, exception %s", clazz, e);
		}
	}

	private void doXmlAttribute(Annotation annotation, XMLAttribute xmlAttr) {
		if (current == null) {
			if (clazz.isInterface()) {
				if (ocd == null)
					ocd = new OCDDef();
				ocd.addExtensionAttribute(xmlAttr, annotation);
			}
		} else {
			current.addExtensionAttribute(xmlAttr, annotation);
		}
	}

	private void doOCD(ObjectClassDefinition o, Annotation annotation) {
    	if (topLevel) {
			if (clazz.isInterface()) {
				if (ocd == null)
					ocd = new OCDDef();
				ocd.id = o.id() == null ? name.getFQN() : o.id();
				ocd.name = o.name() == null ? space(ocd.id) : o.name();
				ocd.description = o.description() == null ? "" : o.description();
				ocd.localization = o.localization() == null ? "OSGI-INF/l10n/" + name.getFQN() : o.localization();
				if (annotation.get("pid") != null) {
					String[] pids = o.pid();
					designates(pids, false);
				}
				if (annotation.get("factoryPid") != null) {
					String[] pids = o.factoryPid();
					designates(pids, true);
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

	private void designates(String[] pids, boolean factory) {
		for (String pid: pids) {
			ocd.designates.add(new DesignateDef(ocd.id, pid, factory));
		}		
	}




}
