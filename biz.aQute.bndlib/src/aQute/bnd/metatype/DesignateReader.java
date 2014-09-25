package aQute.bnd.metatype;

import java.util.*;

import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;

import aQute.bnd.osgi.*;

public class DesignateReader extends ClassDataCollector {
	
	private Analyzer	analyzer;
	private Clazz	clazz;
	private Map<String, OCDDef> classToOCDMap;
	
	private String[] pids;
	private Annotation designate;

	DesignateReader(Analyzer analyzer, Clazz clazz, Map<String, OCDDef> classToOCDMap) {
		this.analyzer = analyzer;
		this.clazz = clazz;
		this.classToOCDMap = classToOCDMap;
	}

	static DesignateDef getDesignate(Clazz c, Analyzer analyzer, Map<String, OCDDef> classToOCDMap) throws Exception {
	 		DesignateReader r = new DesignateReader(analyzer, c, classToOCDMap);
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
			String ocdClass = ((String) designate.get("ocd"));
			ocdClass = ocdClass.substring(1, ocdClass.length() - 1);
			OCDDef ocd = classToOCDMap.get(ocdClass);
			if (ocd == null) {
				analyzer.error(
						"DS Component %s specifies ocd class %s which cannot be found; known classes %s",
						clazz.getClassName().getFQN(), ocdClass, classToOCDMap.keySet());
				return null;				
			}
			String id = ocd.id;
			boolean factoryPid = Boolean.TRUE == designate.get("factory");
			return new DesignateDef(id, pid, factoryPid);
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
		}
		catch (Exception e) {
			e.printStackTrace();
			analyzer.error("During generation of a component on class %s, exception %s", clazz, e);
		}
	}


}
