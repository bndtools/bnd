package aQute.bnd.metatype;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import aQute.bnd.component.TagResource;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.xmlattribute.XMLAttributeFinder;

/**
 * Analyze the class space for any classes that have an OSGi annotation for DS.
 */
public class MetatypeAnnotations implements AnalyzerPlugin {

	enum Options {
		nested
	}

	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		Parameters header = OSGiHeader.parseHeader(analyzer.getProperty(Constants.METATYPE_ANNOTATIONS, "*"));
		if (header.size() == 0)
			return false;

		Parameters optionsHeader = OSGiHeader.parseHeader(analyzer.getProperty(Constants.METATYPE_ANNOTATIONS_OPTIONS));
		EnumSet<Options> options = EnumSet.noneOf(Options.class);
		for (String s : optionsHeader.keySet()) {
			try {
				options.add(Options.valueOf(s));
			}
			catch (IllegalArgumentException e) {
				analyzer.error("Unrecognized %s value %s, expected values are %s",
						Constants.METATYPE_ANNOTATIONS_OPTIONS, s, EnumSet.allOf(Options.class));
			}
		}

		Map<TypeRef,OCDDef> classToOCDMap = new HashMap<TypeRef,OCDDef>();

		Set<String> ocdIds = new HashSet<String>();
		Set<String> pids = new HashSet<String>();

		Instructions instructions = new Instructions(header);
		Collection<Clazz> list = analyzer.getClassspace().values();

		XMLAttributeFinder finder = new XMLAttributeFinder(analyzer);
		for (Clazz c : list) {
			for (Instruction instruction : instructions.keySet()) {

				if (instruction.matches(c.getFQN())) {
					if (instruction.isNegated())
						break;
					OCDDef definition = OCDReader.getOCDDef(c, analyzer, options, finder);
					if (definition != null) {
						classToOCDMap.put(c.getClassName(), definition);
					}
				}
			}
		}

		header = OSGiHeader.parseHeader(analyzer.getProperty(Constants.DSANNOTATIONS));
		if (header.size() > 0) {

			instructions = new Instructions(header);
			list = analyzer.getClassspace().values();

			for (Clazz c : list) {
				for (Instruction instruction : instructions.keySet()) {

					if (instruction.matches(c.getFQN())) {
						if (instruction.isNegated())
							break;
						DesignateReader.getDesignate(c, analyzer, classToOCDMap, finder);
					}
				}
			}
		}

		for (Map.Entry<TypeRef,OCDDef> entry : classToOCDMap.entrySet()) {
			TypeRef c = entry.getKey();
			OCDDef definition = entry.getValue();
			definition.prepare(analyzer);
			if (!ocdIds.add(definition.id)) {
				analyzer.error("Duplicate OCD id %s from class %s; known ids %s", definition.id, c.getFQN(), ocdIds);
			}
			for (DesignateDef dDef : definition.designates) {
				if (dDef.pid != null && !pids.add(dDef.pid)) {
					analyzer.error("Duplicate pid %s from class %s", dDef.pid, c.getFQN());
				}
			}
			String name = "OSGI-INF/metatype/" + analyzer.validResourcePath(definition.id, "Invalid resource name")
					+ ".xml";
			analyzer.getJar().putResource(name, new TagResource(definition.getTag()));
		}
		return false;
	}

	@Override
	public String toString() {
		return "MetatypeAnnotations";
	}
}
