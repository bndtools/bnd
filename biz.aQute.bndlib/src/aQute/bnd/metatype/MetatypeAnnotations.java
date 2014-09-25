package aQute.bnd.metatype;

import java.util.*;

import aQute.bnd.component.TagResource;
import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.*;

/**
 * Analyze the class space for any classes that have an OSGi annotation for DS.
 */
public class MetatypeAnnotations implements AnalyzerPlugin {
	
	private final Map<String, OCDDef> classToOCDMap = new HashMap<String, OCDDef>();

	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		Parameters header = OSGiHeader.parseHeader(analyzer.getProperty(Constants.METATYPE_ANNOTATIONS));
		if (header.size() == 0)
			return false;

		Instructions instructions = new Instructions(header);
		Collection<Clazz> list = analyzer.getClassspace().values();

		for (Clazz c: list) {
			for (Instruction instruction : instructions.keySet()) {

				if (instruction.matches(c.getFQN())) {
					if (instruction.isNegated())
						break;
					OCDDef definition = OCDReader.getOCDDef(c, analyzer);
					if (definition != null) {
						definition.prepare(analyzer);
						classToOCDMap.put(c.getClassName().getBinary(), definition);
						String name = "OSGI-INF/metatype/" + analyzer.validResourcePath(definition.id, "Invalid resource name") + ".xml";
						analyzer.getJar().putResource(name, new TagResource(definition.getTag()));
					}
				}
			}
		}
		
		header = OSGiHeader.parseHeader(analyzer.getProperty(Constants.DSANNOTATIONS));
		if (header.size() == 0)
			return false;

	    instructions = new Instructions(header);
	    list = analyzer.getClassspace().values();

		for (Clazz c: list) {
			for (Instruction instruction : instructions.keySet()) {

				if (instruction.matches(c.getFQN())) {
					if (instruction.isNegated())
						break;
					DesignateDef designate = DesignateReader.getDesignate(c, analyzer, classToOCDMap);
					if (designate != null) {
						designate.prepare(analyzer);
						String name = "OSGI-INF/metatype/" + analyzer.validResourcePath(c.getFQN(), "Invalid resource name") + ".xml";
						analyzer.getJar().putResource(name, new TagResource(designate.getTag()));
					}
				}
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "MetatypeAnnotations";
	}
}
