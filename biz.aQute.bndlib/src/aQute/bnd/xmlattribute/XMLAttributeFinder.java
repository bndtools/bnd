package aQute.bnd.xmlattribute;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import aQute.bnd.annotation.xml.XMLAttribute;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Annotation;
import aQute.bnd.osgi.ClassDataCollector;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors.TypeRef;

public class XMLAttributeFinder extends ClassDataCollector {

	private final Analyzer				analyzer;

	Map<TypeRef, XMLAttribute>			annoCache		= new HashMap<>();
	Map<TypeRef, Map<String, String>>	defaultsCache	= new HashMap<>();

	public XMLAttributeFinder(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	public synchronized XMLAttribute getXMLAttribute(Annotation a) throws Exception {
		TypeRef name = a.getName();
		if (annoCache.containsKey(name))
			return annoCache.get(name);
		Clazz clazz = analyzer.findClass(name);
		if (clazz != null) {
			xmlAttr = null;
			clazz.parseClassFileWithCollector(this);
			annoCache.put(name, xmlAttr);
			return xmlAttr;
		}
		return null;
	}

	XMLAttribute xmlAttr;

	@Override
	public void annotation(Annotation annotation) throws Exception {
		String fqn = annotation.getName()
			.getFQN();
		if (fqn.equals("aQute.bnd.annotation.xml.XMLAttribute")) {
			xmlAttr = annotation.getAnnotation(XMLAttribute.class);
		}
	}

	public Map<String, String> getDefaults(Annotation a) {
		TypeRef name = a.getName();
		Map<String, String> defaults = defaultsCache.get(name);
		if (defaults == null)
			defaults = extractDefaults(name, analyzer);
		if (defaults == null)
			return new LinkedHashMap<>();
		return new LinkedHashMap<>(defaults);
	}

	private Map<String, String> extractDefaults(TypeRef name, final Analyzer analyzer) {
		try {
			Clazz clazz = analyzer.findClass(name);
			final Map<String, String> props = new LinkedHashMap<>();
			clazz.parseClassFileWithCollector(new ClassDataCollector() {

				@Override
				public void annotationDefault(Clazz.MethodDef defined) {
					Object value = defined.getConstant();
					// check type, exit with warning if annotation or annotation
					// array
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
								analyzer.exception(e, "Exception extracting annotation defaults for type %s", type);
								return;
							}
						}
					}
					if (value != null) {
						String name = defined.getName();
						if (value.getClass()
							.isArray()) {
							StringBuilder sb = new StringBuilder();
							String sep = "";
							// add element individually
							for (int i = 0; i < Array.getLength(value); i++) {
								Object element = Array.get(value, i);
								sb.append(sep)
									.append(convert(element, isClass));
								sep = " ";
							}
							props.put(name, sb.toString());
						} else {
							props.put(name, convert(value, isClass));
						}
					}
				}

				private String convert(Object value, boolean isClass) {
					if (isClass)
						return ((TypeRef) value).getFQN();
					return String.valueOf(value);
				}

			});
			defaultsCache.put(name, props);
			return props;
		} catch (Exception e) {
			analyzer.exception(e, "Exception extracting annotation defaults for type %s", name);
		}
		return null;
	}

}
