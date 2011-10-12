package aQute.bnd.component;

import aQute.bnd.service.*;
import aQute.lib.osgi.*;

/**
 * Analyze the class space for any classes that have an OSGi annotation for DS.
 * 
 */
public class DSAnnotations implements AnalyzerPlugin {

	public boolean analyzeJar(Analyzer analyzer) throws Exception {

		for (Clazz c : analyzer.getClassspace().values()) {
			ComponentDef definition = AnnotationReader.getDefinition(c, analyzer);
			if (definition != null) {
				definition.prepare(analyzer);
				analyzer.getJar().putResource("OSGI-INF/" + definition.name + ".xml",
						new TagResource(definition.getTag()));
			}
		}
		return false;
	}

}
