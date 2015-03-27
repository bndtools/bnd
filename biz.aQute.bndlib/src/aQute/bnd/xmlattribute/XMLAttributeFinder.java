package aQute.bnd.xmlattribute;

import java.util.*;

import aQute.bnd.annotation.xml.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Descriptors.TypeRef;

public class XMLAttributeFinder extends ClassDataCollector {

	Map<TypeRef,XMLAttribute>	annoCache	= new HashMap<TypeRef,XMLAttribute>();

	public XMLAttribute getXMLAttribute(Annotation a, Analyzer analyzer) throws Exception {
		TypeRef name = a.getName();
		if (annoCache.containsKey(name))
			return annoCache.get(name);
		Clazz clazz = analyzer.findClass(name);
		if (clazz != null) {
			clazz.parseClassFileWithCollector(this);
			annoCache.put(name, xmlAttr);
			return xmlAttr;
		}
		return null;
	}

	XMLAttribute	xmlAttr;

	@Override
	public void annotation(Annotation annotation) throws Exception {
		java.lang.annotation.Annotation a = annotation.getAnnotation();
		if (a instanceof XMLAttribute)
			xmlAttr = (XMLAttribute) a;
	}

}