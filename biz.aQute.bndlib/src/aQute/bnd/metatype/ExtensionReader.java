package aQute.bnd.metatype;

import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Clazz.MethodDef;

public interface ExtensionReader {
	
	void doAnnotation(java.lang.annotation.Annotation a, Annotation bndAnno, OCDDef ocdDef, Analyzer analyzer);

	void doMethodAnnotation(MethodDef method, java.lang.annotation.Annotation ad, Annotation a, OCDDef ocd, ADDef adDef,
			Analyzer analyzer);

	String name();

}
