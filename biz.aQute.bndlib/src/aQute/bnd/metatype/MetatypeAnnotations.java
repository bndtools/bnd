package aQute.bnd.metatype;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aQute.bnd.component.TagResource;
import aQute.bnd.header.Attrs;
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
import aQute.libg.generics.Create;

/**
 * Analyze the class space for any classes that have an OSGi annotation for DS.
 */
public class MetatypeAnnotations implements AnalyzerPlugin {

	enum Options {
		nested,
		version {
			@Override
			void process(MetatypeAnnotations anno, Attrs attrs) {
				String v = attrs.get("minimum");
				if (v != null && v.length() > 0) {
					anno.minVersion = MetatypeVersion.valueFor(v);
				}
			}

			@Override
			void reset(MetatypeAnnotations anno) {
				anno.minVersion = MetatypeVersion.VERSION_1_2;
			}
		};

		void process(MetatypeAnnotations anno, Attrs attrs) {

		}

		void reset(MetatypeAnnotations anno) {

		}

		static void parseOption(Map.Entry<String, Attrs> entry, EnumSet<Options> options, MetatypeAnnotations state) {
			String s = entry.getKey();
			boolean negation = false;
			if (s.startsWith("!")) {
				negation = true;
				s = s.substring(1);
			}
			Options option = Options.valueOf(s);
			if (negation) {
				options.remove(option);
				option.reset(state);
			} else {
				options.add(option);
				Attrs attrs;
				if ((attrs = entry.getValue()) != null) {
					option.process(state, attrs);
				}
			}

		}
	};

	MetatypeVersion minVersion;

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		this.minVersion = MetatypeVersion.VERSION_1_2;
		Parameters header = OSGiHeader.parseHeader(analyzer.getProperty(Constants.METATYPE_ANNOTATIONS, "*"));
		if (header.size() == 0)
			return false;

		Parameters optionsHeader = OSGiHeader.parseHeader(analyzer.getProperty(Constants.METATYPE_ANNOTATIONS_OPTIONS));
		EnumSet<Options> options = EnumSet.noneOf(Options.class);
		for (Map.Entry<String, Attrs> entry : optionsHeader.entrySet()) {
			try {
				Options.parseOption(entry, options, this);
			} catch (IllegalArgumentException e) {
				analyzer.error("Unrecognized %s value %s with attributes %s, expected values are %s",
					Constants.METATYPE_ANNOTATIONS_OPTIONS, entry.getKey(), entry.getValue(),
					EnumSet.allOf(Options.class));
			}
		}

		Map<TypeRef, OCDDef> classToOCDMap = new HashMap<>();

		Set<String> ocdIds = new HashSet<>();
		Set<String> pids = new HashSet<>();

		Instructions instructions = new Instructions(header);

		XMLAttributeFinder finder = new XMLAttributeFinder(analyzer);

		List<Clazz> list = Create.list();
		for (Clazz c : analyzer.getClassspace()
			.values()) {
			for (Instruction instruction : instructions.keySet()) {
				if (instruction.matches(c.getFQN())) {
					if (instruction.isNegated()) {
						break;
					}
					list.add(c);
					OCDDef definition = OCDReader.getOCDDef(c, analyzer, options, finder, minVersion);
					if (definition != null) {
						classToOCDMap.put(c.getClassName(), definition);
					}
					break;
				}
			}
		}

		// process Designate annotations after OCD annotations
		for (Clazz c : list) {
			DesignateReader.getDesignate(c, analyzer, classToOCDMap, finder);
		}

		for (Map.Entry<TypeRef, OCDDef> entry : classToOCDMap.entrySet()) {
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
			analyzer.getJar()
				.putResource(name, new TagResource(definition.getTag()));
		}
		return false;
	}

	@Override
	public String toString() {
		return "MetatypeAnnotations";
	}
}
