package aQute.bnd.metatype;

import java.util.*;
import java.util.regex.*;

import org.osgi.service.metatype.annotations.*;

import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Descriptors.TypeRef;

public class OCDReader extends ClassDataCollector {
	
	private Analyzer	analyzer;
	private Clazz	clazz;
	@SuppressWarnings("unused")
	private boolean	inherit;
	
	private TypeRef name;

	private final Map<MethodDef, Pair> methods = new LinkedHashMap<MethodDef, Pair>();
	private MethodDef	method;
	private OCDDef ocd;


	OCDReader(Analyzer analyzer, Clazz clazz, boolean inherit) {
		this.analyzer = analyzer;
		this.clazz = clazz;
		this.inherit = inherit;
	}


     static OCDDef getOCDDef(Clazz c, Analyzer analyzer) throws Exception {
 		boolean inherit = Processor.isTrue(analyzer.getProperty("-metatypeannotations-inherit"));
 		OCDReader r = new OCDReader(analyzer, c, inherit);
 		return r.getDef();
	}


	private OCDDef getDef() throws Exception {
		clazz.parseClassFileWithCollector(this);
		if (ocd != null) {
			doMethods();
		}
		return ocd;
	}


	@Override
	public void classBegin(int access, TypeRef name) {
		this.name = name;
	}

	@Override
	public void method(MethodDef defined) {
		methods.put(defined, null);
		method = defined;
	}
	
	static Pattern	COLLECTION	= Pattern.compile("(.*(Collection|Set|List|Queue|Stack|Deque))<(L.+;)>");
	private void doMethods() throws Exception {
		for (Map.Entry<MethodDef,Pair> entry: methods.entrySet()) {
			MethodDef defined = entry.getKey();
			ADDef ad = new ADDef();
			ocd.attributes.add(ad);
			ad.id = fixup(defined.getName());
			ad.name = space(defined.getName());
			ad.description = "";
			String rtype = defined.getGenericReturnType();
			ad.type = getType(rtype);
			if (rtype.endsWith("[]")) {
				ad.cardinality = Integer.MAX_VALUE;
				rtype = rtype.substring(0, rtype.length() - 2);
			}
			if (rtype.indexOf('<') > 0) {
				if (ad.cardinality != 0)
					analyzer.error(
							"AD for %s.%s uses an array of collections in return type (%s), Metatype allows either Vector or array",
							clazz.getClassName().getFQN(), defined.getName(), defined.getType().getFQN());
				Matcher m = COLLECTION.matcher(rtype);
				if (m.matches()) {
					rtype = Clazz.objectDescriptorToFQN(m.group(3));
					ad.cardinality = Integer.MIN_VALUE;
				}
			}

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
			if (entry.getValue() != null) {
				doAD(ad, entry.getValue());
			}
		}
	}

	private void doAD(ADDef ad, Pair pair) throws Exception {
		AttributeDefinition a = pair.getAd();
		Annotation annotation = pair.getA(); 

		if (a.name() != null) {
			ad.name = a.name();
		}
		ad.description = a.description();
		if (a.type() != null) {
			ad.type = a.type().toString();
		}
		ad.cardinality = a.cardinality();
		ad.max = a.max();
		ad.min = a.min();
		ad.defaults = a.defaultValue();
		if (annotation.get("required") != null) {
			ad.required = a.required();
		}
		if (annotation.get("options") != null) {
			ad.options.clear();
			for (Object o : (Object[])annotation.get("options")) {
				Option opt = ((Annotation)o).getAnnotation();
				ad.options.add(new OptionDef(opt.label(), opt.value()));
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
    	return Clazz.unCamel(name);//TODO I don't understand "spaces" rule in spec 5.2
    }

	String getType(String rtype) {
		if (rtype.endsWith("[]")) {
			rtype = rtype.substring(0, rtype.length() - 2);
			if (rtype.endsWith("[]"))
				throw new IllegalArgumentException("Can only handle array of depth one");
		}

		if ("boolean".equals(rtype) || Boolean.class.getName().equals(rtype))
			return AttributeType.BOOLEAN.toString();
		else if ("byte".equals(rtype) || Byte.class.getName().equals(rtype))
			return AttributeType.BYTE.toString();
		else if ("char".equals(rtype) || Character.class.getName().equals(rtype))
			return AttributeType.CHARACTER.toString();
		else if ("short".equals(rtype) || Short.class.getName().equals(rtype))
			return AttributeType.SHORT.toString();
		else if ("int".equals(rtype) || Integer.class.getName().equals(rtype))
			return AttributeType.INTEGER.toString();
		else if ("long".equals(rtype) || Long.class.getName().equals(rtype))
			return AttributeType.LONG.toString();
		else if ("float".equals(rtype) || Float.class.getName().equals(rtype))
			return AttributeType.FLOAT.toString();
		else if ("double".equals(rtype) || Double.class.getName().equals(rtype))
			return AttributeType.DOUBLE.toString();
		else
			return AttributeType.STRING.toString();
	}
    
	private void parseOptionValues(Clazz c, final List<OptionDef> options) throws Exception {

		c.parseClassFileWithCollector(new ClassDataCollector() {
			@Override
			public void field(Clazz.FieldDef def) {
				if (def.isEnum()) {
					OptionDef o = new OptionDef(def.getName(), def.getName()); //TODO first arg should be "toString()" result
					options.add(o);
				}
			}
		});
	}

	private final static class Pair {
		private final AttributeDefinition ad;
		private final Annotation a;
		
		public Pair(AttributeDefinition ad, Annotation a) {
			this.ad = ad;
			this.a = a;
		}

		AttributeDefinition getAd() {
			return ad;
		}

		Annotation getA() {
			return a;
		}
				
	}
	
    @Override
	public void annotation(Annotation annotation) throws Exception {
		try {
			java.lang.annotation.Annotation a = annotation.getAnnotation();
			if (a instanceof ObjectClassDefinition)
				doOCD((ObjectClassDefinition) a, annotation);
			else if (a instanceof AttributeDefinition)
				methods.put(method, new Pair((AttributeDefinition)a, annotation));
		}
		catch (Exception e) {
			e.printStackTrace();
			analyzer.error("During generation of a component on class %s, exception %s", clazz, e);
		}
	}

	private void doOCD(ObjectClassDefinition o, Annotation annotation) {
		ocd = new OCDDef();
		ocd.id = o.id() == null? name.getFQN(): o.id();
		ocd.name = o.name() == null? space(ocd.id): o.name();  
		ocd.description = o.description() == null? "": o.description();
		ocd.localization = o.localization() == null? "OSGI-INF/l10n/" + name.getFQN(): o.localization();
		if (annotation.get("pid") != null ) {
			String[] pids = o.pid();
			designates(pids, false);
		}
		if (annotation.get("factoryPid") != null ) {
			String[] pids = o.factoryPid();
			designates(pids, true);
		}
		if (annotation.get("icon") != null) {
			Icon[] icons = o.icon();
			for (Icon icon: icons) {
				ocd.icons.add(new IconDef(icon.resource(), icon.size()));
			}
		}
		
	}

	private void designates(String[] pids, boolean factory) {
		for (String pid: pids) {
			ocd.designates.add(new DesignateDef(ocd.id, pid, factory));
		}		
	}




}
