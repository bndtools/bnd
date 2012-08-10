package bndtools.release;

import java.util.List;

import aQute.bnd.build.Project;
import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;

public class DiffHelper {

	public static Baseline createBaseline(Project project, String bsn) throws Exception {

		List<Builder> builders = project.getBuilder(null).getSubBuilders();
		Builder builder = null;
		for (Builder b : builders) {
			if (bsn.equals(b.getBsn())) {
				builder = b;
				break;
			}
		}
		if (builder == null) {
			return null;
		}
		return createBaseline(builder);
	}

	public static Baseline createBaseline(Builder builder) {

		try {

			if (builder != null) {
				Jar jar = builder.build();

				Jar currentJar = builder.getBaselineJar();
				if (currentJar == null) {
				    currentJar = new Jar(".");
				}
				Baseline baseline = new Baseline(builder, new DiffPluginImpl());

				baseline.baseline(jar, currentJar, null);
				return baseline;
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return null;

	}

	public static String removeVersionQualifier(String version) {
		if (version == null) {
			return null;
		}
		// Remove qualifier
		String[] parts = version.split("\\.");
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (int i = 0; i < parts.length; i++) {
			if (i == 3) {
				break;
			}
			sb.append(sep);
			sb.append(parts[i]);
			sep = ".";
		}
		return sb.toString();
	}

}
