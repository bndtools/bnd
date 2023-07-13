package aQute.p2.export;

import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Project;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.export.Exporter;

/**
 * This is an exporter for p2 repositories.
 */
@BndPlugin(name = "p2.exporter")
public class P2Exporter implements Exporter {

	@Override
	public String[] getTypes() {
		return new String[] {
			"p2"
		};
	}

	/**
	 * Export an export definition. The definition consists of a number of
	 * bndrun files that are collected in jar file. The exporter will create the
	 * horrendously and redundant metadata needed for brain damaged p2.
	 * <p>
	 * Problems are reported to the project
	 *
	 * @param type the p2 type
	 * @param bndrun the project file
	 * @param options the options, see the p2exporte md file.
	 * @return a name & resource or null if there was a problem.
	 */
	@Override
	public Entry<String, Resource> export(String type, Project bndrun, Map<String, String> options) throws Exception {

		P2Export p = new P2Export(bndrun, options);

		return p.generate();
	}

}
