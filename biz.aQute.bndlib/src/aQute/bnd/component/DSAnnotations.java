package aQute.bnd.component;

import java.util.*;

import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.service.*;
import aQute.bnd.version.*;
import aQute.service.reporter.Reporter.SetLocation;

/**
 * Analyze the class space for any classes that have an OSGi annotation for DS.
 */
public class DSAnnotations implements AnalyzerPlugin {

	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		Parameters header = OSGiHeader.parseHeader(analyzer.getProperty(Constants.DSANNOTATIONS));
		if (header.size() == 0)
			return false;

		Instructions instructions = new Instructions(header);
		Collection<Clazz> list = analyzer.getClassspace().values();
		String sc = analyzer.getProperty(Constants.SERVICE_COMPONENT);
		List<String> names = new ArrayList<String>();
		if (sc != null && sc.trim().length() > 0)
			names.add(sc);

		for (Clazz c : list) {
			for (Instruction instruction : instructions.keySet()) {

				if (instruction.matches(c.getFQN())) {
					if (instruction.isNegated())
						break;
					ComponentDef definition = AnnotationReader.getDefinition(c, analyzer);
					if (definition != null) {

						definition.sortReferences();
						definition.prepare(analyzer);
						verifyVersion(analyzer, definition);

						String name = "OSGI-INF/"
								+ analyzer.validResourcePath(definition.name, "Invalid component name") + ".xml";
						names.add(name);
						analyzer.getJar().putResource(name, new TagResource(definition.getTag()));
					}
				}
			}
		}
		sc = Processor.append(names.toArray(new String[names.size()]));
		analyzer.setProperty(Constants.SERVICE_COMPONENT, sc);
		return false;
	}

	/**
	 * Verify that the component definition has a version that is <= than the
	 * version of the component package that we build against.
	 */
	private void verifyVersion(Analyzer analyzer, ComponentDef definition) throws Exception {
		PackageRef component = analyzer.getPackageRef("org.osgi.service.component");
		Attrs attrs = analyzer.getClasspathExports().get(component);
		if (attrs != null) {
			String version = attrs.getVersion();
			if (version != null && Verifier.isVersion(version)) {
				Version v = new Version(version);
				if (definition.version.compareTo(v) > 0) {
					SetLocation error = analyzer
							.error("Generating XML for %s in type %s that uses a namespace version %s while you are building against %s",
									definition.name, definition.implementation, definition.version, v);

					error.details(component);
					analyzer.setTypeLocation(error, definition.implementation);
				}
			}
		}
	}

	@Override
	public String toString() {
		return "DSAnnotations";
	}
}
