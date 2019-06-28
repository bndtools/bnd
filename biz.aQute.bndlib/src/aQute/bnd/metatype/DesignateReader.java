package aQute.bnd.metatype;

import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.xml.XMLAttribute;
import aQute.bnd.component.annotations.Component;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Annotation;
import aQute.bnd.osgi.ClassDataCollector;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.xmlattribute.XMLAttributeFinder;

public class DesignateReader extends ClassDataCollector {
	static final Logger					logger	= LoggerFactory.getLogger(DesignateReader.class);

	private Analyzer					analyzer;
	private Clazz						clazz;
	private Map<TypeRef, OCDDef>		classToOCDMap;

	private String[]					pids;
	private String						pid;
	private Annotation					designate;
	private final XMLAttributeFinder	finder;
	private DesignateDef				def;

	DesignateReader(Analyzer analyzer, Clazz clazz, Map<TypeRef, OCDDef> classToOCDMap, XMLAttributeFinder finder) {
		this.analyzer = analyzer;
		this.clazz = clazz;
		this.classToOCDMap = classToOCDMap;
		this.finder = finder;
	}

	static DesignateDef getDesignate(Clazz c, Analyzer analyzer, Map<TypeRef, OCDDef> classToOCDMap,
		XMLAttributeFinder finder) throws Exception {
		DesignateReader r = new DesignateReader(analyzer, c, classToOCDMap, finder);
		return r.getDef();
	}

	private DesignateDef getDef() throws Exception {
		clazz.parseClassFileWithCollector(this);
		if (pid != null && designate != null) {
			if (pids != null && pids.length > 1) {
				analyzer.error(
					"DS Component %s specifies multiple pids %s, and a Designate which requires exactly one pid",
					clazz.getClassName()
						.getFQN(),
					Arrays.asList(pids));
				return null;
			}
			TypeRef ocdClass = designate.get("ocd");
			logger.debug("Class {} annotated {}", clazz, designate);
			OCDDef ocd = classToOCDMap.get(ocdClass);
			if (ocd == null) {
				analyzer.error("DS Component %s specifies ocd class %s which cannot be found; known classes %s",
					clazz.getClassName()
						.getFQN(),
					ocdClass, classToOCDMap.keySet());
				return null;
			}
			String id = ocd.id;
			boolean factoryPid = Boolean.TRUE.equals(designate.get("factory"));
			if (def == null)
				def = new DesignateDef(finder);
			def.ocdRef = id;
			def.pid = pid;
			def.factory = factoryPid;
			ocd.designates.add(def);
			return def;
		}
		return null;
	}

	@Override
	public void annotation(Annotation annotation) throws Exception {
		try {
			switch (annotation.getName()
				.getFQN()) {
				case "org.osgi.service.metatype.annotations.Designate" :
					designate = annotation;
					break;
				case "org.osgi.service.component.annotations.Component" :
					doComponent(annotation, annotation.getAnnotation(Component.class));

					break;
				default :
					XMLAttribute xmlAttr = finder.getXMLAttribute(annotation);
					if (xmlAttr != null) {
						doXmlAttribute(annotation, xmlAttr);
					}
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			analyzer.error("During generation of a component on class %s, exception %s", clazz, e);
		}
	}

	void doComponent(Annotation a, Component c) {
		pids = a.containsKey("configurationPid") ? c.configurationPid() : null;
		if (pids != null) {
			pid = pids[0];
		}
		if (pids == null || "$".equals(pid)) {
			pid = a.containsKey("name") ? c.name()
				: clazz.getClassName()
					.getFQN();
		}
	}

	private void doXmlAttribute(Annotation annotation, XMLAttribute xmlAttr) {
		if (def == null)
			def = new DesignateDef(finder);
		def.addExtensionAttribute(xmlAttr, annotation);
	}

}
