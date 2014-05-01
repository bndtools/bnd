package aQute.bnd.component;

import aQute.bnd.osgi.*;

public interface ExtensionReader {
	
	void doAnnotation(java.lang.annotation.Annotation a, Annotation bndAnno, ComponentDef componentDef, Analyzer analyzer);

	String name();

}
