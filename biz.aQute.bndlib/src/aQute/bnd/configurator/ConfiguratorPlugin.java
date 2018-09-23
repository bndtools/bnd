package aQute.bnd.configurator;

import static aQute.bnd.osgi.Constants.CONFIGURATOR;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.version.Version;
import aQute.lib.io.FileTree;
import aQute.lib.strings.Strings;

@BndPlugin(name = "ConfiguratorPlugin")
public class ConfiguratorPlugin implements AnalyzerPlugin {

	private final static Logger	logger								= LoggerFactory.getLogger(ConfiguratorPlugin.class);
	private static final String	PARAM_TARGET						= "target";
	private final static String	PARAM_FROM							= "from";
	private final static String	PARAM_REQ							= "requirement";

	private final static String	DEFAULT_REQUIREMENTS_ACTIVE_VALUE	= "true";
	private final static String	DEFAULT_FROM_ACTIVE					= "true";
	private final static String	DEFAULT_FROM_INCLUDES				= "configurator/*.json";
	private final static String	DEFAULT_TARGET						= "OSGI-INF/configurator";

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {

		String configurator = analyzer.getProperty(CONFIGURATOR);

		logger.debug("configurator {}", configurator);

		if (configurator == null) {
			return true;
		}

		Parameters clauses = analyzer.parseHeader(analyzer.getProperty(CONFIGURATOR));

		String includes = null;
		String excludes = null;
		String target = DEFAULT_TARGET;
		String base = null;
		String active = DEFAULT_FROM_ACTIVE;
		List<String> src = new ArrayList<>();

		if (clauses.containsKey(PARAM_FROM)) {
			Attrs from = clauses.get(PARAM_FROM);
			excludes = from.get("excludes");
			includes = from.get("includes");
			base = from.get("base");
			active = from.get("active", active);
		}
		if (clauses.containsKey(PARAM_TARGET)) {
			Attrs dir = clauses.get(PARAM_TARGET);
			target = dir.get("dir", target);
		}

		if ("true".equals(active)) {
			FileTree tree = new FileTree();
			tree.addIncludes(includes);
			tree.addExcludes(excludes);

			File baseFile = base != null ? new File(base) : analyzer.getBase();
			List<File> files = tree.getFiles(baseFile, DEFAULT_FROM_INCLUDES);
			for (File file : files) {
				analyzer.getJar()
					.putResource(target + "/" + file.getName(), new FileResource(file));
			}

		}
		boolean configExists = false;

		for (final Entry<String, Resource> entry : analyzer.getJar()
			.getResources()
			.entrySet()) {
			final String key = entry.getKey();
			if (key.startsWith(target) && key.toLowerCase()
				.endsWith(".json")) {
				configExists = true;
				break;
			}
		}
		
		if (configExists) {
			String version = ConfiguratorConstants.CONFIGURATOR_SPECIFICATION_VERSION;
			String reqactive = DEFAULT_REQUIREMENTS_ACTIVE_VALUE;
			if (clauses.containsKey(PARAM_REQ)) {
				Attrs requirement = clauses.get(PARAM_REQ);
				version = requirement.get("version", version);
				reqactive = requirement.get("active", DEFAULT_REQUIREMENTS_ACTIVE_VALUE);
			}
			if ("true".equals(reqactive)) {
				final TreeSet<String> set = new TreeSet<>();
				final String requires = getExtenderRequirement(ConfiguratorConstants.CONFIGURATOR_EXTENDER_NAME,
					new Version(version));
				set.add(requires);

				if (!DEFAULT_TARGET.equals(target)) {
					set.add("configurations:List<String>=" + target);

				}
				updateHeader(analyzer, Constants.REQUIRE_CAPABILITY, set);
			}
		}
		return true;
	}

	// aQute.bnd.component.DSAnnotations may refactor to abstractAnalyzerPlugin

	private String getExtenderRequirement(String extender, Version version) {
		final Version next = new Version(version.getMajor() + 1);
		final Parameters p = new Parameters();
		final Attrs a = new Attrs();
		a.put(Constants.FILTER_DIRECTIVE,
			"\"(&(osgi.extender=" + extender + ")(version>=" + version + ")(!(version>=" + next + ")))\"");
		p.put("osgi.extender", a);
		final String s = p.toString();
		return s;
	}

	private void updateHeader(Analyzer analyzer, String name, TreeSet<String> set) {
		if (!set.isEmpty()) {
			final String value = analyzer.getProperty(name);
			if (value != null) {
				final Parameters p = OSGiHeader.parseHeader(value);
				for (final Map.Entry<String, Attrs> entry : p.entrySet()) {
					final StringBuilder sb = new StringBuilder(entry.getKey());
					if (entry.getValue() != null) {
						sb.append(";");
						entry.getValue()
							.append(sb);
					}
					set.add(sb.toString());
				}
			}
			final String header = Strings.join(set);
			analyzer.setProperty(name, header);
		}
	}

}
