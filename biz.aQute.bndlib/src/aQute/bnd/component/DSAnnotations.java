package aQute.bnd.component;

import java.util.*;

import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.*;

/**
 * Analyze the class space for any classes that have an OSGi annotation for DS.
 */
public class DSAnnotations implements AnalyzerPlugin {

	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		Parameters header = OSGiHeader.parseHeader(analyzer.getProperty(Constants.DSANNOTATIONS));
		if (header.size() == 0)
			return false;

		Instructions instructions = new Instructions(header);
		Set<Clazz> list = new HashSet<Clazz>(analyzer.getClassspace().values());
		String sc = analyzer.getProperty(Constants.SERVICE_COMPONENT);
		List<String> names = new ArrayList<String>();
		if (sc != null && sc.trim().length() > 0)
			names.add(sc);

		for (Clazz c: list) {
			for (Instruction instruction : instructions.keySet()) {

				if (instruction.matches(c.getFQN())) {
					if (instruction.isNegated())
						break;
					else {
						ComponentDef definition = AnnotationReader.getDefinition(c, analyzer);
						if (definition != null) {
							definition.sortReferences();
							definition.prepare(analyzer);
							String name = "OSGI-INF/" + definition.name + ".xml";
							names.add(name);
							analyzer.getJar().putResource(name, new TagResource(definition.getTag()));
						}
					}
				}
			}
		}
		sc = Processor.append(names.toArray(new String[names.size()]));
		analyzer.setProperty(Constants.SERVICE_COMPONENT, sc);
		return false;
	}
}
