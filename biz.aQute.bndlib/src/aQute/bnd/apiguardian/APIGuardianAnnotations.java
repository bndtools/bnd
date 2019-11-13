package aQute.bnd.apiguardian;

import java.util.Map.Entry;

import aQute.bnd.apiguardian.api.API;
import aQute.bnd.apiguardian.api.API.Status;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.service.AnalyzerPlugin;

public class APIGuardianAnnotations implements AnalyzerPlugin {

	private final static String API_ANNOTATION = "org/apiguardian/api/API";
	private final static String	STATUS_PROPERTY	= "status";

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		// Opt-in is required.
		Parameters header = OSGiHeader.parseHeader(analyzer.getProperty(Constants.EXPORT_APIGUARDIAN));
		if (header.isEmpty())
			return false;

		Parameters exportPackages = analyzer.getExportContents();

		Instructions instructions = new Instructions(header);

		Parameters apiGuardianPackages = new Parameters(false);

		for (Clazz c : analyzer.getClassspace()
			.values()) {

			if (c.isModule() || c.isInnerClass() || c.isSynthetic()) {
				continue;
			}

			for (Entry<Instruction, Attrs> entry : instructions.entrySet()) {
				Instruction instruction = entry.getKey();
				if (instruction.matches(c.getFQN())) {
					if (instruction.isNegated()) {
						break;
					}

					c.annotations(API_ANNOTATION)
						.map(ann -> Status.valueOf(ann.get(STATUS_PROPERTY)))
						.max(Status::compareTo)
						.ifPresent(status -> {

							Attrs attrs = apiGuardianPackages.computeIfAbsent(c.getClassName()
								.getPackageRef()
								.getFQN(), k -> new Attrs(entry.getValue()));

							attrs.compute(STATUS_PROPERTY,
								(k, v) -> (v == null) ? status.name()
									: (Status.valueOf(v)
										.compareTo(status) > 0 ? v : status.name()));
						});
				}
			}
		}

		apiGuardianPackages.values()
			.stream()
			.filter(attrs -> API.Status.valueOf(attrs.get(STATUS_PROPERTY)) == Status.INTERNAL)
			.forEach(attrs -> {
				attrs.put(Constants.MANDATORY_DIRECTIVE, STATUS_PROPERTY);
				attrs.put(Constants.NO_IMPORT_DIRECTIVE, "true");
			});

		exportPackages.mergeWith(apiGuardianPackages, false);

		analyzer.setExportContents(exportPackages.toString());

		return false;
	}

}
