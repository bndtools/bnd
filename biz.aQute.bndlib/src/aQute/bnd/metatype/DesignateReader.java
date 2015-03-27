package aQute.bnd.metatype;

import java.util.*;

import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;

import aQute.bnd.annotation.xml.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.xmlattribute.*;

public class DesignateReader extends ClassDataCollector {
	
	private Analyzer	analyzer;
	private Clazz	clazz;
	private Map<TypeRef,OCDDef>			classToOCDMap;
	
	private String[] pids;
	private Annotation designate;
	private final XMLAttributeFinder	finder;
	private DesignateDef				def;

	DesignateReader(Analyzer analyzer, Clazz clazz, Map<TypeRef,OCDDef> classToOCDMap, XMLAttributeFinder finder) {
		this.analyzer = analyzer;
		this.clazz = clazz;
		this.classToOCDMap = classToOCDMap;
		this.finder = finder;
	}

	static DesignateDef getDesignate(Clazz c, Analyzer analyzer, Map<TypeRef,OCDDef> classToOCDMap,
			XMLAttributeFinder finder) throws Exception {
	 		DesignateReader r = new DesignateReader(analyzer, c, classToOCDMap, finder);
	 		return r.getDef();
	}

	private DesignateDef getDef() throws Exception {
		clazz.parseClassFileWithCollector(this);
		if (pids != null && designate != null) {
			if (pids.length != 1) {
				analyzer.error(
						"DS Component %s specifies multiple pids %s, and a Designate which requires exactly one pid",
						clazz.getClassName().getFQN(), Arrays.asList(pids));
				return null;				
			}
			String pid = pids[0];
			TypeRef ocdClass = designate.get("ocd");
			// ocdClass = ocdClass.substring(1, ocdClass.length() - 1);
			OCDDef ocd = classToOCDMap.get(ocdClass);
			if (ocd == null) {
				analyzer.error(
						"DS Component %s specifies ocd class %s which cannot be found; known classes %s",
						clazz.getClassName().getFQN(), ocdClass, classToOCDMap.keySet());
				return null;				
			}
			String id = ocd.id;
			boolean factoryPid = Boolean.TRUE == designate.get("factory");
			if (def == null)

			return new DesignateDef(id, pid, factoryPid);
			def.ocdRef = id;
			def.pid = pid;
			def.factory = factoryPid;
			return def;
		}
		return null;
	}


    @Override
	public void annotation(Annotation annotation) throws Exception {
		try {
			java.lang.annotation.Annotation a = annotation.getAnnotation();
			if (a instanceof Designate)
				designate = annotation;
			else if (a instanceof Component)
				pids = ((Component)a).configurationPid();
			else {
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
		if (def == null)
			def = new DesignateDef();
		def.addExtensionAttribute(xmlAttr, annotation);
	}


}
